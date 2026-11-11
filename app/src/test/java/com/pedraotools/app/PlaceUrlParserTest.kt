package com.pedraotools.app

import com.pedraotools.app.data.PlaceIdService
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class PlaceUrlParserTest {

    @Test
    fun `isValidMapsUrl validates short and full maps urls`() {
        assertTrue(PlaceIdService.isValidMapsUrl("https://maps.app.goo.gl/abc123XYZ"))
        assertTrue(PlaceIdService.isValidMapsUrl("https://www.google.com/maps/place/Padaria/@-23.5,-46.6,17z"))
        assertTrue(PlaceIdService.isValidMapsUrl("https://www.google.com.br/maps/place/Cafe"))
        assertFalse(PlaceIdService.isValidMapsUrl("https://example.com/place"))
        assertFalse(PlaceIdService.isValidMapsUrl("texto solto"))
    }

    @Test
    fun `extractMapsUrl extracts url from shared maps text`() {
        val shared = "Padaria Estrela\nhttps://maps.app.goo.gl/xyz987\nVeja no Maps"
        assertEquals("https://maps.app.goo.gl/xyz987", PlaceIdService.extractMapsUrl(shared))
    }

    @Test
    fun `expandShortUrl stops cleanly on redirect loop and keeps place url`() {
        val placeUrl = "https://www.google.com/maps/place/Padaria+Central/@-23.5505,-46.6333,17z"
        val encodedContinue = URLEncoder.encode(placeUrl, StandardCharsets.UTF_8.name())
        val mockClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request()
                val url = req.url.toString()
                when {
                    url.contains("maps.app.goo.gl") -> Response.Builder()
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .code(302)
                        .message("Found")
                        .header("Location", "https://consent.google.com/ml?continue=$encodedContinue")
                        .body("".toResponseBody(null))
                        .build()
                    else -> Response.Builder()
                        .request(req)
                        .protocol(Protocol.HTTP_1_1)
                        .code(302)
                        .message("Found")
                        .header("Location", "https://consent.google.com/loop")
                        .body("".toResponseBody(null))
                        .build()
                }
            }
            .build()

        val service = PlaceIdService(mockClient)
        val expanded = service.expandShortUrl("https://maps.app.goo.gl/test123")
        assertEquals(placeUrl, expanded)
    }

    @Test
    fun `parseExpandedMapsUrl decodes place name and extracts coordinates`() {
        val expandedUrl =
            "https://www.google.com/maps/place/Padaria+S%C3%A3o+Jo%C3%A3o/@-23.5505199,-46.6333094,17z/data=!3m1!4b1"

        val parsed = PlaceIdService.parseExpandedMapsUrl(expandedUrl)

        assertEquals("Padaria São João", parsed.placeName)
        assertNotNull(parsed.latitude)
        assertNotNull(parsed.longitude)
        assertEquals(-23.5505199, parsed.latitude!!, 0.000001)
        assertEquals(-46.6333094, parsed.longitude!!, 0.000001)
    }

    @Test
    fun `buildWriteReviewUrl formats direct review link with placeId`() {
        val reviewUrl = PlaceIdService.buildWriteReviewUrl("ChIJrTLr-GyuEmsRBfy61i59si0")
        assertEquals(
            "https://search.google.com/local/writereview?placeid=ChIJrTLr-GyuEmsRBfy61i59si0",
            reviewUrl
        )
    }
}

