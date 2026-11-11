package com.pedraotools.app.data

import com.pedraotools.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

data class PlaceDetails(
    val placeId: String,
    val displayName: String,
    val formattedAddress: String,
    val reviewUrl: String,
    val expandedUrl: String
)

data class ParsedMapsUrl(
    val placeName: String,
    val latitude: Double?,
    val longitude: Double?
)

class PlaceIdService(
    private val httpClient: OkHttpClient = defaultOkHttpClient()
) {

    suspend fun resolveAndFetchPlace(inputUrl: String): Result<PlaceDetails> = withContext(Dispatchers.IO) {
        runCatching {
            val extractedUrl = extractMapsUrl(inputUrl)
            require(isValidMapsUrl(extractedUrl)) {
                "Link inválido. Insira um link do Google Maps (ex: https://maps.app.goo.gl/...)."
            }

            val apiKey = BuildConfig.MAPS_API_KEY.trim()
            require(apiKey.isNotEmpty() && apiKey != "SUA_CHAVE_GOOGLE_PLACES_AQUI") {
                "Chave MAPS_API_KEY não configurada no arquivo local.properties."
            }

            // 1. Resolver redirecionamentos HTTP 301/302 passo a passo (evita loop de cookies/consent)
            val expandedUrl = expandShortUrl(extractedUrl)

            // 2. Extrair nome do estabelecimento e coordenadas (@lat,lng)
            val parsed = parseExpandedMapsUrl(expandedUrl)
            require(parsed.placeName.isNotBlank()) {
                "Não foi possível identificar o nome do estabelecimento na URL expandida."
            }

            // 3. Consultar Google Places API (New)
            searchPlaceInGoogleApi(
                apiKey = apiKey,
                parsedUrl = parsed,
                expandedUrl = expandedUrl
            )
        }
    }

    internal fun expandShortUrl(shortUrl: String): String {
        val redirectClient = httpClient.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .cookieJar(InMemoryCookieJar())
            .build()

        var currentUrl = shortUrl
        var bestPlaceUrl: String? = null
        val visitedUrls = mutableSetOf<String>()

        for (step in 0 until MAX_REDIRECT_STEPS) {
            if (!visitedUrls.add(currentUrl)) {
                return bestPlaceUrl ?: currentUrl
            }

            if (hasPlaceAndCoords(currentUrl)) {
                return currentUrl
            }
            if (isUsefulPlaceUrl(currentUrl)) {
                bestPlaceUrl = currentUrl
            }

            val request = Request.Builder()
                .url(currentUrl)
                .header("User-Agent", DESKTOP_USER_AGENT)
                .header("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
                .get()
                .build()

            redirectClient.newCall(request).execute().use { response ->
                val location = response.header("Location")
                if (response.isRedirect && !location.isNullOrBlank()) {
                    val resolvedUrl = response.request.url.resolve(location)?.toString()
                        ?: return bestPlaceUrl ?: currentUrl

                    val parsedNext = resolvedUrl.toHttpUrlOrNull()
                    val host = parsedNext?.host?.lowercase().orEmpty()

                    // Se o Google redirecionar para tela de consentimento, extrai o parâmetro "continue"
                    if (host.contains("consent.google.")) {
                        val continueUrl = parsedNext?.queryParameter("continue")
                        if (!continueUrl.isNullOrBlank() && visitedUrls.add("$currentUrl->continue")) {
                            currentUrl = continueUrl
                            return@use
                        }
                        if (bestPlaceUrl != null) {
                            return bestPlaceUrl!!
                        }
                    }

                    if (bestPlaceUrl != null && (host.contains("accounts.google.") || resolvedUrl.contains("/sorry/"))) {
                        return bestPlaceUrl!!
                    }

                    if (hasPlaceAndCoords(resolvedUrl)) {
                        return resolvedUrl
                    }
                    if (isUsefulPlaceUrl(resolvedUrl)) {
                        bestPlaceUrl = resolvedUrl
                    }

                    currentUrl = resolvedUrl
                } else {
                    if (bestPlaceUrl != null) {
                        return bestPlaceUrl!!
                    }

                    // Fallback: se retornou HTML (ex: página intermediária), procura link do Maps no HTML
                    val contentType = response.header("Content-Type").orEmpty().lowercase()
                    if (contentType.contains("text/html")) {
                        val html = response.body?.string().orEmpty()
                        extractMapsPlaceUrlFromHtml(html)?.let { return it }
                    }

                    return response.request.url.toString().ifBlank { currentUrl }
                }
            }
        }

        return bestPlaceUrl ?: currentUrl
    }

    private fun searchPlaceInGoogleApi(
        apiKey: String,
        parsedUrl: ParsedMapsUrl,
        expandedUrl: String
    ): PlaceDetails {
        val requestJson = buildPlacesRequestJson(parsedUrl)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestJson.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(PLACES_SEARCH_TEXT_ENDPOINT)
            .addHeader("X-Goog-Api-Key", apiKey)
            .addHeader("X-Goog-FieldMask", "places.id,places.displayName,places.formattedAddress")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            val rawBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val apiErrorMessage = runCatching {
                    JSONObject(rawBody).optJSONObject("error")?.optString("message")
                }.getOrNull()
                val detail = if (!apiErrorMessage.isNullOrBlank()) ": $apiErrorMessage" else ""
                throw IOException("Erro na Places API (HTTP ${response.code})$detail")
            }

            val rootJson = JSONObject(rawBody)
            val placesArray = rootJson.optJSONArray("places")
            if (placesArray == null || placesArray.length() == 0) {
                throw IllegalStateException(
                    "Nenhum local encontrado na Google Places API para \"${parsedUrl.placeName}\"."
                )
            }

            val firstPlace = placesArray.getJSONObject(0)
            val placeId = firstPlace.optString("id").trim()
            if (placeId.isEmpty()) {
                throw IllegalStateException("A resposta da Google Places API não retornou um Place ID válido.")
            }

            val displayName = firstPlace
                .optJSONObject("displayName")
                ?.optString("text")
                ?.takeIf { it.isNotBlank() }
                ?: parsedUrl.placeName

            val formattedAddress = firstPlace
                .optString("formattedAddress")
                .ifBlank { "Endereço não informado" }

            val reviewUrl = buildWriteReviewUrl(placeId)

            return PlaceDetails(
                placeId = placeId,
                displayName = displayName,
                formattedAddress = formattedAddress,
                reviewUrl = reviewUrl,
                expandedUrl = expandedUrl
            )
        }
    }

    companion object {
        private const val PLACES_SEARCH_TEXT_ENDPOINT = "https://places.googleapis.com/v1/places:searchText"
        private const val LOCATION_BIAS_RADIUS_METERS = 500.0
        private const val MAX_REDIRECT_STEPS = 15
        private const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"

        private val URL_IN_TEXT_REGEX = Regex("""https?://[^\s<>"]+""", RegexOption.IGNORE_CASE)
        private val PLACE_NAME_REGEX = Regex("""/maps/place/([^/@?#]+)""")
        private val AT_COORDINATES_REGEX = Regex("""@(-?\d+\.\d+),(-?\d+\.\d+)""")
        private val DATA_COORDINATES_REGEX = Regex("""!3d(-?\d+\.\d+)!4d(-?\d+\.\d+)""")
        private val QUERY_PARAM_REGEX = Regex("""[?&](?:q|query)=([^&#]+)""")
        private val HTML_MAPS_PLACE_REGEX = Regex("""https://www\.google\.com(?:\.br)?/maps/place/[^"'\s<>\\]+""")
        private val HTML_OG_URL_REGEX = Regex("""<meta\s+[^>]*property=["']og:url["'][^>]*content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

        fun defaultOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        fun extractMapsUrl(rawText: String): String {
            val trimmed = rawText.trim()
            val matches = URL_IN_TEXT_REGEX.findAll(trimmed).map { it.value }.toList()
            return matches.firstOrNull { isValidMapsUrl(it) } ?: trimmed
        }

        private fun isUsefulPlaceUrl(url: String): Boolean {
            return PLACE_NAME_REGEX.containsMatchIn(url) || QUERY_PARAM_REGEX.containsMatchIn(url)
        }

        private fun hasPlaceAndCoords(url: String): Boolean {
            val hasPlace = PLACE_NAME_REGEX.containsMatchIn(url)
            val hasCoords = AT_COORDINATES_REGEX.containsMatchIn(url) || DATA_COORDINATES_REGEX.containsMatchIn(url)
            return hasPlace && hasCoords
        }

        private fun extractMapsPlaceUrlFromHtml(html: String): String? {
            if (html.isBlank()) return null
            HTML_OG_URL_REGEX.find(html)?.groupValues?.getOrNull(1)?.let { ogUrl ->
                val unescaped = ogUrl.replace("&amp;", "&")
                if (isUsefulPlaceUrl(unescaped)) return unescaped
            }
            HTML_MAPS_PLACE_REGEX.find(html)?.value?.let { directUrl ->
                return directUrl.replace("\\u003d", "=").replace("\\u0026", "&").replace("&amp;", "&")
            }
            return null
        }

        fun isValidMapsUrl(url: String): Boolean {
            val normalized = url.trim().lowercase()
            if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
                return false
            }
            return normalized.contains("maps.app.goo.gl") ||
                normalized.contains("goo.gl/maps") ||
                normalized.contains("google.com/maps") ||
                normalized.contains("google.com.br/maps") ||
                normalized.contains("maps.google.")
        }

        fun parseExpandedMapsUrl(expandedUrl: String): ParsedMapsUrl {
            val rawPlaceSegment = PLACE_NAME_REGEX.find(expandedUrl)?.groupValues?.getOrNull(1)
                ?: QUERY_PARAM_REGEX.find(expandedUrl)?.groupValues?.getOrNull(1)
                ?: ""

            val decodedPlaceName = if (rawPlaceSegment.isNotBlank()) {
                runCatching {
                    URLDecoder.decode(
                        rawPlaceSegment.replace("+", " "),
                        StandardCharsets.UTF_8.name()
                    ).trim()
                }.getOrDefault(rawPlaceSegment.replace("+", " ").trim())
            } else {
                ""
            }

            val coordsMatch = AT_COORDINATES_REGEX.find(expandedUrl)
                ?: DATA_COORDINATES_REGEX.find(expandedUrl)

            val latitude = coordsMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull()
            val longitude = coordsMatch?.groupValues?.getOrNull(2)?.toDoubleOrNull()

            return ParsedMapsUrl(
                placeName = decodedPlaceName,
                latitude = latitude,
                longitude = longitude
            )
        }

        fun buildPlacesRequestJson(parsedUrl: ParsedMapsUrl): JSONObject {
            val body = JSONObject()
            body.put("textQuery", parsedUrl.placeName)

            val lat = parsedUrl.latitude
            val lng = parsedUrl.longitude
            if (lat != null && lng != null) {
                val center = JSONObject()
                    .put("latitude", lat)
                    .put("longitude", lng)
                val circle = JSONObject()
                    .put("center", center)
                    .put("radius", LOCATION_BIAS_RADIUS_METERS)
                val locationBias = JSONObject()
                    .put("circle", circle)
                body.put("locationBias", locationBias)
            }

            return body
        }

        fun buildWriteReviewUrl(placeId: String): String {
            return "https://search.google.com/local/writereview?placeid=$placeId"
        }
    }
}

private class InMemoryCookieJar : CookieJar {
    private val cookieStore = ConcurrentHashMap<String, List<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        val existing = cookieStore[url.host].orEmpty().associateBy { it.name }.toMutableMap()
        for (cookie in cookies) {
            existing[cookie.name] = cookie
        }
        cookieStore[url.host] = existing.values.toList()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        return cookieStore[url.host].orEmpty().filter { it.expiresAt >= now }
    }
}

