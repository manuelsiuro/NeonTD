package com.msa.neontd.game.entities

import com.msa.neontd.config.RenderConfig
import com.msa.neontd.engine.ecs.Entity
import com.msa.neontd.engine.ecs.World
import com.msa.neontd.game.components.LifetimeComponent
import com.msa.neontd.game.components.ModelComponent
import com.msa.neontd.game.components.SpriteComponent
import com.msa.neontd.game.components.TransformComponent
import com.msa.neontd.game.components.VelocityComponent
import com.msa.neontd.util.Color
import com.msa.neontd.util.Vector2

class ProjectileFactory(private val world: World) {

    /**
     * Determine the projectile visual type based on tower type.
     */
    private fun getProjectileType(towerType: TowerType, isHoming: Boolean): ProjectileType {
        return when {
            isHoming -> ProjectileType.MISSILE
            towerType == TowerType.TESLA || towerType == TowerType.CHAIN -> ProjectileType.CHAIN_LIGHTNING
            towerType == TowerType.LASER -> ProjectileType.BEAM
            towerType == TowerType.SPLASH -> ProjectileType.MISSILE
            towerType == TowerType.MISSILE -> ProjectileType.MISSILE
            else -> ProjectileType.BULLET
        }
    }

    /**
     * Get scale for projectile based on type.
     */
    private fun getProjectileScale(type: ProjectileType): Float {
        return when (type) {
            ProjectileType.BULLET -> 6f
            ProjectileType.MISSILE -> 8f
            ProjectileType.BEAM -> 10f
            ProjectileType.CHAIN_LIGHTNING -> 5f
            ProjectileType.EXPLOSION -> 15f
        }
    }

    fun createProjectile(
        position: Vector2,
        direction: Vector2,
        speed: Float,
        damage: Float,
        damageType: DamageType,
        source: Entity,
        towerType: TowerType,
        target: Entity? = null,
        splashRadius: Float = 0f,
        piercing: Int = 1,
        chainCount: Int = 0,
        chainRange: Float = 0f,
        slowPercent: Float = 0f,
        slowDuration: Float = 0f,
        dotDamage: Float = 0f,
        dotDuration: Float = 0f
    ): Entity {
        val entity = world.createEntity()

        // Transform
        world.addComponent(entity, TransformComponent(
            position = position.copy()
        ))

        // Velocity
        world.addComponent(entity, VelocityComponent(
            velocity = Vector2(direction.x * speed, direction.y * speed),
            maxSpeed = speed
        ))

        // Determine color based on damage type
        val color = when (damageType) {
            DamageType.PHYSICAL -> Color.WHITE.copy()
            DamageType.FIRE -> Color(1f, 0.4f, 0f, 1f)
            DamageType.ICE -> Color(0.5f, 0.8f, 1f, 1f)
            DamageType.LIGHTNING -> Color(1f, 1f, 0.3f, 1f)
            DamageType.POISON -> Color(0.3f, 0.9f, 0.3f, 1f)
            DamageType.ENERGY -> Color.NEON_CYAN.copy()
            DamageType.TRUE -> Color.NEON_MAGENTA.copy()
        }

        // Determine projectile type for 3D rendering
        val projectileType = getProjectileType(towerType, target != null)

        // Add 3D model component if 3D projectiles are enabled
        if (RenderConfig.use3DProjectiles) {
            world.addComponent(entity, ModelComponent(
                color = color,
                glow = 0.8f,
                visible = true,
                scale = getProjectileScale(projectileType)
            ))
        }

        // Sprite (for 2D fallback or when 3D is disabled)
        if (!RenderConfig.use3DProjectiles) {
            world.addComponent(entity, SpriteComponent(
                width = 8f,
                height = 8f,
                color = color,
                glow = 0.8f,
                layer = 15
            ))
        }

        // Projectile data
        world.addComponent(entity, ProjectileComponent(
            source = source,
            towerType = towerType,
            target = target,
            damage = damage,
            damageType = damageType,
            speed = speed,
            direction = direction.copy(),
            splashRadius = splashRadius,
            piercing = piercing,
            chainCount = chainCount,
            chainRange = chainRange,
            slowPercent = slowPercent,
            slowDuration = slowDuration,
            dotDamage = dotDamage,
            dotDuration = dotDuration,
            trailColor = color.copy(),
            projectileType = projectileType
        ))

        // Lifetime (auto-destroy after max distance)
        world.addComponent(entity, LifetimeComponent(
            remainingTime = 5f  // Max 5 seconds
        ))

        return entity
    }

