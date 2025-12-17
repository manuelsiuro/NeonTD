package com.msa.neontd.engine.vfx

import com.msa.neontd.engine.audio.AudioEventHandler
import com.msa.neontd.engine.graphics.Camera
import com.msa.neontd.engine.graphics.SpriteBatch
import com.msa.neontd.engine.resources.Texture
import com.msa.neontd.game.entities.DamageType
import com.msa.neontd.game.entities.TowerType
import com.msa.neontd.util.Color
import com.msa.neontd.util.Vector2

/**
 * Centralized VFX management for the game
 */
class VFXManager {

    private val particleSystem = ParticleSystem(1500)
    private var camera: Camera? = null

    // Charging effects tracking (for future charging tower implementation)
    private val chargingTowers = mutableMapOf<Int, Float>()

    fun setCamera(cam: Camera) {
        camera = cam
    }

    fun update(deltaTime: Float) {
        particleSystem.update(deltaTime)
    }

    fun render(spriteBatch: SpriteBatch, whiteTexture: Texture) {
        particleSystem.render(spriteBatch, whiteTexture)
    }

    fun clear() {
        particleSystem.clear()
    }

    // ===== Projectile Effects =====

    fun onProjectileSpawn(entityId: Int, position: Vector2, towerType: TowerType) {
        val color = towerType.baseColor
        emit(position, ParticleSystem.IMPACT_SMALL, color)
    }

    fun onProjectileTrail(entityId: Int, position: Vector2, towerType: TowerType) {
        val color = towerType.baseColor.copy()
        color.a = 0.7f
        // Use brighter trail for certain tower types
        val config = when (towerType) {
            TowerType.SNIPER, TowerType.LASER, TowerType.MISSILE -> ParticleSystem.TRAIL_BRIGHT
            else -> ParticleSystem.TRAIL
        }
        emit(position, config, color)
    }

    fun onProjectileImpact(position: Vector2, damageType: DamageType, towerType: TowerType?) {
        val config = when (damageType) {
            DamageType.FIRE -> ParticleSystem.FIRE
            DamageType.ICE -> ParticleSystem.FREEZE
            DamageType.LIGHTNING -> ParticleSystem.ELECTRIC
            DamageType.POISON -> ParticleSystem.POISON
            else -> ParticleSystem.IMPACT_SMALL
        }
        val color = towerType?.baseColor ?: getColorForDamageType(damageType)
        emit(position, config, color)

        // Audio: Impact sound
        AudioEventHandler.onProjectileImpact(position, damageType, towerType)
    }

    fun onExplosion(position: Vector2, radius: Float, damageType: DamageType) {
        val color = getColorForDamageType(damageType)

        // Use large explosion for bigger radius
        val mainConfig = if (radius > 60f) ParticleSystem.EXPLOSION_LARGE else ParticleSystem.EXPLOSION
        emit(position, mainConfig, color)

        // Ring of particles at the edge
        val ringCount = (radius / 5f).toInt().coerceIn(8, 24)
        particleSystem.emitCircle(position, radius * 0.8f, ParticleSystem.IMPACT_SMALL.copy(count = ringCount), color)

        // Screen shake based on explosion size
        val shakeIntensity = (radius / 40f).coerceIn(3f, 12f)
        camera?.shake(shakeIntensity, 0.2f)

        // Audio: Explosion sound
        AudioEventHandler.onExplosion(position, radius, damageType)
    }

    fun onChainLightning(start: Vector2, end: Vector2) {
        particleSystem.emitLine(start, end, ParticleSystem.ELECTRIC, Color.NEON_CYAN.copy())

        // Audio: Chain lightning sound
        AudioEventHandler.onChainLightning(start, end)
    }

    fun onBeamHit(position: Vector2, towerType: TowerType) {
        val config = ParticleSystem.IMPACT_SMALL.copy(count = 3)
        emit(position, config, towerType.baseColor)

        // Audio: Beam hit sound
        AudioEventHandler.onBeamHit(position, towerType)
    }

    // ===== Tower Firing Effects =====

