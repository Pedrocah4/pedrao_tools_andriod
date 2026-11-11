package com.pedraotools.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPasteGo
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pedraotools.app.data.HistoryManager
import com.pedraotools.app.data.PlaceDetails
import com.pedraotools.app.data.PlaceIdService
import com.pedraotools.app.data.SavedClient
import com.pedraotools.app.ui.theme.NfcAccentGreen
import com.pedraotools.app.ui.theme.NfcBackground
import com.pedraotools.app.ui.theme.NfcButtonBorder
import com.pedraotools.app.ui.theme.NfcDangerRed
import com.pedraotools.app.ui.theme.NfcDividerColor
import com.pedraotools.app.ui.theme.NfcListItemBackground
import com.pedraotools.app.ui.theme.NfcTextPrimary
import com.pedraotools.app.ui.theme.NfcTextSecondary
import com.pedraotools.app.ui.theme.NfcToolsActionButton
import com.pedraotools.app.ui.theme.NfcToolsTopBar
import com.pedraotools.app.ui.theme.PedraoToolsTheme
import kotlinx.coroutines.launch

@Composable
fun GoogleReviewScreen(
    onBackClick: () -> Unit,
    placeIdService: PlaceIdService = remember { PlaceIdService() }
) {
    val context = LocalContext.current
    val historyManager = remember(context) { HistoryManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var inputUrl by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentResult by remember { mutableStateOf<PlaceDetails?>(null) }
    var savedClients by remember { mutableStateOf(historyManager.getSavedClients()) }

    fun copyToClipboard(label: String, text: String, toastMessage: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
    }

    fun readFromClipboard(): String {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val item = clipboard.primaryClip?.getItemAt(0)
        return item?.coerceToText(context)?.toString()?.trim().orEmpty()
    }

    fun convertMapsLink(rawUrl: String) {
        val extracted = PlaceIdService.extractMapsUrl(rawUrl)
        if (extracted.isEmpty()) {
            errorMessage = "Nenhum link encontrado. Copie um link do Google Maps primeiro."
            return
        }
        if (!PlaceIdService.isValidMapsUrl(extracted)) {
            errorMessage = "Link inválido. Use um link do Google Maps (ex: https://maps.app.goo.gl/...)."
            return
        }
        inputUrl = extracted

        errorMessage = null
        isLoading = true

        coroutineScope.launch {
            val result = placeIdService.resolveAndFetchPlace(extracted)
            isLoading = false
            result.onSuccess { place ->
                currentResult = place
                copyToClipboard(
                    label = "Google Review Link",
                    text = place.reviewUrl,
                    toastMessage = "Link de avaliação copiado!"
                )
            }.onFailure { throwable ->
                errorMessage = throwable.message ?: "Erro inesperado ao converter o link."
            }
        }
    }

    Scaffold(
        containerColor = NfcBackground,
        topBar = {
            NfcToolsTopBar(
                title = "Google Review Link",
                subtitle = "Converter link do Maps em avaliação direta",
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(NfcBackground)
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // A. Entrada e Ação em 1 Toque (Clipboard-First)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    NfcToolsActionButton(
                        text = "Colar do Maps e Converter",
                        icon = Icons.Default.ContentPasteGo,
                        enabled = !isLoading,
                        isHighlighted = true,
                        onClick = {
                            val clipText = readFromClipboard()
                            if (clipText.isNotBlank()) {
                                inputUrl = clipText
                                convertMapsLink(clipText)
                            } else if (inputUrl.isNotBlank()) {
                                convertMapsLink(inputUrl)
                            } else {
                                errorMessage = "A área de transferência está vazia."
                                Toast.makeText(
                                    context,
                                    "A área de transferência está vazia.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    )

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = {
                            inputUrl = it
                            errorMessage = null
                        },
                        label = {
                            Text(
                                text = "Link do Google Maps (visualizar / editar)",
                                color = NfcTextSecondary
                            )
                        },
                        placeholder = {
                            Text(
                                text = "https://maps.app.goo.gl/...",
                                color = NfcTextSecondary.copy(alpha = 0.6f)
                            )
                        },
                        singleLine = true,
                        enabled = !isLoading,
                        trailingIcon = {
                            if (inputUrl.isNotBlank() && !isLoading) {
                                IconButton(onClick = { convertMapsLink(inputUrl) }) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = "Converter link digitado",
                                        tint = NfcTextPrimary
                                    )
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = NfcTextPrimary,
                            unfocusedTextColor = NfcTextPrimary,
                            disabledTextColor = NfcTextSecondary,
                            focusedContainerColor = NfcListItemBackground,
                            unfocusedContainerColor = NfcListItemBackground,
                            disabledContainerColor = NfcListItemBackground,
                            focusedBorderColor = NfcTextPrimary,
                            unfocusedBorderColor = NfcDividerColor,
                            cursorColor = NfcTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (isLoading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(NfcListItemBackground, RoundedCornerShape(4.dp))
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = NfcTextPrimary,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Expandindo link e consultando Google Places...",
                                color = NfcTextPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    errorMessage?.let { msg ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = NfcListItemBackground),
                            border = BorderStroke(1.dp, NfcDangerRed),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                color = NfcDangerRed,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            // C. Card de Confirmação Visual e Salvamento Manual
            currentResult?.let { result ->
                item {
                    PlaceConfirmationCard(
                        place = result,
                        isAlreadySaved = savedClients.any { it.placeId == result.placeId },
                        onCopyAgain = {
                            copyToClipboard(
                                label = "Google Review Link",
                                text = result.reviewUrl,
                                toastMessage = "Link de avaliação copiado!"
                            )
                        },
                        onSaveToHistory = {
                            savedClients = historyManager.saveClient(
                                SavedClient(
                                    placeId = result.placeId,
                                    name = result.displayName,
                                    address = result.formattedAddress,
                                    reviewUrl = result.reviewUrl
                                )
                            )
                            Toast.makeText(
                                context,
                                "Cliente salvo no histórico!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }

            // D. Seção de Histórico Manual ("Clientes Salvos")
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = NfcDividerColor, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Clientes Salvos (${savedClients.size})",
                        color = NfcTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Apenas os locais salvos manualmente aparecem aqui",
                        color = NfcTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            if (savedClients.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NfcListItemBackground, RoundedCornerShape(4.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Nenhum cliente salvo ainda.",
                            color = NfcTextSecondary,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                items(
                    items = savedClients,
                    key = { it.placeId }
                ) { client ->
                    SavedClientRow(
                        client = client,
                        onCopyLink = {
                            copyToClipboard(
                                label = client.name,
                                text = client.reviewUrl,
                                toastMessage = "Link de avaliação copiado!"
                            )
                        },
                        onDelete = {
                            savedClients = historyManager.deleteClient(client.placeId)
                            Toast.makeText(
                                context,
                                "Removido do histórico",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceConfirmationCard(
    place: PlaceDetails,
    isAlreadySaved: Boolean,
    onCopyAgain: () -> Unit,
    onSaveToHistory: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NfcListItemBackground),
        border = BorderStroke(1.dp, NfcButtonBorder),
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "LOCAL ENCONTRADO",
                color = NfcAccentGreen,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Nome Oficial Encontrado:",
                    color = NfcTextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = place.displayName,
                    color = NfcTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Endereço Completo:",
                    color = NfcTextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = place.formattedAddress,
                    color = NfcTextPrimary,
                    fontSize = 13.sp
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Link Gerado:",
                    color = NfcTextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = place.reviewUrl,
                    color = NfcTextPrimary,
                    fontSize = 12.sp
                )
            }

            HorizontalDivider(
                color = NfcDividerColor,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NfcToolsActionButton(
                    text = "Copiar Novamente",
                    icon = Icons.Default.ContentCopy,
                    onClick = onCopyAgain,
                    modifier = Modifier.fillMaxWidth()
                )

                NfcToolsActionButton(
                    text = if (isAlreadySaved) "Atualizar no Histórico" else "Salvar no Histórico",
                    icon = Icons.Default.BookmarkAdd,
                    onClick = onSaveToHistory,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SavedClientRow(
    client: SavedClient,
    onCopyLink: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NfcListItemBackground, RoundedCornerShape(4.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = client.name,
                    color = NfcTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = client.address,
                    color = NfcTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(10.dp))
                NfcToolsActionButton(
                    text = "Copiar Link",
                    icon = Icons.Default.ContentCopy,
                    onClick = onCopyLink
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Excluir do histórico",
                    tint = NfcTextSecondary
                )
            }
        }
        HorizontalDivider(color = NfcDividerColor, thickness = 1.dp)
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceConfirmationCardPreview() {
    PedraoToolsTheme {
        Box(modifier = Modifier.background(NfcBackground).padding(16.dp)) {
            PlaceConfirmationCard(
                place = PlaceDetails(
                    placeId = "ChIJrTLr-GyuEmsRBfy61i59si0",
                    displayName = "Padaria Estrela Paulista",
                    formattedAddress = "Av. Paulista, 1000 - Bela Vista, São Paulo - SP, Brasil",
                    reviewUrl = "https://search.google.com/local/writereview?placeid=ChIJrTLr-GyuEmsRBfy61i59si0",
                    expandedUrl = "https://www.google.com/maps/place/Padaria+Estrela+Paulista/@-23.564,-46.652,17z"
                ),
                isAlreadySaved = false,
                onCopyAgain = {},
                onSaveToHistory = {}
            )
        }
    }
}

