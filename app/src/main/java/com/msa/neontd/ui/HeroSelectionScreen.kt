package com.msa.neontd.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msa.neontd.game.heroes.HeroDefinition
import com.msa.neontd.game.heroes.HeroDefinitions
import com.msa.neontd.game.heroes.HeroId
import com.msa.neontd.game.heroes.HeroPassiveType
import com.msa.neontd.game.heroes.HeroProgress
import com.msa.neontd.game.heroes.HeroSystemData
import com.msa.neontd.game.heroes.HeroUnlockRequirement

// Neon color palette
private val NeonBackground = Color(0xFF0A0A12)
private val NeonDarkPanel = Color(0xFF0D0D18)
private val NeonCyan = Color(0xFF00FFFF)
private val NeonMagenta = Color(0xFFFF00FF)
private val NeonYellow = Color(0xFFFFFF00)
private val NeonGreen = Color(0xFF00FF00)
private val NeonOrange = Color(0xFFFF8800)

/**
 * Hero selection screen with neon-themed UI.
 * Shows all heroes with unlock status, level, and abilities.
 *
 * @param heroData Current hero system data
 * @param onHeroSelected Called when a hero is selected (persists selection)
 * @param onStartGame Called when play button is pressed with selected hero
 * @param onBackClick Called when back button is pressed
 */
@Composable
fun HeroSelectionScreen(
    heroData: HeroSystemData,
    onHeroSelected: (HeroId) -> Unit,
    onStartGame: (HeroId) -> Unit,
    onBackClick: () -> Unit
) {
    var selectedHero by remember { mutableStateOf(heroData.getSelectedHero() ?: HeroId.COMMANDER_VOLT) }

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
                text = "SELECT HERO",
                color = NeonCyan,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Hero list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(HeroDefinitions.allHeroes) { hero ->
                    val isUnlocked = heroData.isHeroUnlocked(hero.id)
                    val isSelected = hero.id == selectedHero
                    val progress = heroData.getHeroProgress(hero.id)

                    HeroCard(
                        hero = hero,
                        progress = progress,
                        isUnlocked = isUnlocked,
                        isSelected = isSelected,
                        onClick = {
                            if (isUnlocked) {
                                selectedHero = hero.id
                                onHeroSelected(hero.id)
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Play button
            Button(
                onClick = { onStartGame(selectedHero) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen.copy(alpha = 0.25f),
                    contentColor = NeonGreen
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                val heroName = HeroDefinitions.getById(selectedHero).name
                Text(
                    text = "PLAY WITH $heroName",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Back button
            Button(
                onClick = onBackClick,
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
                    text = "< BACK TO LEVELS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HeroCard(
    hero: HeroDefinition,
    progress: HeroProgress,
    isUnlocked: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val heroColor = Color(hero.primaryColor)
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> heroColor
            isUnlocked -> heroColor.copy(alpha = 0.4f)
            else -> Color.Gray.copy(alpha = 0.3f)
        },
        animationSpec = tween(200),
        label = "border"
    )

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.02f else 1f,
        animationSpec = tween(200),
        label = "scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isUnlocked) 1f else 0.6f,
        animationSpec = tween(200),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(alpha)
            .clip(RoundedCornerShape(12.dp))
            .background(NeonDarkPanel)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            // Hero header: Name and title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = hero.name,
                        color = heroColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = hero.title,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }

                // Level badge or lock icon
                if (isUnlocked) {
                    LevelBadge(progress.currentLevel, heroColor)
                } else {
                    LockBadge(hero.id)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Description
            Text(
                text = hero.description,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Affected towers
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BOOSTS: ",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                hero.affectedTowerTypes.forEachIndexed { index, tower ->
                    if (index > 0) {
                        Text(
                            text = ", ",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = tower.displayName,
                        color = heroColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Passive bonuses
            PassiveBonusesList(hero, progress.currentLevel, heroColor, isUnlocked)

            Spacer(modifier = Modifier.height(8.dp))

            // Active ability
            ActiveAbilitySection(hero, heroColor, isUnlocked)

            // XP Progress bar (only if unlocked)
            if (isUnlocked && !progress.isMaxLevel) {
                Spacer(modifier = Modifier.height(12.dp))
                XPProgressBar(progress, heroColor)
            }
        }
    }
}

@Composable
private fun LevelBadge(level: Int, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "LV.$level",
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LockBadge(heroId: HeroId) {
    val requirement = HeroDefinitions.getUnlockRequirement(heroId)
    val requirementText = when (requirement) {
        is HeroUnlockRequirement.Free -> "FREE"
        is HeroUnlockRequirement.LevelsCompleted -> "Complete ${requirement.count} levels"
        is HeroUnlockRequirement.PrestigeLevel -> "Prestige ${requirement.level}"
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "LOCKED",
            color = Color.Red.copy(alpha = 0.8f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = requirementText,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp
        )
    }
}

@Composable
private fun PassiveBonusesList(
    hero: HeroDefinition,
    currentLevel: Int,
    heroColor: Color,
    isUnlocked: Boolean
) {
    Column {
        Text(
            text = "PASSIVE BONUSES",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))

        // Group bonuses by unlock level
        val bonusesByLevel = hero.passiveBonuses.groupBy { it.unlocksAtLevel }
        bonusesByLevel.forEach { (level, bonuses) ->
            val isActive = isUnlocked && currentLevel >= level
            val bonusText = bonuses.joinToString(", ") { bonus ->
                when (bonus.type) {
                    HeroPassiveType.DAMAGE_MULTIPLIER -> "+${(bonus.value * 100).toInt()}% DMG"
                    HeroPassiveType.RANGE_MULTIPLIER -> "+${(bonus.value * 100).toInt()}% Range"
                    HeroPassiveType.DOT_MULTIPLIER -> "+${(bonus.value * 100).toInt()}% DOT"
                    HeroPassiveType.FIRE_RATE_MULTIPLIER -> "+${(bonus.value * 100).toInt()}% Fire Rate"
                    HeroPassiveType.STARTING_GOLD_BONUS -> "+${bonus.value.toInt()} Gold"
                    HeroPassiveType.TOWER_COST_REDUCTION -> "-${(bonus.value * 100).toInt()}% Cost"
                    HeroPassiveType.ABILITY_COOLDOWN_REDUCTION -> "-${(bonus.value * 100).toInt()}% CD"
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.alpha(if (isActive) 1f else 0.4f)
            ) {
                Text(
                    text = "L$level",
                    color = if (isActive) heroColor else Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(24.dp)
                )
                Text(
                    text = bonusText,
                    color = if (isActive) Color.White else Color.Gray,
                    fontSize = 12.sp
                )
                if (isActive) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ACTIVE",
                        color = NeonGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveAbilitySection(hero: HeroDefinition, heroColor: Color, isUnlocked: Boolean) {
    val ability = hero.activeAbility

    Column {
        Text(
            text = "ACTIVE ABILITY",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(heroColor.copy(alpha = 0.1f))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ability.name,
                    color = heroColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = ability.description,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${ability.cooldownSeconds.toInt()}s",
                    color = heroColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "COOLDOWN",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 8.sp
                )
            }
        }
    }
}

@Composable
private fun XPProgressBar(progress: HeroProgress, heroColor: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "XP: ${progress.currentXP}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
            Text(
                text = "Next: ${progress.getXPForNextLevel()}",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress.getXPProgress() },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = heroColor,
            trackColor = heroColor.copy(alpha = 0.2f)
        )
    }
}
