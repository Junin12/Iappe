package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.CoverTemplateStyle
import com.example.data.model.CoverThumbnailState
import com.example.ui.theme.*

@Composable
fun CoverGeneratorModal(
    state: CoverThumbnailState,
    onClose: () -> Unit,
    onStyleChange: (CoverTemplateStyle) -> Unit,
    onHeadlineChange: (String) -> Unit,
    onPriceTagChange: (String) -> Unit,
    onSaveCover: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = ObsidianSurface,
            border = BorderStroke(1.dp, ObsidianBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🖼️ Gerador de Capa TikTok Shop",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "A miniatura que faz a pessoa clicar no feed de produtos",
                            color = Gold18k,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = onClose, modifier = Modifier.testTag("close_cover_modal")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fechar", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 9:16 Mini Preview of the Cover Thumbnail
                Box(
                    modifier = Modifier
                        .height(260.dp)
                        .aspectRatio(9f / 16f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            when (state.style) {
                                CoverTemplateStyle.OLD_MONEY -> Brush.verticalGradient(
                                    listOf(Color(0xFF1F1610), Color(0xFF140D07), Color(0xFF0A0703))
                                )
                                CoverTemplateStyle.TIKTOK_SHOP_VIRAL -> Brush.verticalGradient(
                                    listOf(Color(0xFF280B22), Color(0xFF13061C), Color(0xFF0A0210))
                                )
                                CoverTemplateStyle.MINIMAL_BLACK -> Brush.verticalGradient(
                                    listOf(Color(0xFF0F172A), Color(0xFF050B14), Color(0xFF000000))
                                )
                                CoverTemplateStyle.URGENT_SALE -> Brush.verticalGradient(
                                    listOf(Color(0xFF3B0B0B), Color(0xFF1F0505), Color(0xFF0F0000))
                                )
                            }
                        )
                        .border(
                            2.dp,
                            when (state.style) {
                                CoverTemplateStyle.OLD_MONEY -> Gold18k
                                CoverTemplateStyle.TIKTOK_SHOP_VIRAL -> TikTokPink
                                CoverTemplateStyle.MINIMAL_BLACK -> TikTokCyan
                                CoverTemplateStyle.URGENT_SALE -> Color(0xFFFF4500)
                            },
                            RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Top Badge
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = state.style.badgeColor.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, state.style.badgeColor)
                        ) {
                            Text(
                                text = "✨ TIKTOK SHOP DESTAQUE",
                                color = state.style.badgeColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        // Center Big Headline
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = state.mainHeadline.ifBlank { "SUA JOIA AQUI" },
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "💎 💎 💎",
                                fontSize = 14.sp
                            )
                        }

                        // Bottom Price Tag & Guarantee
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFFE600) // TikTok yellow
                            ) {
                                Text(
                                    text = "APENAS ${state.priceTag}",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = state.subtitleTag,
                                color = TextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Style Selector
                Text(text = "Estilo Visual da Capa:", color = TextMuted, fontSize = 11.sp, modifier = Modifier.align(Alignment.Start))
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CoverTemplateStyle.values()) { style ->
                        val isSelected = state.style == style
                        Surface(
                            onClick = { onStyleChange(style) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) ObsidianSurfaceHighlight else ObsidianSurfaceElevated,
                            border = BorderStroke(1.dp, if (isSelected) style.badgeColor else ObsidianBorder),
                            modifier = Modifier.testTag("cover_style_${style.name}")
                        ) {
                            Text(
                                text = style.displayName,
                                color = if (isSelected) style.badgeColor else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Editable Fields
                OutlinedTextField(
                    value = state.mainHeadline,
                    onValueChange = onHeadlineChange,
                    label = { Text("Título da Capa") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TikTokPink,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("cover_headline_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = state.priceTag,
                    onValueChange = onPriceTagChange,
                    label = { Text("Selo de Preço / Oferta") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Gold18k,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("cover_pricetag_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                GlowButton(
                    text = "Salvar Capa para Publicação",
                    icon = { Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color.White) },
                    onClick = {
                        onSaveCover()
                        onClose()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "save_cover_button"
                )
            }
        }
    }
}