    fun onTowerFire(position: Vector2, direction: Vector2, towerType: TowerType) {
        // Muzzle flash at tower position
        val flashConfig = ParticleSystem.MUZZLE_FLASH.copy(
            angle = getAngleRange(direction, 40f)
        )
        emit(position, flashConfig, towerType.baseColor)

        // Audio: Tower fire sound
        AudioEventHandler.onTowerFire(position, direction, towerType)
    }

    // Todo: No charging towers, should be added later
    fun onTowerCharging(entityId: Int, position: Vector2, towerType: TowerType, progress: Float) {
        // Emit charge-up particles converging on tower
        if (progress > 0.2f) {
            chargingTowers[entityId] = progress
            val config = ParticleSystem.CHARGE_UP.copy(
                spread = 50f * (1f - progress) + 20f
            )
            emit(position, config, towerType.baseColor)

            // Audio: Charging sound (rate limited in handler)
            AudioEventHandler.onTowerCharging(entityId, position, towerType, progress)
        }
    }

    // Todo: No charging towers, should be added later
    fun onTowerChargeComplete(entityId: Int, position: Vector2, towerType: TowerType) {
        chargingTowers.remove(entityId)
        // Burst effect when charge completes
        val burstConfig = ParticleSystem.IMPACT_SMALL.copy(count = 12, speed = 150f..250f)
        emit(position, burstConfig, towerType.baseColor)

        // Audio: Charge complete sound
        AudioEventHandler.onTowerChargeComplete(entityId, position, towerType)
    }

    private fun getAngleRange(direction: Vector2, spread: Float): ClosedFloatingPointRange<Float> {
        val angle = kotlin.math.atan2(direction.y, direction.x) * 180f / kotlin.math.PI.toFloat()
        return (angle - spread)..(angle + spread)
    }

    // ===== Enemy Effects =====

    fun onEnemySpawn(position: Vector2) {
        emit(position, ParticleSystem.SPAWN)

        // Audio: Enemy spawn sound
        AudioEventHandler.onEnemySpawn(position)
    }

    fun onEnemyDeath(position: Vector2, color: Color, isBoss: Boolean = false) {
        if (isBoss) {
            // Epic boss death effect
            emit(position, ParticleSystem.DEATH_BOSS, color)
            // Multiple rings expanding outward
            particleSystem.emitCircle(position, 30f, ParticleSystem.EXPLOSION.copy(count = 20), color)
            particleSystem.emitCircle(position, 60f, ParticleSystem.EXPLOSION.copy(count = 24), color)
            // Major screen shake
            camera?.shake(15f, 0.4f)
        } else {
            emit(position, ParticleSystem.DEATH, color)
            // Small shake on enemy death
            camera?.shake(1.5f, 0.08f)
        }

        // Audio: Enemy death sound
        AudioEventHandler.onEnemyDeath(position, isBoss)
    }

    fun onMiniBossDeath(position: Vector2, color: Color) {
        // Medium effect between regular and boss
        val config = ParticleSystem.DEATH.copy(count = 40, speed = 150f..300f)
        emit(position, config, color)
        particleSystem.emitCircle(position, 40f, ParticleSystem.EXPLOSION.copy(count = 16), color)
        camera?.shake(8f, 0.25f)

        // Audio: Mini-boss death sound
        AudioEventHandler.onMiniBossDeath(position)
    }

    fun onEnemyHit(position: Vector2, damageType: DamageType) {
        val config = ParticleSystem.IMPACT_SMALL.copy(count = 5)
        emit(position, config, getColorForDamageType(damageType))

        // Audio: Enemy hit sound (rate limited in handler)
        AudioEventHandler.onEnemyHit(position, damageType)
    }

    fun onEnemyReachedEnd(position: Vector2) {
        val config = ParticleSystem.DEATH.copy(
            startColor = Color.NEON_MAGENTA.copy(),
            endColor = Color(0.5f, 0f, 0.5f, 0f),
            count = 30
        )
        emit(position, config)
        // Screen shake when enemy reaches end (damage taken!)
        camera?.shake(5f, 0.15f)

        // Audio: Enemy reached end sound
        AudioEventHandler.onEnemyReachedEnd(position)
    }

