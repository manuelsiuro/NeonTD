package com.msa.neontd.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.msa.neontd.engine.graphics.ShapeType
import com.msa.neontd.game.data.Encyclopedia
import com.msa.neontd.game.data.EnemyInfo
import com.msa.neontd.game.data.TowerInfo
import kotlin.math.cos
import kotlin.math.sin

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

@Composable
fun EncyclopediaScreen(onBackClick: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var towerIndex by remember { mutableIntStateOf(0) }
    var enemyIndex by remember { mutableIntStateOf(0) }

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
            // Title with glow
            NeonTitle()

            Spacer(modifier = Modifier.height(12.dp))

            // Tabs
            TabRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Pager content
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        (fadeIn(tween(200)) + slideInHorizontally { it / 4 }) togetherWith
                                (fadeOut(tween(200)) + slideOutHorizontally { -it / 4 })
                    },
                    label = "tabContent"
                ) { tab ->
                    if (tab == 0) {
                        TowerPager(
                            currentIndex = towerIndex,
                            onIndexChange = { towerIndex = it }
                        )
                    } else {
                        EnemyPager(
                            currentIndex = enemyIndex,
                            onIndexChange = { enemyIndex = it }
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
                    text = "◄ BACK TO MENU",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun NeonTitle() {
    val infiniteTransition = rememberInfiniteTransition(label = "title")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "⬡ ENCYCLOPEDIA ⬡",
            color = NeonCyan.copy(alpha = glowAlpha),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TabRow(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TabButton(
            text = "TOWERS",
            count = Encyclopedia.towers.size,
            isSelected = selectedTab == 0,
            color = NeonCyan,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(0) }
        )
        TabButton(
            text = "ENEMIES",
            count = Encyclopedia.enemies.size,
            isSelected = selectedTab == 1,
            color = NeonMagenta,
            modifier = Modifier.weight(1f),
            onClick = { onTabSelected(1) }
        )
    }
}

@Composable
private fun TabButton(
    text: String,
    count: Int,
    isSelected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) color.copy(alpha = 0.25f) else Color.Transparent,
        label = "tabBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) color else color.copy(alpha = 0.3f),
        label = "tabBorder"
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                color = if (isSelected) color else color.copy(alpha = 0.5f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .background(color.copy(alpha = if (isSelected) 0.3f else 0.15f), CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$count",
                    color = color.copy(alpha = if (isSelected) 1f else 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TowerPager(currentIndex: Int, onIndexChange: (Int) -> Unit) {
    val towers = Encyclopedia.towers
    val tower = towers[currentIndex]

    Column(modifier = Modifier.fillMaxSize()) {
        // Navigation header with arrows
        PagerNavigation(
            currentIndex = currentIndex,
            total = towers.size,
            itemName = tower.type.displayName,
            accentColor = NeonCyan,
            onPrevious = { if (currentIndex > 0) onIndexChange(currentIndex - 1) },
            onNext = { if (currentIndex < towers.size - 1) onIndexChange(currentIndex + 1) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Tower detail card
        TowerDetailCard(tower = tower)

        Spacer(modifier = Modifier.height(12.dp))

        // Page indicators
        PageIndicators(
            currentIndex = currentIndex,
            total = towers.size,
            color = NeonCyan,
            onIndexSelected = onIndexChange
        )
    }
}

@Composable
private fun EnemyPager(currentIndex: Int, onIndexChange: (Int) -> Unit) {
    val enemies = Encyclopedia.enemies
    val enemy = enemies[currentIndex]

    Column(modifier = Modifier.fillMaxSize()) {
        // Navigation header with arrows
        PagerNavigation(
            currentIndex = currentIndex,
            total = enemies.size,
            itemName = enemy.type.displayName,
            accentColor = NeonMagenta,
            onPrevious = { if (currentIndex > 0) onIndexChange(currentIndex - 1) },
            onNext = { if (currentIndex < enemies.size - 1) onIndexChange(currentIndex + 1) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Enemy detail card
        EnemyDetailCard(enemy = enemy)

        Spacer(modifier = Modifier.height(12.dp))

        // Page indicators
        PageIndicators(
            currentIndex = currentIndex,
            total = enemies.size,
            color = NeonMagenta,
            onIndexSelected = onIndexChange
        )
    }
}

@Composable
private fun PagerNavigation(
    currentIndex: Int,
    total: Int,
    itemName: String,
    accentColor: Color,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous button
        NavArrowButton(
            text = "◄",
            enabled = currentIndex > 0,
            color = accentColor,
            onClick = onPrevious
        )

        // Title and counter
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = itemName,
                color = accentColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${currentIndex + 1} / $total",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }

        // Next button
        NavArrowButton(
            text = "►",
            enabled = currentIndex < total - 1,
            color = accentColor,
            onClick = onNext
        )
    }
}

@Composable
private fun NavArrowButton(
    text: String,
    enabled: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (enabled) color.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = 2.dp,
                color = if (enabled) color.copy(alpha = 0.6f) else color.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) color else color.copy(alpha = 0.2f),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ColumnScope.TowerDetailCard(tower: TowerInfo) {
    val towerColor = tower.type.baseColor.let { Color(it.r, it.g, it.b, 1f) }
    val scrollState = rememberScrollState()

    val infiniteTransition = rememberInfiniteTransition(label = "iconPulse")
    val iconGlow by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NeonDarkPanel,
                        NeonDarkPanel.copy(alpha = 0.95f),
                        NeonBackground
                    )
                )
            )
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(NeonCyan.copy(alpha = 0.6f), NeonCyan.copy(alpha = 0.2f))
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(towerColor.copy(alpha = 0.1f))
                    .border(3.dp, towerColor.copy(alpha = iconGlow * 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                ShapeIcon(
                    shape = tower.type.shape,
                    color = towerColor.copy(alpha = iconGlow),
                    size = 60f
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = tower.type.description,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Stats grid
            StatsGrid {
                StatBox("COST", "${tower.type.baseCost}g", NeonYellow)
                StatBox("DAMAGE", "${tower.damage.toInt()}", NeonOrange)
                StatBox("RANGE", "${tower.range.toInt()}", NeonBlue)
                StatBox("FIRE RATE", "${tower.fireRate}/s", NeonGreen)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Special ability section
            tower.specialAbility?.let { ability ->
                InfoSection(
                    title = "⚡ SPECIAL ABILITY",
                    titleColor = NeonMagenta,
                    content = ability
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Tips section
            InfoSection(
                title = "💡 TACTICAL TIP",
                titleColor = NeonGreen,
                content = tower.tips
            )
        }
    }
}

@Composable
private fun ColumnScope.EnemyDetailCard(enemy: EnemyInfo) {
    val enemyColor = enemy.type.baseColor.let { Color(it.r, it.g, it.b, 1f) }
    val scrollState = rememberScrollState()

    val infiniteTransition = rememberInfiniteTransition(label = "iconPulse")
    val iconGlow by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        NeonDarkPanel,
                        NeonDarkPanel.copy(alpha = 0.95f),
                        NeonBackground
                    )
                )
            )
            .border(
                width = 2.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(NeonMagenta.copy(alpha = 0.6f), NeonMagenta.copy(alpha = 0.2f))
                ),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Large icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(enemyColor.copy(alpha = 0.1f))
                    .border(3.dp, enemyColor.copy(alpha = iconGlow * 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                ShapeIcon(
                    shape = enemy.type.shape,
                    color = enemyColor.copy(alpha = iconGlow),
                    size = 60f * enemy.type.sizeScale.coerceIn(0.8f, 1.2f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Type label
            Text(
                text = enemy.type.name.replace("_", " "),
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Stats grid
            StatsGrid {
                StatBox("HEALTH", "${enemy.type.baseHealth.toInt()}", NeonGreen)
                StatBox("SPEED", "${enemy.type.baseSpeed.toInt()}", NeonCyan)
                StatBox("ARMOR", "${enemy.type.baseArmor.toInt()}", NeonBlue)
                StatBox("GOLD", "${enemy.type.goldReward}g", NeonYellow)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Special ability
            enemy.specialAbility?.let { ability ->
                InfoSection(
                    title = "⚡ ABILITY",
                    titleColor = NeonPurple,
                    content = ability
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Weaknesses
            if (enemy.weaknesses.isNotEmpty()) {
                InfoSection(
                    title = "✓ WEAK TO",
                    titleColor = NeonGreen,
                    content = enemy.weaknesses.joinToString(" • ")
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Resistances
            if (enemy.resistances.isNotEmpty()) {
                InfoSection(
                    title = "✗ RESISTS",
                    titleColor = NeonRed,
                    content = enemy.resistances.joinToString(" • ")
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Tips
            InfoSection(
                title = "💡 COUNTER STRATEGY",
                titleColor = NeonCyan,
                content = enemy.tips
            )
        }
    }
}

@Composable
private fun StatsGrid(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        content()
    }
}

@Composable
private fun StatBox(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = value,
            color = color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = color.copy(alpha = 0.7f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun InfoSection(title: String, titleColor: Color, content: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(titleColor.copy(alpha = 0.08f))
            .border(1.dp, titleColor.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(
            text = title,
            color = titleColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = content,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun PageIndicators(
    currentIndex: Int,
    total: Int,
    color: Color,
    onIndexSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until total) {
            val isSelected = i == currentIndex
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (isSelected) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) color
                        else color.copy(alpha = 0.25f)
                    )
                    .then(
                        if (isSelected) Modifier.border(1.dp, color.copy(alpha = 0.8f), CircleShape)
                        else Modifier
                    )
                    .clickable { onIndexSelected(i) }
            )
        }
    }
}

@Composable
private fun ShapeIcon(shape: ShapeType, color: Color, size: Float) {
    Canvas(modifier = Modifier.size(size.dp)) {
        val center = Offset(size / 2, size / 2)
        val radius = size / 2 * 0.85f
        val strokeWidth = size * 0.06f

        when (shape) {
            ShapeType.CIRCLE, ShapeType.TOWER_PULSE -> {
                drawCircle(color, radius, center, style = Stroke(strokeWidth))
            }
            ShapeType.DIAMOND, ShapeType.TOWER_SNIPER -> {
                drawPolygon(center, radius, 4, strokeWidth, color, startAngle = 45f)
            }
            ShapeType.HEXAGON, ShapeType.TOWER_SPLASH -> {
                drawPolygon(center, radius, 6, strokeWidth, color)
            }
            ShapeType.OCTAGON, ShapeType.TOWER_TESLA -> {
                drawPolygon(center, radius, 8, strokeWidth, color)
            }
            ShapeType.TRIANGLE -> {
                drawPolygon(center, radius, 3, strokeWidth, color, startAngle = -90f)
            }
            ShapeType.TRIANGLE_DOWN -> {
                drawPolygon(center, radius, 3, strokeWidth, color, startAngle = 90f)
            }
            ShapeType.STAR, ShapeType.STAR_6 -> {
                drawStar(center, radius, 6, 0.5f, strokeWidth, color)
            }
            ShapeType.CROSS, ShapeType.TOWER_SUPPORT -> {
                drawCross(center, radius, strokeWidth, color)
            }
            ShapeType.TOWER_LASER -> {
                drawPolygon(center, radius * 1.2f, 4, strokeWidth, color, yScale = 0.5f)
            }
            ShapeType.LIGHTNING_BOLT -> {
                drawLightning(center, radius, strokeWidth, color)
            }
            ShapeType.RING -> {
                drawCircle(color, radius, center, style = Stroke(strokeWidth))
                drawCircle(color.copy(alpha = 0.6f), radius * 0.6f, center, style = Stroke(strokeWidth * 0.8f))
            }
            ShapeType.ARROW -> {
                drawArrow(center, radius, strokeWidth, color)
            }
            ShapeType.HOURGLASS -> {
                drawHourglass(center, radius, strokeWidth, color)
            }
            ShapeType.PENTAGON -> {
                drawPolygon(center, radius, 5, strokeWidth, color, startAngle = -90f)
            }
            ShapeType.HEART -> {
                drawHeart(center, radius, strokeWidth, color)
            }
            ShapeType.RECTANGLE -> {
                val hw = radius * 0.9f
                val hh = radius * 0.7f
                drawRect(
                    color,
                    topLeft = Offset(center.x - hw, center.y - hh),
                    size = androidx.compose.ui.geometry.Size(hw * 2, hh * 2),
                    style = Stroke(strokeWidth)
                )
            }
            else -> {
                drawCircle(color, radius, center, style = Stroke(strokeWidth))
            }
        }
    }
}

private fun DrawScope.drawPolygon(
    center: Offset,
    radius: Float,
    sides: Int,
    strokeWidth: Float,
    color: Color,
    startAngle: Float = 0f,
    yScale: Float = 1f
) {
    val path = Path()
    for (i in 0 until sides) {
        val angle = Math.toRadians((startAngle + i * 360f / sides).toDouble())
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat() * yScale
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color, style = Stroke(strokeWidth))
}

private fun DrawScope.drawStar(
    center: Offset,
    radius: Float,
    points: Int,
    innerRatio: Float,
    strokeWidth: Float,
    color: Color
) {
    val path = Path()
    val totalPoints = points * 2
    for (i in 0 until totalPoints) {
        val r = if (i % 2 == 0) radius else radius * innerRatio
        val angle = Math.toRadians((-90 + i * 180f / points).toDouble())
        val x = center.x + r * cos(angle).toFloat()
        val y = center.y + r * sin(angle).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color, style = Stroke(strokeWidth))
}

private fun DrawScope.drawCross(center: Offset, radius: Float, strokeWidth: Float, color: Color) {
    drawLine(color, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), strokeWidth)
    drawLine(color, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), strokeWidth)
}

private fun DrawScope.drawLightning(center: Offset, radius: Float, strokeWidth: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x + radius * 0.2f, center.y - radius)
        lineTo(center.x - radius * 0.3f, center.y - radius * 0.1f)
        lineTo(center.x + radius * 0.1f, center.y + radius * 0.1f)
        lineTo(center.x - radius * 0.2f, center.y + radius)
    }
    drawPath(path, color, style = Stroke(strokeWidth))
}

private fun DrawScope.drawArrow(center: Offset, radius: Float, strokeWidth: Float, color: Color) {
    drawLine(color, Offset(center.x, center.y + radius * 0.8f), Offset(center.x, center.y - radius * 0.4f), strokeWidth * 1.2f)
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x - radius * 0.5f, center.y - radius * 0.3f)
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius * 0.5f, center.y - radius * 0.3f)
    }
    drawPath(path, color, style = Stroke(strokeWidth))
}

private fun DrawScope.drawHourglass(center: Offset, radius: Float, strokeWidth: Float, color: Color) {
    val hw = radius * 0.6f
    val path = Path().apply {
        moveTo(center.x - hw, center.y - radius)
        lineTo(center.x + hw, center.y - radius)
        lineTo(center.x, center.y)
        lineTo(center.x + hw, center.y + radius)
        lineTo(center.x - hw, center.y + radius)
        lineTo(center.x, center.y)
        close()
    }
    drawPath(path, color, style = Stroke(strokeWidth))
}

private fun DrawScope.drawHeart(center: Offset, radius: Float, strokeWidth: Float, color: Color) {
    val r = radius * 0.4f
    drawCircle(color, r, Offset(center.x - r, center.y - r * 0.3f), style = Stroke(strokeWidth))
    drawCircle(color, r, Offset(center.x + r, center.y - r * 0.3f), style = Stroke(strokeWidth))
    val path = Path().apply {
        moveTo(center.x - radius * 0.75f, center.y)
        lineTo(center.x, center.y + radius)
        lineTo(center.x + radius * 0.75f, center.y)
    }
    drawPath(path, color, style = Stroke(strokeWidth))
}
