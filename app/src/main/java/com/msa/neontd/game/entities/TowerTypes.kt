package com.msa.neontd.game.entities

import com.msa.neontd.engine.graphics.ShapeType
import com.msa.neontd.util.Color

enum class TowerType(
    val displayName: String,
    val description: String,
    val baseCost: Int,
    val baseColor: Color,
    val shape: ShapeType
) {
    PULSE(
        "Pulse Tower",
        "Basic tower that fires energy pulses",
        100,
        Color.NEON_CYAN,
        ShapeType.CIRCLE
    ),
    SNIPER(
        "Sniper Tower",
        "High damage, slow fire rate, long range",
        200,
        Color.NEON_BLUE,
        ShapeType.DIAMOND
    ),
    SPLASH(
        "Splash Tower",
        "Explosive shots deal area damage",
        250,
        Color.NEON_ORANGE,
        ShapeType.HEXAGON
    ),
    FLAME(
        "Flame Tower",
        "Continuous fire damage in a cone",
        200,
        Color(1f, 0.3f, 0f, 1f),
        ShapeType.TRIANGLE
    ),
    SLOW(
        "Slow Tower",
        "Slows enemies in range",
        150,
        Color(0.5f, 0.8f, 1f, 1f),
        ShapeType.STAR_6
    ),
    TESLA(
        "Tesla Tower",
        "Chain lightning jumps between enemies",
        300,
        Color.NEON_PURPLE,
        ShapeType.OCTAGON
    ),
    GRAVITY(
        "Gravity Tower",
        "Pulls enemies toward center",
        275,
        Color(0.4f, 0f, 0.6f, 1f),
        ShapeType.RING
    ),
    TEMPORAL(
        "Temporal Tower",
        "Creates time freeze zones",
        350,
        Color(0.8f, 0.8f, 1f, 1f),
        ShapeType.HOURGLASS
    ),
    LASER(
        "Laser Tower",
        "Continuous beam deals sustained damage",
        275,
        Color.NEON_GREEN,
        ShapeType.TOWER_LASER
    ),
    POISON(
        "Poison Tower",
        "Applies stacking damage over time",
        175,
        Color(0.2f, 0.8f, 0.2f, 1f),
        ShapeType.HEXAGON
    ),
    MISSILE(
        "Missile Tower",
        "Fires homing missiles",
        300,
        Color(0.8f, 0.2f, 0.2f, 1f),
        ShapeType.ARROW
    ),
    BUFF(
        "Support Tower",
        "Boosts nearby towers",
        200,
        Color.NEON_YELLOW,
        ShapeType.CROSS
    ),
    DEBUFF(
        "Weakening Tower",
        "Reduces enemy defenses",
        225,
        Color.NEON_PINK,
        ShapeType.TRIANGLE_DOWN
    ),
    CHAIN(
        "Chain Tower",
        "Projectiles bounce between enemies",
        250,
        Color(0.6f, 0.4f, 1f, 1f),
        ShapeType.LIGHTNING_BOLT
    )
}

enum class TargetingMode {
    FIRST,      // Target enemy closest to exit
    LAST,       // Target enemy furthest from exit
    STRONGEST,  // Target enemy with most health
    WEAKEST,    // Target enemy with least health
    CLOSEST,    // Target closest enemy to tower
    RANDOM      // Random target
}

enum class DamageType {
    PHYSICAL,
    FIRE,
    ICE,
    LIGHTNING,
    POISON,
    ENERGY,
    TRUE        // Ignores armor
}