    fun onElementalDeath(position: Vector2, damageType: DamageType) {
        // Type-specific death effect for elemental enemies
        val config = when (damageType) {
            DamageType.FIRE -> ParticleSystem.FIRE.copy(count = 25, speed = 100f..200f)
            DamageType.ICE -> ParticleSystem.FREEZE.copy(count = 25, speed = 80f..150f)
            DamageType.LIGHTNING -> ParticleSystem.ELECTRIC.copy(count = 30, speed = 150f..300f)
            DamageType.POISON -> ParticleSystem.POISON.copy(count = 20, speed = 50f..100f)
            else -> ParticleSystem.DEATH
        }
        emit(position, config)
        camera?.shake(3f, 0.1f)

        // Audio: Elemental death sound
        AudioEventHandler.onElementalDeath(position, damageType)
    }

    // ===== Status Effect Visuals =====

    fun onSlowApplied(position: Vector2) {
        emit(position, ParticleSystem.FREEZE.copy(count = 5))

        // Audio: Slow applied sound
        AudioEventHandler.onSlowApplied(position)
    }

    fun onBurnTick(position: Vector2) {
        emit(position, ParticleSystem.FIRE.copy(count = 2))

        // Audio: Burn tick sound (rate limited in handler)
        AudioEventHandler.onBurnTick(position)
    }

    fun onPoisonTick(position: Vector2) {
        emit(position, ParticleSystem.POISON.copy(count = 1))

        // Audio: Poison tick sound (rate limited in handler)
        AudioEventHandler.onPoisonTick(position)
    }

    fun onStunApplied(position: Vector2) {
        val config = ParticleSystem.ELECTRIC.copy(count = 8)
        emit(position, config)

        // Audio: Stun applied sound
        AudioEventHandler.onStunApplied(position)
    }

    // Todo: No healing mechanic in game, should be added later
    fun onHeal(position: Vector2) {
        emit(position, ParticleSystem.HEAL)

        // Audio: Heal sound
        AudioEventHandler.onHeal(position)
    }

    // ===== Tower Effects =====

    fun onTowerPlace(position: Vector2, towerType: TowerType) {
        val config = ParticleSystem.SPAWN.copy(count = 15)
        emit(position, config, towerType.baseColor)

        // Audio: Tower place sound
        AudioEventHandler.onTowerPlace(position, towerType)
    }

    fun onTowerUpgrade(position: Vector2, towerType: TowerType) {
        // Central burst effect
        val burstConfig = ParticleSystem.SPAWN.copy(
            count = 30,
            speed = 100f..200f,
            lifetime = 0.4f..0.6f
        )
        emit(position, burstConfig, towerType.baseColor)

        // Expanding ring of particles
        particleSystem.emitCircle(position, 45f, ParticleSystem.IMPACT_SMALL.copy(count = 18), towerType.baseColor)

        // Secondary gold accent particles (power-up feel)
        val goldAccent = ParticleSystem.TRAIL_BRIGHT.copy(
            startColor = Color.NEON_YELLOW.copy(),
            endColor = towerType.baseColor.copy().also { it.a = 0f },
            count = 8,
            speed = 60f..100f,
            lifetime = 0.3f..0.5f
        )
        emit(position, goldAccent)

        // Small screen feedback
        camera?.shake(2f, 0.1f)

        // Audio: Tower upgrade sound
        AudioEventHandler.onTowerUpgrade(position, towerType)
    }

    fun onTowerSell(position: Vector2, towerType: TowerType? = null) {
        // Gold implosion effect with tower's color fading to gold
        val baseColor = towerType?.baseColor ?: Color.NEON_CYAN
        val config = ParticleSystem.DEATH.copy(
            startColor = baseColor.copy(),
            endColor = Color.NEON_YELLOW.copy().also { it.a = 0f },
            count = 20,
            speed = 40f..100f
        )
        emit(position, config)

        // Gold coin particle burst
        val goldBurst = ParticleSystem.IMPACT_SMALL.copy(
            startColor = Color.NEON_YELLOW.copy(),
            endColor = Color.NEON_YELLOW.copy().also { it.a = 0f },
            count = 12,
            speed = 80f..150f,
            lifetime = 0.3f..0.5f
        )
        emit(position, goldBurst)

        // Audio: Tower sell sound
        AudioEventHandler.onTowerSell(position, towerType)
    }

