package com.pedraotools.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Paleta Oficial inspirada no NFC Tools
val NfcBackground = Color(0xFF1E1E1E)
val NfcListItemBackground = Color(0xFF252525)
val NfcButtonBackground = Color(0xFF2A2A2A)
val NfcDividerColor = Color(0xFF333333)
val NfcButtonBorder = Color(0xFF888888)
val NfcTextPrimary = Color(0xFFFFFFFF)
val NfcTextSecondary = Color(0xFFAAAAAA)
val NfcAccentGreen = Color(0xFF4CAF50)
val NfcDangerRed = Color(0xFFE57373)

private val PedraoDarkColorScheme = darkColorScheme(
    primary = NfcTextPrimary,
    onPrimary = NfcBackground,
    secondary = NfcTextSecondary,
    onSecondary = NfcBackground,
    background = NfcBackground,
    onBackground = NfcTextPrimary,
    surface = NfcListItemBackground,
    onSurface = NfcTextPrimary,
    surfaceVariant = NfcButtonBackground,
    onSurfaceVariant = NfcTextSecondary,
    outline = NfcButtonBorder
)

@Composable
fun PedraoToolsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PedraoDarkColorScheme,
        content = content
    )
}

/**
 * Barra superior utilitária estilo NFC Tools com fundo escuro e linha divisória sutil.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcToolsTopBar(
    title: String,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = title,
                        color = NfcTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            color = NfcTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            navigationIcon = {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = NfcTextPrimary
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = NfcListItemBackground,
                titleContentColor = NfcTextPrimary,
                navigationIconContentColor = NfcTextPrimary
            )
        )
        HorizontalDivider(color = NfcDividerColor, thickness = 1.dp)
    }
}

/**
 * Linha de lista estilo NFC Tools:
 * Ícone branco à esquerda, Título em branco (#FFFFFF), Subtítulo em cinza (#AAAAAA)
 * e seta (>) à direita indicando navegação, com fundo #252525 e divisória #333333.
 */
@Composable
fun NfcToolsMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NfcListItemBackground)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = NfcTextPrimary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = NfcTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = NfcTextSecondary,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Abrir",
                tint = NfcTextSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
        HorizontalDivider(color = NfcDividerColor, thickness = 1.dp)
    }
}

/**
 * Botão de ação estilo NFC Tools:
 * Formato retangular, fundo escuro (#2A2A2A), borda fina cinza clara (#888888)
 * e ícone redondo branco à esquerda do texto (como os botões "Cancelar" e "OK" do NFC Tools).
 */
@Composable
fun NfcToolsActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isHighlighted: Boolean = false
) {
    val borderColor = if (isHighlighted) NfcTextPrimary else NfcButtonBorder
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = NfcButtonBackground,
            contentColor = NfcTextPrimary,
            disabledContainerColor = NfcButtonBackground.copy(alpha = 0.5f),
            disabledContentColor = NfcTextSecondary
        ),
        border = BorderStroke(1.dp, if (enabled) borderColor else NfcDividerColor),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (enabled) NfcTextPrimary else NfcTextSecondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = NfcButtonBackground,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                color = if (enabled) NfcTextPrimary else NfcTextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

