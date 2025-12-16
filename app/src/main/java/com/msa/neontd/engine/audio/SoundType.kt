package com.msa.neontd.engine.audio

import com.msa.neontd.R
import com.msa.neontd.game.entities.DamageType
import com.msa.neontd.game.entities.TowerType

/**
 * All sound effects in the game.
 * @param resourceId Raw resource ID for the sound file
 * @param priority Higher = more important (1-10), used when SoundPool streams are full
 * @param baseVolume Default volume multiplier (0.0-1.0)
 */
enum class SoundType(
    val resourceId: Int,
    val priority: Int = 5,
    val baseVolume: Float = 1.0f
) {
    // === Tower Firing Sounds (14 unique) ===
    TOWER_PULSE_FIRE(R.raw.tower_pulse_fire, 5),
    TOWER_SNIPER_FIRE(R.raw.tower_sniper_fire, 6),
    TOWER_SPLASH_FIRE(R.raw.tower_splash_fire, 6),
    TOWER_FLAME_FIRE(R.raw.tower_flame_fire, 5, 0.8f),
    TOWER_SLOW_FIRE(R.raw.tower_slow_fire, 4),
    TOWER_TESLA_FIRE(R.raw.tower_tesla_fire, 6),
    TOWER_GRAVITY_FIRE(R.raw.tower_gravity_fire, 5),
    TOWER_TEMPORAL_FIRE(R.raw.tower_temporal_fire, 5),
    TOWER_LASER_FIRE(R.raw.tower_laser_fire, 5, 0.7f),
    TOWER_POISON_FIRE(R.raw.tower_poison_fire, 4),
    TOWER_MISSILE_FIRE(R.raw.tower_missile_fire, 6),
    TOWER_BUFF_FIRE(R.raw.tower_buff_fire, 4),
    TOWER_DEBUFF_FIRE(R.raw.tower_debuff_fire, 4),
    TOWER_CHAIN_FIRE(R.raw.tower_chain_fire, 5),

    // === Impact/Explosion Sounds ===
    IMPACT_PHYSICAL(R.raw.impact_physical, 4),
    IMPACT_FIRE(R.raw.impact_fire, 5),
    IMPACT_ICE(R.raw.impact_ice, 5),
    IMPACT_LIGHTNING(R.raw.impact_lightning, 5),
    IMPACT_POISON(R.raw.impact_poison, 4),
    IMPACT_ENERGY(R.raw.impact_energy, 5),
    EXPLOSION_SMALL(R.raw.explosion_small, 7),
    EXPLOSION_LARGE(R.raw.explosion_large, 8),
    CHAIN_LIGHTNING(R.raw.chain_lightning, 6),

    // === Enemy Sounds ===
    ENEMY_SPAWN(R.raw.enemy_spawn, 3, 0.5f),
    ENEMY_DEATH(R.raw.enemy_death, 5),
    ENEMY_DEATH_BOSS(R.raw.enemy_death_boss, 9),
    ENEMY_DEATH_MINIBOSS(R.raw.enemy_death_miniboss, 8),
    ENEMY_REACHED_END(R.raw.enemy_reached_end, 9),

    // === Status Effect Sounds ===
    STATUS_SLOW(R.raw.status_slow, 3, 0.6f),
    STATUS_BURN(R.raw.status_burn, 3, 0.4f),
    STATUS_POISON(R.raw.status_poison, 3, 0.4f),
    STATUS_STUN(R.raw.status_stun, 4),
    STATUS_HEAL(R.raw.status_heal, 4),

    // === Tower Management Sounds ===
    TOWER_PLACE(R.raw.tower_place, 6),
    TOWER_UPGRADE(R.raw.tower_upgrade, 7),
    TOWER_SELL(R.raw.tower_sell, 5),
    TOWER_SELECT(R.raw.tower_select, 4),

    // === Wave/Game Sounds ===
    WAVE_START(R.raw.wave_start, 8),
    WAVE_COMPLETE(R.raw.wave_complete, 8),
    BOSS_INCOMING(R.raw.boss_incoming, 10),
    GOLD_EARNED(R.raw.gold_earned, 3, 0.5f),

    // === UI Sounds ===
    UI_BUTTON_CLICK(R.raw.ui_button_click, 6),
    UI_MENU_OPEN(R.raw.ui_menu_open, 5),
    UI_MENU_CLOSE(R.raw.ui_menu_close, 5),
    UI_ERROR(R.raw.ui_error, 7),

    // === Game State Sounds ===
    GAME_VICTORY(R.raw.game_victory, 10),
    GAME_OVER(R.raw.game_over, 10),
    GAME_PAUSE(R.raw.game_pause, 6),
    GAME_RESUME(R.raw.game_resume, 6);

    companion object {
        /**
         * Get tower fire sound for a specific tower type.
         */
        fun forTowerFire(towerType: TowerType): SoundType {
            return when (towerType) {
                TowerType.PULSE -> TOWER_PULSE_FIRE
                TowerType.SNIPER -> TOWER_SNIPER_FIRE
                TowerType.SPLASH -> TOWER_SPLASH_FIRE
                TowerType.FLAME -> TOWER_FLAME_FIRE
                TowerType.SLOW -> TOWER_SLOW_FIRE
                TowerType.TESLA -> TOWER_TESLA_FIRE
                TowerType.GRAVITY -> TOWER_GRAVITY_FIRE
                TowerType.TEMPORAL -> TOWER_TEMPORAL_FIRE
                TowerType.LASER -> TOWER_LASER_FIRE
                TowerType.POISON -> TOWER_POISON_FIRE
                TowerType.MISSILE -> TOWER_MISSILE_FIRE
                TowerType.BUFF -> TOWER_BUFF_FIRE
                TowerType.DEBUFF -> TOWER_DEBUFF_FIRE
                TowerType.CHAIN -> TOWER_CHAIN_FIRE
            }
        }

        /**
         * Get impact sound for a damage type.
         */
        fun forImpact(damageType: DamageType): SoundType {
            return when (damageType) {
                DamageType.PHYSICAL -> IMPACT_PHYSICAL
                DamageType.FIRE -> IMPACT_FIRE
                DamageType.ICE -> IMPACT_ICE
                DamageType.LIGHTNING -> IMPACT_LIGHTNING
                DamageType.POISON -> IMPACT_POISON
                DamageType.ENERGY -> IMPACT_ENERGY
                DamageType.TRUE -> IMPACT_ENERGY
            }
        }
    }
}
