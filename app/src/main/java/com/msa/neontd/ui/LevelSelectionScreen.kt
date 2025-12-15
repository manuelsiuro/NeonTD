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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msa.neontd.game.level.LevelDefinition
import com.msa.neontd.game.level.LevelDifficulty
import com.msa.neontd.game.level.LevelRegistry
import com.msa.neontd.game.level.ProgressionData

// Neon color palette (matching EncyclopediaScreen)
private val NeonBackground = Color(0xFF0A0A12)
private val NeonDarkPanel = Color(0xFF0D0D18)
private val NeonCyan = Color(0xFF00FFFF)
private val NeonMagenta = Color(0xFFFF00FF)
private val NeonYellow = Color(0xFFFFFF00)
private val NeonGreen = Color(0xFF00FF00)
private val NeonOrange = Color(0xFFFF8800)

/**
 * Level selection screen with neon-themed UI.
 * Shows all levels with unlock status, difficulty badges, and stars.
 *
 * @param progression Current player progression data
 * @param devModeEnabled Whether developer mode is enabled (shows all levels as unlocked)
 * @param onLevelSelected Called when a level is selected, with level ID
 * @param onBackClick Called when back button is pressed
 */
@Composable
fun LevelSelectionScreen(
    progression: ProgressionData,
    devModeEnabled: Boolean,
    onLevelSelected: (Int) -> Unit,
    onBackClick: () -> Unit
) {
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
            LevelSelectionTitle()

            Spacer(modifier = Modifier.height(8.dp))

            // Progress summary
            ProgressSummary(progression)

            Spacer(modifier = Modifier.height(12.dp))

            // Level list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(LevelRegistry.levels) { level ->
                    val isUnlocked = devModeEnabled || progression.isUnlocked(level.id)
                    val isCompleted = progression.isCompleted(level.id)
                    val stars = progression.getStars(level.id)

                    LevelCard(
                        level = level,
                        isUnlocked = isUnlocked,
                        isCompleted = isCompleted,
                        stars = stars,
                        onClick = { if (isUnlocked) onLevelSelected(level.id) }
                    )
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
                    text = "◄ BACK TO MENU",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LevelSelectionTitle() {
    Text(
        text = "SELECT LEVEL",
        color = NeonCyan,
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@Composable
private fun ProgressSummary(progression: ProgressionData) {
    val totalStars = progression.getTotalStars()
    val maxStars = progression.getMaxStars()
    val completedCount = progression.getCompletedCount()
    val totalLevels = LevelRegistry.totalLevels

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NeonDarkPanel)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Stars count
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "★ $totalStars / $maxStars",
                color = NeonYellow,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "STARS",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        }

        // Vertical divider
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(30.dp)
                .background(Color.White.copy(alpha = 0.2f))
        )

        // Levels completed
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$completedCount / $totalLevels",
                color = NeonGreen,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "LEVELS",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun LevelCard(
    level: LevelDefinition,
    isUnlocked: Boolean,
    isCompleted: Boolean,
    stars: Int,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            !isUnlocked -> Color.Gray.copy(alpha = 0.3f)
            isCompleted -> NeonGreen.copy(alpha = 0.6f)
            else -> NeonCyan.copy(alpha = 0.5f)
        },
        animationSpec = tween(300),
        label = "borderColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            !isUnlocked -> NeonDarkPanel.copy(alpha = 0.3f)
            else -> NeonDarkPanel
        },
        animationSpec = tween(300),
        label = "backgroundColor"
    )

    val scale by animateFloatAsState(
        targetValue = if (isUnlocked) 1f else 0.98f,
        animationSpec = tween(300),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = isUnlocked, onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Level number badge
            LevelNumberBadge(
                levelId = level.id,
                isUnlocked = isUnlocked,
                difficulty = level.difficulty
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Level info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = level.name,
                    color = if (isUnlocked) Color.White else Color.Gray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = level.description,
                    color = if (isUnlocked) Color.White.copy(alpha = 0.7f) else Color.Gray.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DifficultyBadge(level.difficulty, isUnlocked)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${level.totalWaves} waves",
                        color = if (isUnlocked) Color.White.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.3f),
                        fontSize = 11.sp
                    )
                }
            }

            // Stars (if completed)
            if (isCompleted && stars > 0) {
                StarDisplay(stars = stars)
            }
        }
    }
}

@Composable
private fun LevelNumberBadge(
    levelId: Int,
    isUnlocked: Boolean,
    difficulty: LevelDifficulty
) {
    val difficultyColor = getDifficultyColor(difficulty)

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isUnlocked) difficultyColor.copy(alpha = 0.2f)
                else Color.Gray.copy(alpha = 0.1f)
            )
            .border(
                2.dp,
                if (isUnlocked) difficultyColor else Color.Gray.copy(alpha = 0.3f),
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isUnlocked) {
            Text(
                text = "$levelId",
                color = difficultyColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = "🔒",
                fontSize = 20.sp
            )
        }
    }
}

@Composable
private fun DifficultyBadge(difficulty: LevelDifficulty, isUnlocked: Boolean) {
    val color = getDifficultyColor(difficulty)
    val alpha = if (isUnlocked) 1f else 0.5f

    Box(
        modifier = Modifier
            .alpha(alpha)
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = difficulty.name,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StarDisplay(stars: Int) {
    Row {
        repeat(3) { index ->
            Text(
                text = if (index < stars) "★" else "☆",
                color = if (index < stars) NeonYellow else Color.Gray,
                fontSize = 18.sp
            )
        }
    }
}

private fun getDifficultyColor(difficulty: LevelDifficulty): Color {
    return when (difficulty) {
        LevelDifficulty.EASY -> NeonGreen
        LevelDifficulty.NORMAL -> NeonCyan
        LevelDifficulty.HARD -> NeonOrange
        LevelDifficulty.EXTREME -> NeonMagenta
    }
}
