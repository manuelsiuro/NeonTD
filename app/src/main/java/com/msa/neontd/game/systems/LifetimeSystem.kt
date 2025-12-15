package com.msa.neontd.game.systems

import com.msa.neontd.engine.ecs.System
import com.msa.neontd.engine.ecs.World
import com.msa.neontd.game.components.LifetimeComponent
import com.msa.neontd.game.components.SpriteComponent

/**
 * System that handles entity lifetime expiration.
 * Entities with LifetimeComponent are destroyed when their time runs out.
 * Also handles fade-out effect for visual smoothness.
 */
class LifetimeSystem : System(priority = 100) {  // Run late in the frame

    override fun update(world: World, deltaTime: Float) {
        world.forEach<LifetimeComponent> { entity, lifetime ->
            // Decrease remaining time
            lifetime.remainingTime -= deltaTime

            // Apply fade-out effect to sprites
            val sprite = world.getComponent<SpriteComponent>(entity)
            if (sprite != null && lifetime.remainingTime < 0.3f) {
                // Fade out during the last 0.3 seconds
                val fadeProgress = (lifetime.remainingTime / 0.3f).coerceIn(0f, 1f)
                sprite.color.a *= fadeProgress
                sprite.glow *= fadeProgress
            }

            // Check if expired
            if (lifetime.isExpired) {
                // Call expiration callback if set
                lifetime.onExpire?.invoke()

                // Destroy the entity
                world.destroyEntity(entity)
            }
        }
    }
}
