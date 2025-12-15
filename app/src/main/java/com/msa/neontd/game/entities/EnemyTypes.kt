package com.msa.neontd.game.entities

import com.msa.neontd.engine.graphics.ShapeType
import com.msa.neontd.util.Color

enum class EnemyType(
    val displayName: String,
    val baseHealth: Float,
    val baseSpeed: Float,
    val baseArmor: Float,
    val goldReward: Int,
    val baseColor: Color,
    val shape: ShapeType,
    val sizeScale: Float = 1f
) {
    BASIC(
        "Drone",
        100f, 60f, 0f, 10,
        Color.NEON_CYAN,
        ShapeType.CIRCLE,
        1.0f
    ),
    FAST(
        "Runner",
        50f, 120f, 0f, 15,
        Color.NEON_GREEN,
        ShapeType.DIAMOND,
        0.7f
    ),
    TANK(
        "Heavy",
        400f, 30f, 30f, 25,
        Color(0.6f, 0.6f, 0.7f, 1f),
        ShapeType.HEXAGON,
        1.5f
    ),
    FLYING(
        "Flyer",
        80f, 80f, 0f, 20,
        Color(0.8f, 0.8f, 1f, 1f),
        ShapeType.TRIANGLE,
        0.8f
    ),
    HEALER(
        "Medic",
        120f, 50f, 5f, 30,
        Color(0.2f, 1f, 0.5f, 1f),
        ShapeType.CROSS,
        1.0f
    ),
    SHIELDED(
        "Guardian",
        150f, 45f, 10f, 35,
        Color(0.3f, 0.6f, 1f, 1f),
        ShapeType.OCTAGON,
        1.2f
    ),
    SPAWNER(
        "Hive",
        200f, 35f, 15f, 40,
        Color(0.8f, 0.4f, 0.8f, 1f),
        ShapeType.HEXAGON,
        1.3f
    ),
    PHASING(
        "Phantom",
        100f, 55f, 0f, 35,
        Color(0.5f, 0.2f, 0.8f, 1f),
        ShapeType.DIAMOND,
        0.9f
    ),
    SPLITTING(
        "Splitter",
        150f, 50f, 5f, 20,
        Color(1f, 0.5f, 0.2f, 1f),
        ShapeType.TRIANGLE,
        1.0f
    ),
    REGENERATING(
        "Regen",
        180f, 45f, 5f, 30,
        Color(0.3f, 0.9f, 0.3f, 1f),
        ShapeType.HEART,
        1.0f
    ),
    STEALTH(
        "Shadow",
        70f, 70f, 0f, 25,
        Color(0.2f, 0.2f, 0.3f, 1f),
        ShapeType.DIAMOND,
        0.8f
    ),
    FIRE_ELEMENTAL(
        "Inferno",
        200f, 50f, 10f, 40,
        Color(1f, 0.3f, 0f, 1f),
        ShapeType.PENTAGON,
        1.1f
    ),
    ICE_ELEMENTAL(
        "Frost",
        200f, 50f, 10f, 40,
        Color(0.5f, 0.8f, 1f, 1f),
        ShapeType.STAR_6,
        1.1f
    ),
    LIGHTNING_ELEMENTAL(
        "Storm",
        180f, 65f, 5f, 40,
        Color(1f, 1f, 0.3f, 1f),
        ShapeType.LIGHTNING_BOLT,
        1.1f
    ),
    MINI_BOSS(
        "Elite",
        800f, 40f, 40f, 100,
        Color.NEON_ORANGE,
        ShapeType.PENTAGON,
        1.8f
    ),
    BOSS(
        "Overlord",
        3000f, 25f, 60f, 500,
        Color.NEON_MAGENTA,
        ShapeType.STAR,
        2.5f
    )
}

// Resistances for elemental enemies
data class Resistances(
    var physical: Float = 0f,
    var fire: Float = 0f,
    var ice: Float = 0f,
    var lightning: Float = 0f,
    var poison: Float = 0f,
    var energy: Float = 0f
) {
    fun getResistance(damageType: DamageType): Float {
        return when (damageType) {
            DamageType.PHYSICAL -> physical
            DamageType.FIRE -> fire
            DamageType.ICE -> ice
            DamageType.LIGHTNING -> lightning
            DamageType.POISON -> poison
            DamageType.ENERGY -> energy
            DamageType.TRUE -> 0f  // True damage ignores resistances
        }
    }

    companion object {
        fun forEnemyType(type: EnemyType): Resistances {
            return when (type) {
                EnemyType.FIRE_ELEMENTAL -> Resistances(fire = 1f, ice = -0.5f)  // Immune to fire, weak to ice
                EnemyType.ICE_ELEMENTAL -> Resistances(ice = 1f, fire = -0.5f)   // Immune to ice, weak to fire
                EnemyType.LIGHTNING_ELEMENTAL -> Resistances(lightning = 1f, physical = -0.25f)
                EnemyType.TANK -> Resistances(physical = 0.3f)
                EnemyType.BOSS -> Resistances(physical = 0.2f, fire = 0.2f, ice = 0.2f, lightning = 0.2f)
                else -> Resistances()
            }
        }
    }
}
