package com.msa.neontd.ui.skins

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msa.neontd.engine.audio.AudioEventHandler
import com.msa.neontd.game.achievements.CosmeticReward
import com.msa.neontd.game.achievements.CosmeticRewards
import com.msa.neontd.game.achievements.TowerSkinsRepository
import com.msa.neontd.game.entities.TowerType

// Neon color palette
private val NeonBackground = Color(0xFF0A0A12)
private val NeonDarkPanel = Color(0xFF0D0D18)
private val NeonCyan = Color(0xFF00FFFF)
private val NeonMagenta = Color(0xFFFF00FF)
private val NeonYellow = Color(0xFFFFFF00)
private val NeonGreen = Color(0xFF00FF00)
private val NeonOrange = Color(0xFFFF8800)
private val NeonPurple = Color(0xFF9900FF)
private val NeonGold = Color(0xFFFFD700)

@Composable
fun TowerSkinsScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { TowerSkinsRepository.getInstance(context) }
    var equippedSkins by remember { mutableStateOf(repository.loadData()) }
    var selectedTowerType by remember { mutableStateOf<TowerType?>(null) }

    // Get all tower types that have skins
    val towerTypesWithSkins = remember { CosmeticRewards.getTowerTypesWithSkins() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeonBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = "TOWER SKINS",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = NeonOrange,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Customize your towers with unique colors",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tower type selection
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(towerTypesWithSkins) { towerType ->
                    val isSelected = selectedTowerType == towerType
                    val hasEquippedSkin = equippedSkins.equippedSkins.containsKey(towerType.name)
                    val towerColor = getTowerDisplayColor(towerType)

                    TowerTypeChip(
                        towerType = towerType,
                        isSelected = isSelected,
                        hasEquippedSkin = hasEquippedSkin,
                        towerColor = towerColor,
                        onClick = {
                            AudioEventHandler.onButtonClick()
                            selectedTowerType = if (selectedTowerType == towerType) null else towerType
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Skin selection for selected tower
            if (selectedTowerType != null) {
                val skins = remember(selectedTowerType) {
                    CosmeticRewards.getSkinsForTower(selectedTowerType!!)
                }
                val equippedSkinId = equippedSkins.equippedSkins[selectedTowerType!!.name]

                SkinSelectionPanel(
                    towerType = selectedTowerType!!,
                    skins = skins,
                    equippedSkinId = equippedSkinId,
                    onSkinSelect = { skinId ->
                        AudioEventHandler.onButtonClick()
                        repository.equipSkin(selectedTowerType!!, skinId)
                        equippedSkins = repository.loadData()
                    },
                    onResetToDefault = {
                        AudioEventHandler.onButtonClick()
                        repository.equipSkin(selectedTowerType!!, null)
                        equippedSkins = repository.loadData()
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                // No tower selected message
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Select a tower to customize",
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Equipped skins will apply to all new towers",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Back button
            Button(
                onClick = {
                    AudioEventHandler.onButtonClick()
                    onBackClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonCyan.copy(alpha = 0.15f),
                    contentColor = NeonCyan
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "< BACK",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TowerTypeChip(
    towerType: TowerType,
    isSelected: Boolean,
    hasEquippedSkin: Boolean,
    towerColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) towerColor.copy(alpha = 0.25f)
                else NeonDarkPanel
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) towerColor else towerColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(towerColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = towerType.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) towerColor else Color.White.copy(alpha = 0.8f)
            )
            if (hasEquippedSkin) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "*",
                    fontSize = 12.sp,
                    color = NeonGold
                )
            }
        }
    }
}

@Composable
private fun SkinSelectionPanel(
    towerType: TowerType,
    skins: List<CosmeticReward>,
    equippedSkinId: String?,
    onSkinSelect: (String) -> Unit,
    onResetToDefault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val towerBaseColor = getTowerDisplayColor(towerType)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NeonDarkPanel)
            .border(
                width = 1.dp,
                color = towerBaseColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        // Tower type header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${towerType.name} TOWER SKINS",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = towerBaseColor
            )

            // Reset to default button
            if (equippedSkinId != null) {
                Text(
                    text = "RESET",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonOrange.copy(alpha = 0.8f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(NeonOrange.copy(alpha = 0.15f))
                        .clickable(onClick = onResetToDefault)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Default skin option
        SkinOption(
            name = "Default",
            description = "Original tower color",
            colorHex = null,
            isEquipped = equippedSkinId == null,
            displayColor = towerBaseColor,
            onClick = onResetToDefault
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Available skins
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(skins) { skin ->
                val skinColor = parseSkinColor(skin.colorHex)
                SkinOption(
                    name = skin.name,
                    description = skin.description,
                    colorHex = skin.colorHex,
                    isEquipped = equippedSkinId == skin.id,
                    displayColor = skinColor ?: towerBaseColor,
                    onClick = { onSkinSelect(skin.id) }
                )
            }
        }
    }
}

@Composable
private fun SkinOption(
    name: String,
    description: String,
    colorHex: String?,
    isEquipped: Boolean,
    displayColor: Color,
    onClick: () -> Unit
) {
    val isRainbow = colorHex == "#RAINBOW"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isEquipped) displayColor.copy(alpha = 0.2f)
                else Color.Transparent
            )
            .border(
                width = if (isEquipped) 2.dp else 1.dp,
                color = if (isEquipped) displayColor else displayColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color preview
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(4.dp))
                .then(
                    if (isRainbow) {
                        Modifier.background(
                            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Red, Color.Yellow, Color.Green,
                                    Color.Cyan, Color.Blue, Color.Magenta
                                )
                            )
                        )
                    } else {
                        Modifier.background(displayColor)
                    }
                )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isEquipped) displayColor else Color.White
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        if (isEquipped) {
            Text(
                text = "EQUIPPED",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = NeonGreen,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(NeonGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

/**
 * Get the display color for a tower type (approximation for UI).
 */
private fun getTowerDisplayColor(towerType: TowerType): Color {
    return when (towerType) {
        TowerType.PULSE -> Color(0xFF00AAFF)
        TowerType.SNIPER -> Color(0xFFFF6600)
        TowerType.SPLASH -> Color(0xFFFF3300)
        TowerType.FLAME -> Color(0xFFFF4400)
        TowerType.SLOW -> Color(0xFF00FFFF)
        TowerType.TESLA -> Color(0xFFAA00FF)
        TowerType.GRAVITY -> Color(0xFF9900FF)
        TowerType.TEMPORAL -> Color(0xFF00FFAA)
        TowerType.LASER -> Color(0xFFFF0044)
        TowerType.POISON -> Color(0xFF00FF00)
        TowerType.MISSILE -> Color(0xFFFF0088)
        TowerType.BUFF -> Color(0xFFFFD700)
        TowerType.DEBUFF -> Color(0xFF660088)
        TowerType.CHAIN -> Color(0xFFFFFF00)
    }
}

/**
 * Parse skin color hex to Compose Color.
 */
private fun parseSkinColor(colorHex: String?): Color? {
    if (colorHex == null || colorHex == "#RAINBOW") return null
    val cleanHex = colorHex.removePrefix("#")
    if (cleanHex.length != 6) return null

    return try {
        val r = cleanHex.substring(0, 2).toInt(16)
        val g = cleanHex.substring(2, 4).toInt(16)
        val b = cleanHex.substring(4, 6).toInt(16)
        Color(r / 255f, g / 255f, b / 255f)
    } catch (e: Exception) {
        null
    }
}
