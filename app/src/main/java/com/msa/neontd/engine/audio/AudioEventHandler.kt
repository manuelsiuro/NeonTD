package com.msa.neontd.engine.audio

import com.msa.neontd.game.entities.DamageType
import com.msa.neontd.game.entities.TowerType
import com.msa.neontd.util.Vector2

/**
 * Handles audio events by mirroring VFXManager callbacks.
 * This object bridges game events to AudioManager for sound playback.
 *
 * Features rate limiting to prevent sound spam during intense gameplay.
 */
object AudioEventHandler {

    // Rate limiting to prevent sound spam
    private val lastPlayTimes = mutableMapOf<SoundType, Long>()
    private const val DEFAULT_MIN_INTERVAL_MS = 50L  // Minimum ms between same sound

    // === Tower Firing ===

    /**
     * Called when a tower fires a projectile.
     */
    fun onTowerFire(position: Vector2, direction: Vector2, towerType: TowerType) {
        val sound = SoundType.forTowerFire(towerType)
        playWithRateLimit(sound)
    }

    /**
     * Called when a tower charges up (for charging towers like LASER).
     */
    fun onTowerCharging(entityId: Int, position: Vector2, towerType: TowerType, progress: Float) {
        // Only play at certain progress thresholds to avoid spam
        if (progress < 0.1f) {
            val sound = SoundType.forTowerFire(towerType)
            playWithRateLimit(sound, 500L, volumeMultiplier = 0.3f)
        }
    }

    /**
     * Called when a tower finishes charging.
     */
    fun onTowerChargeComplete(entityId: Int, position: Vector2, towerType: TowerType) {
        val sound = SoundType.forTowerFire(towerType)
        AudioManager.playSound(sound, volumeMultiplier = 1.2f)
    }

    // === Projectile Events ===

    /**
     * Called when a projectile impacts a target.
     */
    fun onProjectileImpact(position: Vector2, damageType: DamageType, towerType: TowerType?) {
        val sound = SoundType.forImpact(damageType)
        playWithRateLimit(sound, DEFAULT_MIN_INTERVAL_MS * 2)
    }

    /**
     * Called when an explosion occurs.
     */
    fun onExplosion(position: Vector2, radius: Float, damageType: DamageType) {
        val sound = if (radius > 60f) SoundType.EXPLOSION_LARGE else SoundType.EXPLOSION_SMALL
        AudioManager.playSound(sound)
    }

    /**
     * Called when chain lightning connects between targets.
     */
    fun onChainLightning(start: Vector2, end: Vector2) {
        playWithRateLimit(SoundType.CHAIN_LIGHTNING)
    }

    /**
     * Called when a beam hits a target (for LASER tower).
     */
    fun onBeamHit(position: Vector2, towerType: TowerType) {
        // Beam hits are frequent, so use longer interval
        playWithRateLimit(SoundType.forImpact(DamageType.ENERGY), 200L, volumeMultiplier = 0.5f)
    }

    // === Enemy Events ===

    /**
     * Called when an enemy spawns.
     */
    fun onEnemySpawn(position: Vector2) {
        playWithRateLimit(SoundType.ENEMY_SPAWN, 100L)
    }

    /**
     * Called when an enemy dies.
     */
    fun onEnemyDeath(position: Vector2, isBoss: Boolean = false) {
        val sound = if (isBoss) SoundType.ENEMY_DEATH_BOSS else SoundType.ENEMY_DEATH
        AudioManager.playSound(sound)
    }

    /**
     * Called when a mini-boss dies.
     */
    fun onMiniBossDeath(position: Vector2) {
        AudioManager.playSound(SoundType.ENEMY_DEATH_MINIBOSS)
    }

    /**
     * Called when an enemy is hit but not killed.
     */
    fun onEnemyHit(position: Vector2, damageType: DamageType) {
        // Enemy hit sounds are frequent, rate limit aggressively
        playWithRateLimit(SoundType.forImpact(damageType), 100L, volumeMultiplier = 0.4f)
    }

    /**
     * Called when an enemy reaches the end of the path.
     */
    fun onEnemyReachedEnd(position: Vector2) {
        AudioManager.playSound(SoundType.ENEMY_REACHED_END)
    }

    /**
     * Called when an elemental enemy dies with a special effect.
     */
    fun onElementalDeath(position: Vector2, damageType: DamageType) {
        AudioManager.playSound(SoundType.forImpact(damageType), volumeMultiplier = 1.2f)
    }

    // === Status Effects ===

    /**
     * Called when slow effect is applied.
     */
    fun onSlowApplied(position: Vector2) {
        playWithRateLimit(SoundType.STATUS_SLOW, 200L)
    }

