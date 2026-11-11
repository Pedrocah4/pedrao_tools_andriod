package com.pedraotools.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.pedraotools.app.ui.theme.NfcBackground
import com.pedraotools.app.ui.theme.NfcToolsTopBar
import com.pedraotools.app.ui.theme.PedraoToolsTheme

@Composable
fun PlaceholderScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        containerColor = NfcBackground,
        topBar = {
            NfcToolsTopBar(
                title = "Na próxima versão...",
                subtitle = "Novas ferramentas em breve",
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NfcBackground)
                .padding(innerPadding)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderScreenPreview() {
    PedraoToolsTheme {
        PlaceholderScreen(onBackClick = {})
    }
}

