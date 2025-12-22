package com.msa.neontd.ui.prestige

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msa.neontd.game.prestige.PrestigeData
import com.msa.neontd.game.prestige.PrestigePreview

// Neon color palette
private object NeonColors {
    val cyan = Color(0xFF00FFFF)
    val green = Color(0xFF00FF00)
    val red = Color(0xFFFF0044)
    val yellow = Color(0xFFFFFF00)
    val orange = Color(0xFFFF8800)
    val blue = Color(0xFF0088FF)
    val purple = Color(0xFF8800FF)
}

/**
 * Prestige screen showing current prestige status and ability to prestige.
 */
@Composable
fun PrestigeScreen(
    prestigeData: PrestigeData,
    completedLevels: Int,
    onPrestige: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canPrestige = prestigeData.canPrestige(completedLevels)
    val preview = PrestigePreview.fromPrestigeData(prestigeData)
    val tierColor = Color(prestigeData.getPrestigeTierColor())

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0A1A),
                        Color(0xFF1A0A2A),
                        Color(0xFF0A1A2A)
                    )
                )
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title
        Text(
            text = "PRESTIGE",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = NeonColors.cyan,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Current Tier Display
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.5f))
                .border(2.dp, tierColor, RoundedCornerShape(12.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = prestigeData.getPrestigeTierName(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = tierColor
                )
                Text(
                    text = "Prestige Level ${prestigeData.currentPrestigeLevel}",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                if (prestigeData.isMaxPrestige) {
                    Text(
                        text = "MAX PRESTIGE",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Current Bonuses Section
            if (prestigeData.currentPrestigeLevel > 0) {
                SectionCard(
                    title = "CURRENT BONUSES",
                    titleColor = NeonColors.green
                ) {
                    Text(
                        text = prestigeData.getBonusesDescription(),
                        fontSize = 14.sp,
                        color = NeonColors.green,
                        lineHeight = 22.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                SectionCard(
                    title = "DIFFICULTY MODIFIERS",
                    titleColor = NeonColors.red
                ) {
                    Text(
                        text = prestigeData.getDifficultyDescription(),
                        fontSize = 14.sp,
                        color = NeonColors.red
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Next Prestige Preview
            if (preview != null) {
                SectionCard(
                    title = "NEXT PRESTIGE: ${preview.newTierName.uppercase()}",
                    titleColor = Color(preview.newTierColor)
                ) {
                    Column {
                        Text(
                            text = "New Bonuses:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        BonusRow("+${preview.goldBonusIncrease}% Starting Gold", NeonColors.yellow)
                        BonusRow("+${preview.damageBonusIncrease}% Tower Damage", NeonColors.cyan)
                        BonusRow("+${preview.rangeBonusIncrease}% Tower Range", NeonColors.blue)
                        BonusRow("+${preview.fireRateBonusIncrease}% Fire Rate", NeonColors.purple)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Difficulty Increase:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        BonusRow("+${preview.enemyHealthIncrease}% Enemy Health", NeonColors.red)
                        BonusRow("+${preview.enemySpeedIncrease}% Enemy Speed", NeonColors.orange)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress to Prestige
                SectionCard(
                    title = "PRESTIGE REQUIREMENTS",
                    titleColor = NeonColors.cyan
                ) {
                    val progress = completedLevels.coerceAtMost(PrestigeData.LEVELS_REQUIRED_TO_PRESTIGE)
                    val required = PrestigeData.LEVELS_REQUIRED_TO_PRESTIGE

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Complete all 30 levels to prestige",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(20.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.5f))
                                .border(1.dp, NeonColors.cyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(progress.toFloat() / required)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (canPrestige) NeonColors.green
                                        else NeonColors.cyan.copy(alpha = 0.7f)
                                    )
                            )
                            Text(
                                text = "$progress / $required",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        if (canPrestige) {
                            Text(
                                text = "Ready to Prestige!",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonColors.green,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            // Warning about prestige
            if (canPrestige) {
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(NeonColors.orange.copy(alpha = 0.2f))
                        .border(1.dp, NeonColors.orange, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Warning: Prestiging will reset your level progress! " +
                                "All unlocked levels will be locked again, but you keep " +
                                "your heroes, achievements, and tower skins.",
                        fontSize = 12.sp,
                        color = NeonColors.orange,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Back button
            Button(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                border = androidx.compose.foundation.BorderStroke(2.dp, NeonColors.cyan)
            ) {
                Text(
                    text = "BACK",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonColors.cyan
                )
            }

            // Prestige button
            if (preview != null) {
                Button(
                    onClick = onPrestige,
                    enabled = canPrestige,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canPrestige) NeonColors.green else Color.Gray.copy(alpha = 0.3f),
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = "PRESTIGE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canPrestige) Color.Black else Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    titleColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black.copy(alpha = 0.4f))
            .border(1.dp, titleColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun BonusRow(text: String, color: Color) {
    Text(
        text = "• $text",
        fontSize = 13.sp,
        color = color,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
