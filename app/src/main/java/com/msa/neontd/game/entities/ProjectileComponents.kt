package com.msa.neontd.game.entities

import com.msa.neontd.engine.ecs.Component
import com.msa.neontd.engine.ecs.Entity
import com.msa.neontd.util.Color
import com.msa.neontd.util.Vector2

data class ProjectileComponent(
    val source: Entity,
    val towerType: TowerType,         // For VFX coloring
    var target: Entity? = null,       // For homing projectiles
    var damage: Float,
    var damageType: DamageType,
    var speed: Float,
    var direction: Vector2,
    var maxDistance: Float = 500f,
    var distanceTraveled: Float = 0f,

    // Area damage
    var splashRadius: Float = 0f,

    // Piercing
    var piercing: Int = 1,
    var hitEntities: MutableSet<Int> = mutableSetOf(),

    // Chain lightning
    var chainCount: Int = 0,
    var chainRange: Float = 0f,
    var chainedEntities: MutableSet<Int> = mutableSetOf(),

    // Status effects to apply
    var slowPercent: Float = 0f,
    var slowDuration: Float = 0f,
    var dotDamage: Float = 0f,
    var dotDuration: Float = 0f,

    // Visual
    var trailColor: Color = Color.WHITE
) : Component {
    val isHoming: Boolean get() = target != null
    val hasSplash: Boolean get() = splashRadius > 0f
    val canPierce: Boolean get() = piercing > hitEntities.size
    val canChain: Boolean get() = chainCount > chainedEntities.size
}

enum class ProjectileType {
    BULLET,
    MISSILE,
    BEAM,
    CHAIN_LIGHTNING,
    EXPLOSION
}