    fun createChainLightning(
        startPosition: Vector2,
        targetEntity: Entity,
        damage: Float,
        source: Entity,
        chainCount: Int,
        chainRange: Float,
        alreadyChained: Set<Int>
    ): Entity {
        val entity = world.createEntity()

        val targetTransform = world.getComponent<TransformComponent>(targetEntity)
        val targetPos = targetTransform?.position ?: startPosition

        // Direction to target
        val direction = Vector2(
            targetPos.x - startPosition.x,
            targetPos.y - startPosition.y
        ).normalize()

        world.addComponent(entity, TransformComponent(
            position = startPosition.copy()
        ))

        world.addComponent(entity, VelocityComponent(
            velocity = Vector2(direction.x * 800f, direction.y * 800f),
            maxSpeed = 800f
        ))

        val lightningColor = Color(0.8f, 0.8f, 1f, 1f)

        // Add 3D model component if 3D projectiles are enabled
        if (RenderConfig.use3DProjectiles) {
            world.addComponent(entity, ModelComponent(
                color = lightningColor,
                glow = 1f,
                visible = true,
                scale = getProjectileScale(ProjectileType.CHAIN_LIGHTNING)
            ))
        }

        if (!RenderConfig.use3DProjectiles) {
            world.addComponent(entity, SpriteComponent(
                width = 6f,
                height = 6f,
                color = lightningColor,
                glow = 1f,
                layer = 15
            ))
        }

        val chainedSet = alreadyChained.toMutableSet()
        chainedSet.add(targetEntity.id)

        world.addComponent(entity, ProjectileComponent(
            source = source,
            towerType = TowerType.TESLA,  // Chain lightning is from Tesla/Chain towers
            target = targetEntity,
            damage = damage * 0.8f,  // Reduced damage for chains
            damageType = DamageType.LIGHTNING,
            speed = 800f,
            direction = direction,
            chainCount = chainCount - 1,
            chainRange = chainRange,
            chainedEntities = chainedSet,
            projectileType = ProjectileType.CHAIN_LIGHTNING
        ))

        world.addComponent(entity, LifetimeComponent(remainingTime = 2f))

        return entity
    }

    fun createExplosion(
        position: Vector2,
        radius: Float,
        damage: Float,
        damageType: DamageType,
        source: Entity
    ): Entity {
        val entity = world.createEntity()

        world.addComponent(entity, TransformComponent(
            position = position.copy()
        ))

        val explosionColor = when (damageType) {
            DamageType.FIRE -> Color(1f, 0.5f, 0f, 0.6f)
            else -> Color(1f, 1f, 1f, 0.6f)
        }

        // Add 3D model component if 3D projectiles are enabled
        if (RenderConfig.use3DProjectiles) {
            world.addComponent(entity, ModelComponent(
                color = explosionColor,
                glow = 1f,
                visible = true,
                scale = radius * 2  // Scale sphere to match explosion radius
            ))
        }

        if (!RenderConfig.use3DProjectiles) {
            world.addComponent(entity, SpriteComponent(
                width = radius * 2,
                height = radius * 2,
                color = explosionColor,
                glow = 1f,
                layer = 20
            ))
        }

        // Explosion marker for damage processing
        world.addComponent(entity, ProjectileComponent(
            source = source,
            towerType = TowerType.SPLASH,  // Explosions typically from splash towers
            damage = damage,
            damageType = damageType,
            speed = 0f,
            direction = Vector2.ZERO,
            splashRadius = radius,
            projectileType = ProjectileType.EXPLOSION
        ))

        // Short lifetime for visual effect
        world.addComponent(entity, LifetimeComponent(remainingTime = 0.1f))

        return entity
    }
}
