package com.msa.neontd.ui.challenges

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.msa.neontd.game.challenges.ChallengeConfig
import com.msa.neontd.game.challenges.ChallengeData
import com.msa.neontd.game.challenges.ChallengeDefinition
import com.msa.neontd.game.challenges.ChallengeDefinitions
import com.msa.neontd.game.challenges.ChallengeModifiers
import com.msa.neontd.game.challenges.ChallengeRepository
import com.msa.neontd.game.challenges.ChallengeType
import com.msa.neontd.game.challenges.EndlessHighScore
import com.msa.neontd.game.level.MapId
import kotlinx.coroutines.delay

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

// Tab colors
private fun getTabColor(type: ChallengeType): Color = when (type) {
    ChallengeType.DAILY -> NeonCyan
    ChallengeType.WEEKLY -> NeonPurple
    ChallengeType.ENDLESS -> NeonMagenta
    ChallengeType.BOSS_RUSH -> NeonRed
}

enum class ChallengeTab {
    DAILY, WEEKLY, ENDLESS, BOSS_RUSH
}

@Composable
fun ChallengesScreen(
    onBackClick: () -> Unit,
    onPlayChallenge: (ChallengeConfig) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ChallengeRepository(context) }
    var selectedTab by remember { mutableStateOf(ChallengeTab.DAILY) }

    // Load challenge data
    val challengeData = remember { repository.loadData() }
    val dailyChallenge = remember { repository.getCurrentDaily() }
    val weeklyChallenge = remember { repository.getCurrentWeekly() }
    val endlessHighScores = remember { repository.getEndlessLeaderboard() }

    // Timer states
    var dailyTimeRemaining by remember { mutableLongStateOf(repository.getTimeUntilDailyReset()) }
    var weeklyTimeRemaining by remember { mutableLongStateOf(repository.getTimeUntilWeeklyReset()) }

    // Update timers every second
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            dailyTimeRemaining = repository.getTimeUntilDailyReset()
            weeklyTimeRemaining = repository.getTimeUntilWeeklyReset()
        }
    }

    val isDailyCompleted = challengeData.attempts[dailyChallenge.id]?.isCompleted == true
    val isWeeklyCompleted = challengeData.attempts[weeklyChallenge.id]?.isCompleted == true
    val dailyStreak = repository.getDailyStreak()
    val totalCompleted = challengeData.totalChallengesCompleted

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
            // Title with stats
            ChallengesTitle(dailyStreak, totalCompleted)

            Spacer(modifier = Modifier.height(12.dp))

            // Tab row
            ChallengeTabRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Content based on selected tab
            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    (fadeIn(tween(200)) + slideInHorizontally { it / 4 }) togetherWith
                            (fadeOut(tween(200)) + slideOutHorizontally { -it / 4 })
                },
                label = "tabContent"
            ) { tab ->
                when (tab) {
                    ChallengeTab.DAILY -> DailyContent(
                        challenge = dailyChallenge,
                        timeRemaining = dailyTimeRemaining,
                        isCompleted = isDailyCompleted,
                        bestScore = challengeData.attempts[dailyChallenge.id]?.bestScore ?: 0,
                        attempts = challengeData.attempts[dailyChallenge.id]?.attempts ?: 0,
                        onPlay = { onPlayChallenge(ChallengeConfig(dailyChallenge)) }
                    )
                    ChallengeTab.WEEKLY -> WeeklyContent(
                        challenge = weeklyChallenge,
                        timeRemaining = weeklyTimeRemaining,
                        isCompleted = isWeeklyCompleted,
                        bestScore = challengeData.attempts[weeklyChallenge.id]?.bestScore ?: 0,
                        attempts = challengeData.attempts[weeklyChallenge.id]?.attempts ?: 0,
                        onPlay = { onPlayChallenge(ChallengeConfig(weeklyChallenge)) }
                    )
                    ChallengeTab.ENDLESS -> EndlessContent(
                        highScores = endlessHighScores,
                        onPlayMap = { mapId ->
                            val endless = ChallengeDefinitions.generateEndlessChallenge(mapId)
                            onPlayChallenge(ChallengeConfig(endless, isEndlessMode = true))
                        }
                    )
                    ChallengeTab.BOSS_RUSH -> BossRushContent(
                        challengeData = challengeData,
                        onPlayBossRush = { mapId, tierIndex ->
                            val bossRush = ChallengeDefinitions.generateBossRushChallenge(mapId, tierIndex)
                            onPlayChallenge(ChallengeConfig(bossRush))
                        }
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
                    text = "< BACK TO MENU",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ChallengesTitle(dailyStreak: Int, totalCompleted: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CHALLENGES",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = NeonOrange,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (dailyStreak > 0) {
                Text(
                    text = "$dailyStreak Day Streak",
                    fontSize = 14.sp,
                    color = NeonOrange.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Text(
                text = "$totalCompleted Completed",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun ChallengeTabRow(
    selectedTab: ChallengeTab,
    onTabSelected: (ChallengeTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NeonDarkPanel)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ChallengeTab.entries.forEach { tab ->
            val isSelected = selectedTab == tab
            val tabColor = when (tab) {
                ChallengeTab.DAILY -> NeonCyan
                ChallengeTab.WEEKLY -> NeonPurple
                ChallengeTab.ENDLESS -> NeonMagenta
                ChallengeTab.BOSS_RUSH -> NeonRed
            }
            val animatedBgAlpha by animateFloatAsState(
                targetValue = if (isSelected) 0.25f else 0f,
                animationSpec = tween(200),
                label = "tabBgAlpha"
            )
            val animatedTextColor by animateColorAsState(
                targetValue = if (isSelected) tabColor else Color.White.copy(alpha = 0.6f),
                animationSpec = tween(200),
                label = "tabTextColor"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(tabColor.copy(alpha = animatedBgAlpha))
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tab.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = animatedTextColor
                )
            }
        }
    }
}

@Composable
private fun DailyContent(
    challenge: ChallengeDefinition,
    timeRemaining: Long,
    isCompleted: Boolean,
    bestScore: Int,
    attempts: Int,
    onPlay: () -> Unit
) {
    ChallengeCard(
        challenge = challenge,
        timeRemaining = timeRemaining,
        isCompleted = isCompleted,
        bestScore = bestScore,
        attempts = attempts,
        accentColor = NeonCyan,
        onPlay = onPlay
    )
}

@Composable
private fun WeeklyContent(
    challenge: ChallengeDefinition,
    timeRemaining: Long,
    isCompleted: Boolean,
    bestScore: Int,
    attempts: Int,
    onPlay: () -> Unit
) {
    ChallengeCard(
        challenge = challenge,
        timeRemaining = timeRemaining,
        isCompleted = isCompleted,
        bestScore = bestScore,
        attempts = attempts,
        accentColor = NeonPurple,
        onPlay = onPlay
    )
}

@Composable
private fun ChallengeCard(
    challenge: ChallengeDefinition,
    timeRemaining: Long,
    isCompleted: Boolean,
    bestScore: Int,
    attempts: Int,
    accentColor: Color,
    onPlay: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NeonDarkPanel,
                        NeonDarkPanel.copy(alpha = 0.8f)
                    )
                )
            )
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        // Header with name and timer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = challenge.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            TimerDisplay(timeRemaining, accentColor)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Map info
        Text(
            text = "Map: ${ChallengeDefinitions.getMapDisplayName(challenge.mapId)} | ${challenge.totalWaves} Waves",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Description
        Text(
            text = challenge.description,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Modifiers
        if (challenge.modifiers.isNotEmpty()) {
            Text(
                text = "MODIFIERS:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = NeonYellow
            )
            Spacer(modifier = Modifier.height(4.dp))
            challenge.modifiers.forEach { modifier ->
                val description = ChallengeModifiers.getModifierDescription(modifier)
                val isNegative = ChallengeModifiers.isNegativeModifier(modifier.type)
                Text(
                    text = "• $description",
                    fontSize = 12.sp,
                    color = if (isNegative) NeonRed.copy(alpha = 0.9f) else NeonGreen.copy(alpha = 0.9f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Best Score",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    text = if (bestScore > 0) bestScore.toString() else "-",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Attempts",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    text = attempts.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Play button
        Button(
            onClick = onPlay,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCompleted) NeonGreen.copy(alpha = 0.2f) else accentColor.copy(alpha = 0.2f),
                contentColor = if (isCompleted) NeonGreen else accentColor
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = if (isCompleted) "COMPLETED - PLAY AGAIN" else "START CHALLENGE",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TimerDisplay(timeRemaining: Long, accentColor: Color) {
    val hours = (timeRemaining / (1000 * 60 * 60)) % 24
    val minutes = (timeRemaining / (1000 * 60)) % 60
    val seconds = (timeRemaining / 1000) % 60

    val timerText = String.format("%02d:%02d:%02d", hours, minutes, seconds)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = timerText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

@Composable
private fun EndlessContent(
    highScores: List<EndlessHighScore>,
    onPlayMap: (MapId) -> Unit
) {
    var selectedMapId by remember { mutableStateOf(MapId.SERPENTINE) }
    val availableMaps = remember { ChallengeDefinitions.getEndlessMaps() }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Map selection header
        item {
            Text(
                text = "SELECT MAP",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NeonMagenta
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Map grid (showing 5 maps at a time in a scrollable list)
        items(availableMaps.chunked(3)) { mapRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mapRow.forEach { mapId ->
                    val isSelected = selectedMapId == mapId
                    val bestForMap = highScores.find { it.mapId == mapId.ordinal }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) NeonMagenta.copy(alpha = 0.2f)
                                else NeonDarkPanel
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) NeonMagenta else NeonMagenta.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedMapId = mapId }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = mapId.name.replace('_', ' '),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) NeonMagenta else Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            if (bestForMap != null) {
                                Text(
                                    text = "W${bestForMap.wave}",
                                    fontSize = 9.sp,
                                    color = NeonGold.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
                // Fill remaining space if row is incomplete
                repeat(3 - mapRow.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Selected map info and play button
        item {
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeonDarkPanel)
                    .border(
                        width = 1.dp,
                        color = NeonMagenta.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "ENDLESS MODE",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonMagenta
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Survive as long as possible on ${selectedMapId.name.replace('_', ' ')}. " +
                            "Waves scale infinitely with increasing difficulty.",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onPlayMap(selectedMapId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonMagenta.copy(alpha = 0.2f),
                        contentColor = NeonMagenta
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "START ENDLESS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // High scores section
        if (highScores.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "TOP SCORES",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonGold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(highScores.take(10)) { score ->
                HighScoreRow(score)
            }
        }
    }
}

@Composable
private fun HighScoreRow(score: EndlessHighScore) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(NeonDarkPanel.copy(alpha = 0.7f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = ChallengeDefinitions.getMapDisplayName(score.mapId),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Wave ${score.wave}",
                fontSize = 11.sp,
                color = NeonMagenta.copy(alpha = 0.8f)
            )
        }
        Text(
            text = score.score.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = NeonGold
        )
    }
}

@Composable
private fun BossRushContent(
    challengeData: ChallengeData,
    onPlayBossRush: (MapId, Int) -> Unit
) {
    var selectedMapId by remember { mutableStateOf(MapId.FORTRESS) }
    var selectedTierIndex by remember { mutableStateOf(0) }
    val availableMaps = remember { ChallengeDefinitions.getBossRushMaps() }
    val tiers = remember { ChallengeDefinitions.getBossRushTiers() }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Title and description
        item {
            Text(
                text = "BOSS RUSH",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = NeonRed
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Face waves of bosses and mini-bosses. No regular enemies - just the big ones!",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Tier selection
        item {
            Text(
                text = "SELECT DIFFICULTY",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NeonOrange
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(tiers) { (name, description, index) ->
            val isSelected = selectedTierIndex == index
            val tierColor = when (index) {
                0 -> NeonGreen
                1 -> NeonOrange
                else -> NeonRed
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) tierColor.copy(alpha = 0.2f)
                        else NeonDarkPanel
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) tierColor else tierColor.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { selectedTierIndex = index }
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = tierColor
                    )
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Map selection
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SELECT MAP",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NeonRed
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Map grid
        items(availableMaps.chunked(3)) { mapRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mapRow.forEach { mapId ->
                    val isSelected = selectedMapId == mapId
                    val challengeId = "boss_rush_${mapId.name.lowercase()}_$selectedTierIndex"
                    val bestScore = challengeData.attempts[challengeId]?.bestScore ?: 0

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isSelected) NeonRed.copy(alpha = 0.2f)
                                else NeonDarkPanel
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) NeonRed else NeonRed.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedMapId = mapId }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = mapId.name.replace('_', ' '),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) NeonRed else Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            if (bestScore > 0) {
                                Text(
                                    text = bestScore.toString(),
                                    fontSize = 9.sp,
                                    color = NeonGold.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
                // Fill remaining space if row is incomplete
                repeat(3 - mapRow.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Play button
        item {
            Spacer(modifier = Modifier.height(16.dp))

            val tierName = tiers.getOrNull(selectedTierIndex)?.first ?: "Apprentice"
            val challengeId = "boss_rush_${selectedMapId.name.lowercase()}_$selectedTierIndex"
            val isCompleted = challengeData.attempts[challengeId]?.isCompleted == true

            Button(
                onClick = { onPlayBossRush(selectedMapId, selectedTierIndex) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCompleted) NeonGreen.copy(alpha = 0.2f) else NeonRed.copy(alpha = 0.2f),
                    contentColor = if (isCompleted) NeonGreen else NeonRed
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isCompleted) "COMPLETED - PLAY AGAIN" else "START BOSS RUSH: $tierName",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
