package com.pedraotools.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pedraotools.app.ui.theme.NfcBackground
import com.pedraotools.app.ui.theme.NfcToolsMenuRow
import com.pedraotools.app.ui.theme.NfcToolsTopBar
import com.pedraotools.app.ui.theme.PedraoToolsTheme

@Composable
fun HomeScreen(
    onOpenGoogleReview: () -> Unit,
    onOpenPlaceholder: () -> Unit
) {
    Scaffold(
        containerColor = NfcBackground,
        topBar = {
            NfcToolsTopBar(
                title = "Pedrao Tools",
                subtitle = "Hub de ferramentas utilitárias"
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NfcBackground)
                .padding(innerPadding)
        ) {
            // Item 1: Google Review Link
            NfcToolsMenuRow(
                icon = Icons.Default.StarRate,
                title = "Google Review Link",
                subtitle = "Converter link do Maps em link de avaliação direta",
                onClick = onOpenGoogleReview
            )

            // Item 2: Na próxima versão...
            NfcToolsMenuRow(
                icon = Icons.Default.Build,
                title = "Na próxima versão...",
                subtitle = "Novas ferramentas em breve",
                onClick = onOpenPlaceholder
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    PedraoToolsTheme {
        HomeScreen(
            onOpenGoogleReview = {},
            onOpenPlaceholder = {}
        )
    }
}

