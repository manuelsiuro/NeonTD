package com.msa.neontd.ui.achievements

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msa.neontd.game.achievements.AchievementCategory
import com.msa.neontd.game.achievements.AchievementDefinition
import com.msa.neontd.game.achievements.AchievementDefinitions
import com.msa.neontd.game.achievements.AchievementRarity
import com.msa.neontd.game.achievements.AchievementRepository

// Neon color palette
private val NeonBackground = Color(0xFF0A0A12)
private val NeonDarkPanel = Color(0xFF0D0D18)
private val NeonCyan = Color(0xFF00FFFF)
private val NeonMagenta = Color(0xFFFF00FF)
private val NeonYellow = Color(0xFFFFFF00)
private val NeonGreen = Color(0xFF00FF00)
private val NeonOrange = Color(0xFFFF8800)
private val NeonPurple = Color(0xFF9900FF)
private val NeonBlue = Color(0xFF3388FF)
private val NeonRed = Color(0xFFFF3366)
private val NeonGold = Color(0xFFFFD700)

// Category colors
private fun getCategoryColor(category: AchievementCategory): Color = when (category) {
    AchievementCategory.COMPLETION -> NeonCyan
    AchievementCategory.TOWER -> NeonOrange
    AchievementCategory.COMBAT -> NeonRed
    AchievementCategory.ECONOMY -> NeonGold
    AchievementCategory.CHALLENGE -> NeonPurple
}

// Rarity colors
private fun getRarityColor(rarity: AchievementRarity): Color = when (rarity) {
    AchievementRarity.COMMON -> Color.White
    AchievementRarity.UNCOMMON -> NeonGreen
    AchievementRarity.RARE -> NeonBlue
    AchievementRarity.EPIC -> NeonPurple
    AchievementRarity.LEGENDARY -> NeonGold
}

@Composable
fun AchievementsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { AchievementRepository(context) }
    val achievementData = remember { repository.loadData() }
    var selectedCategory by remember { mutableStateOf(AchievementCategory.COMPLETION) }

    val unlockedCount = achievementData.getUnlockedCount()
    val totalCount = AchievementDefinitions.totalCount

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
            AchievementTitle(unlockedCount, totalCount)

            Spacer(modifier = Modifier.height(12.dp))

            // Stats summary
            StatsSummary(achievementData.playerStats)

            Spacer(modifier = Modifier.height(12.dp))

            // Category tabs
            CategoryTabRow(
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Achievement list
            AnimatedContent(
                targetState = selectedCategory,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(tween(200)) + slideInHorizontally { it / 4 }) togetherWith
                            (fadeOut(tween(200)) + slideOutHorizontally { -it / 4 })
                },
                label = "categoryContent"
            ) { category ->
                val achievements = AchievementDefinitions.getByCategory(category)
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(achievements) { achievement ->
                        val isUnlocked = achievementData.isAchievementUnlocked(achievement.id)
                        val progress = achievementData.getProgress(achievement.id)
                        AchievementCard(
                            achievement = achievement,
                            isUnlocked = isUnlocked,
                            progress = progress
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

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
                    text = "< BACK TO MENU",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AchievementTitle(unlockedCount: Int, totalCount: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ACHIEVEMENTS",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = NeonCyan,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$unlockedCount / $totalCount Unlocked",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StatsSummary(stats: com.msa.neontd.game.achievements.PlayerStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NeonDarkPanel, RoundedCornerShape(8.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(label = "KILLS", value = stats.totalEnemiesKilled.toString(), color = NeonRed)
        StatItem(label = "TOWERS", value = stats.totalTowersPlaced.toString(), color = NeonOrange)
        StatItem(label = "GOLD", value = formatGold(stats.totalGoldEarned), color = NeonGold)
        StatItem(label = "WINS", value = stats.totalVictories.toString(), color = NeonGreen)
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
    }
}

private fun formatGold(gold: Int): String {
    return when {
        gold >= 1_000_000 -> "${gold / 1_000_000}M"
        gold >= 1_000 -> "${gold / 1_000}K"
        else -> gold.toString()
    }
}

@Composable
private fun CategoryTabRow(
    selectedCategory: AchievementCategory,
    onCategorySelected: (AchievementCategory) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AchievementCategory.entries.forEach { category ->
            val isSelected = category == selectedCategory
            val color = getCategoryColor(category)
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent,
                label = "tabBg"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(bgColor, RoundedCornerShape(6.dp))
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) color else color.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable { onCategorySelected(category) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getCategoryShortName(category),
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) color else color.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
        }
    }
}

private fun getCategoryShortName(category: AchievementCategory): String = when (category) {
    AchievementCategory.COMPLETION -> "STORY"
    AchievementCategory.TOWER -> "TOWER"
    AchievementCategory.COMBAT -> "COMBAT"
    AchievementCategory.ECONOMY -> "GOLD"
    AchievementCategory.CHALLENGE -> "EXTRA"
}

@Composable
private fun AchievementCard(
    achievement: AchievementDefinition,
    isUnlocked: Boolean,
    progress: Int
) {
    val rarityColor = getRarityColor(achievement.rarity)
    val borderColor = if (isUnlocked) rarityColor else NeonMagenta.copy(alpha = 0.3f)
    val progressPercent = if (achievement.targetValue > 1) {
        (progress.toFloat() / achievement.targetValue).coerceIn(0f, 1f)
    } else {
        if (isUnlocked) 1f else 0f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent,
        animationSpec = tween(500),
        label = "progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isUnlocked) NeonDarkPanel else NeonDarkPanel.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Achievement icon/status
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isUnlocked) rarityColor.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f),
                        CircleShape
                    )
                    .border(2.dp, if (isUnlocked) rarityColor else Color.Gray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isUnlocked) "✓" else "?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) rarityColor else Color.Gray
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Achievement details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUnlocked || !achievement.isHidden) achievement.name else "???",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) rarityColor else Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Progress indicator
                    if (achievement.targetValue > 1) {
                        Text(
                            text = "$progress/${achievement.targetValue}",
                            fontSize = 12.sp,
                            color = if (isUnlocked) NeonGreen else NeonYellow.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (isUnlocked || !achievement.isHidden) achievement.description else "Hidden achievement",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Progress bar for multi-target achievements
                if (achievement.targetValue > 1 && !isUnlocked) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = NeonYellow,
                        trackColor = Color.Gray.copy(alpha = 0.3f)
                    )
                }

                // Reward indicator
                achievement.rewardId?.let { rewardId ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isUnlocked) "★ Reward unlocked!" else "★ Unlocks reward",
                        fontSize = 10.sp,
                        color = if (isUnlocked) NeonGold else NeonGold.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}