    // ===== Aura Effects =====

    fun onAuraPulse(center: Vector2, radius: Float, color: Color) {
        particleSystem.emitCircle(center, radius, ParticleSystem.TRAIL.copy(count = 16), color)
    }

    // ===== Wave Effects =====

    fun onWaveStart(spawnPoints: List<Vector2>) {
        for (point in spawnPoints) {
            emit(point, ParticleSystem.SPAWN.copy(count = 40))
            // Add an expanding ring effect
            particleSystem.emitCircle(point, 25f, ParticleSystem.TRAIL.copy(count = 12), Color.NEON_GREEN.copy())
        }

        // Audio: Wave start sound
        AudioEventHandler.onWaveStart(spawnPoints)
    }

    fun onWaveComplete() {
        // Celebration burst effect (call this with all tower positions)

        // Audio: Wave complete sound
        AudioEventHandler.onWaveComplete()
    }

    fun onWaveCelebration(towerPositions: List<Vector2>) {
        // Each tower emits a victory burst
        for (pos in towerPositions) {
            val config = ParticleSystem.SPAWN.copy(count = 15, speed = 60f..100f)
            emit(pos, config, Color.NEON_YELLOW.copy())
        }
    }

    fun onVictory(centerX: Float, centerY: Float) {
        // Epic victory effect
        val center = Vector2(centerX, centerY)
        for (i in 0 until 5) {
            val radius = 50f + i * 40f
            val config = ParticleSystem.EXPLOSION_LARGE.copy(count = 30)
            particleSystem.emitCircle(center, radius, config, Color.NEON_YELLOW.copy())
        }
        camera?.shake(10f, 0.5f)

        // Audio: Victory sound and music
        AudioEventHandler.onVictory(centerX, centerY)
    }

    fun onGameOver(centerX: Float, centerY: Float) {
        // Dramatic game over effect
        val center = Vector2(centerX, centerY)
        emit(center, ParticleSystem.DEATH_BOSS.copy(count = 80), Color.NEON_MAGENTA.copy())
        camera?.shake(20f, 0.6f)

        // Audio: Game over sound and music
        AudioEventHandler.onGameOver(centerX, centerY)
    }

    // ===== Ambient Effects =====

    fun emitAmbientSpawnPoint(position: Vector2) {
        // Subtle particle emission at spawn point
        val config = ParticleSystem.TRAIL.copy(count = 1, lifetime = 0.3f..0.5f)
        emit(position, config, Color.NEON_GREEN.copy().also { it.a = 0.5f })
    }

    fun emitAmbientExitPoint(position: Vector2) {
        // Subtle suction effect at exit
        val config = ParticleSystem.CHARGE_UP.copy(count = 1, spread = 30f)
        emit(position, config, Color.NEON_MAGENTA.copy().also { it.a = 0.4f })
    }

    // Todo: implement later
    fun emitGridPulse(position: Vector2) {
        // Path cell pulse effect
        val config = ParticleSystem.TRAIL.copy(count = 1, size = 3f..4f, lifetime = 0.2f..0.3f)
        emit(position, config, Color.NEON_CYAN.copy().also { it.a = 0.3f })
    }

    // ===== Helper Methods =====

    private fun emit(position: Vector2, config: ParticleConfig, color: Color? = null) {
        particleSystem.emit(position, config, color)
    }

    private fun getColorForDamageType(damageType: DamageType): Color {
        return when (damageType) {
            DamageType.PHYSICAL -> Color.WHITE.copy()
            DamageType.FIRE -> Color.NEON_ORANGE.copy()
            DamageType.ICE -> Color(0.7f, 0.9f, 1f, 1f)
            DamageType.LIGHTNING -> Color.NEON_CYAN.copy()
            DamageType.POISON -> Color.NEON_GREEN.copy()
            DamageType.ENERGY -> Color.NEON_MAGENTA.copy()
            DamageType.TRUE -> Color.WHITE.copy()
        }
    }

}