    /**
     * Called when burn damage ticks.
     */
    fun onBurnTick(position: Vector2) {
        playWithRateLimit(SoundType.STATUS_BURN, 500L)
    }

    /**
     * Called when poison damage ticks.
     */
    fun onPoisonTick(position: Vector2) {
        playWithRateLimit(SoundType.STATUS_POISON, 500L)
    }

    /**
     * Called when stun effect is applied.
     */
    fun onStunApplied(position: Vector2) {
        playWithRateLimit(SoundType.STATUS_STUN, 200L)
    }

    /**
     * Called when healing occurs.
     */
    fun onHeal(position: Vector2) {
        playWithRateLimit(SoundType.STATUS_HEAL, 300L)
    }

    // === Tower Management ===

    /**
     * Called when a tower is placed.
     */
    fun onTowerPlace(position: Vector2, towerType: TowerType) {
        AudioManager.playSound(SoundType.TOWER_PLACE)
    }

    /**
     * Called when a tower is upgraded.
     */
    fun onTowerUpgrade(position: Vector2, towerType: TowerType) {
        AudioManager.playSound(SoundType.TOWER_UPGRADE)
    }

    /**
     * Called when a tower is sold.
     */
    fun onTowerSell(position: Vector2, towerType: TowerType?) {
        AudioManager.playSound(SoundType.TOWER_SELL)
    }

    /**
     * Called when a tower is selected.
     */
    fun onTowerSelect(position: Vector2, towerType: TowerType) {
        AudioManager.playSound(SoundType.TOWER_SELECT)
    }

    // === Wave Events ===

    /**
     * Called when a wave starts.
     */
    fun onWaveStart(spawnPoints: List<Vector2>) {
        AudioManager.playSound(SoundType.WAVE_START)
    }

    /**
     * Called when a wave is completed.
     */
    fun onWaveComplete() {
        AudioManager.playSound(SoundType.WAVE_COMPLETE)
    }

    /**
     * Called when a boss wave is starting.
     */
    fun onBossIncoming() {
        AudioManager.playSound(SoundType.BOSS_INCOMING)
    }

    /**
     * Called during wave celebration (multiple VFX bursts).
     */
    fun onWaveCelebration(towerPositions: List<Vector2>) {
        // Only play completion sound once, not per tower
    }

    // === Game State ===

    /**
     * Called when the player wins.
     */
    fun onVictory(centerX: Float, centerY: Float) {
        AudioManager.playSound(SoundType.GAME_VICTORY)
        AudioManager.playMusic(MusicType.VICTORY_JINGLE, loop = false)
    }

    /**
     * Called when the player loses.
     */
    fun onGameOver(centerX: Float, centerY: Float) {
        AudioManager.playSound(SoundType.GAME_OVER)
        AudioManager.playMusic(MusicType.GAME_OVER_JINGLE, loop = false)
    }

    // === UI Events ===

    /**
     * Called when gold is earned.
     */
    fun onGoldEarned(amount: Int) {
        // Only play for significant amounts
        if (amount >= 10) {
            playWithRateLimit(SoundType.GOLD_EARNED, 100L)
        }
    }

    /**
     * Called when a UI button is clicked.
     */
    fun onButtonClick() {
        AudioManager.playSound(SoundType.UI_BUTTON_CLICK)
    }

    /**
     * Called when a menu opens.
     */
    fun onMenuOpen() {
        AudioManager.playSound(SoundType.UI_MENU_OPEN)
    }

    /**
     * Called when a menu closes.
     */
    fun onMenuClose() {
        AudioManager.playSound(SoundType.UI_MENU_CLOSE)
    }

    /**
     * Called when an error/invalid action occurs.
     */
    fun onError() {
        AudioManager.playSound(SoundType.UI_ERROR)
    }

    // === Helper Methods ===

    /**
     * Play a sound with rate limiting to prevent spam.
     *
     * @param sound The sound type to play
     * @param minInterval Minimum milliseconds between plays of this sound
     * @param volumeMultiplier Additional volume multiplier
     */
    private fun playWithRateLimit(
        sound: SoundType,
        minInterval: Long = DEFAULT_MIN_INTERVAL_MS,
        volumeMultiplier: Float = 1f
    ) {
        val now = System.currentTimeMillis()
        val lastTime = lastPlayTimes[sound] ?: 0L

        if (now - lastTime >= minInterval) {
            AudioManager.playSound(sound, volumeMultiplier)
            lastPlayTimes[sound] = now
        }
    }

    /**
     * Clear rate limiting state. Call when starting a new game.
     */
    fun reset() {
        lastPlayTimes.clear()
    }
}
