package com.msa.neontd.game.data

import com.msa.neontd.game.entities.EnemyType
import com.msa.neontd.game.entities.TowerType
import com.msa.neontd.game.synergies.TowerSynergy

data class TowerInfo(
    val type: TowerType,
    val damage: Float,
    val range: Float,
    val fireRate: Float,
    val specialAbility: String?,
    val tips: String
)

data class EnemyInfo(
    val type: EnemyType,
    val specialAbility: String?,
    val weaknesses: List<String>,
    val resistances: List<String>,
    val tips: String
)

data class SynergyInfo(
    val synergy: TowerSynergy,
    val detailedEffect: String,
    val tips: String
)

object Encyclopedia {

    val towers: List<TowerInfo> = listOf(
        TowerInfo(
            type = TowerType.PULSE,
            damage = 10f,
            range = 150f,
            fireRate = 1.5f,
            specialAbility = null,
            tips = "Reliable starter tower. Place near path bends for maximum coverage."
        ),
        TowerInfo(
            type = TowerType.SNIPER,
            damage = 50f,
            range = 300f,
            fireRate = 0.5f,
            specialAbility = "20% critical hit chance (3x damage)",
            tips = "Best against single strong enemies. Place at map center for full coverage."
        ),
        TowerInfo(
            type = TowerType.SPLASH,
            damage = 15f,
            range = 140f,
            fireRate = 0.8f,
            specialAbility = "50 unit splash radius",
            tips = "Essential for crowds. Place where enemies bunch up on tight paths."
        ),
        TowerInfo(
            type = TowerType.FLAME,
            damage = 8f,
            range = 100f,
            fireRate = 10f,
            specialAbility = "Cone attack + 5 burn damage/2 sec",
            tips = "High DPS at close range. Combine with Slow towers for devastating effect."
        ),
        TowerInfo(
            type = TowerType.SLOW,
            damage = 5f,
            range = 130f,
            fireRate = 2f,
            specialAbility = "30% slow for 2 seconds",
            tips = "Force multiplier. Place early in the path to boost other towers' effectiveness."
        ),
        TowerInfo(
            type = TowerType.TESLA,
            damage = 20f,
            range = 140f,
            fireRate = 0.8f,
            specialAbility = "Lightning chains to 3 enemies (80 unit range)",
            tips = "Excels against grouped enemies. Useless against lone targets."
        ),
        TowerInfo(
            type = TowerType.GRAVITY,
            damage = 5f,
            range = 120f,
            fireRate = 1f,
            specialAbility = "Pulls enemies toward tower center",
            tips = "Strategic placement. Pull enemies into Splash or Flame tower kill zones."
        ),
        TowerInfo(
            type = TowerType.TEMPORAL,
            damage = 0f,
            range = 100f,
            fireRate = 0.2f,
            specialAbility = "Complete freeze for 1.5 seconds",
            tips = "Premium crowd control. Save for boss waves or emergencies."
        ),
        TowerInfo(
            type = TowerType.LASER,
            damage = 15f,
            range = 180f,
            fireRate = 20f,
            specialAbility = "Continuous beam attack",
            tips = "Sustained damage champion. Destroys single targets. Poor vs fast movers."
        ),
        TowerInfo(
            type = TowerType.POISON,
            damage = 3f,
            range = 140f,
            fireRate = 1f,
            specialAbility = "8 poison damage/sec for 4 seconds",
            tips = "Damage over time stacks. Great opening tower, let poison do the work."
        ),
        TowerInfo(
            type = TowerType.MISSILE,
            damage = 35f,
            range = 200f,
            fireRate = 0.6f,
            specialAbility = "Homing missiles + 30 unit splash",
            tips = "Never misses! High damage with small AOE. Excellent vs flyers."
        ),
        TowerInfo(
            type = TowerType.BUFF,
            damage = 0f,
            range = 120f,
            fireRate = 0f,
            specialAbility = "Buffs nearby tower damage, range, and fire rate",
            tips = "Huge value when surrounded by damage towers. Build tower clusters."
        ),
        TowerInfo(
            type = TowerType.DEBUFF,
            damage = 0f,
            range = 140f,
            fireRate = 1f,
            specialAbility = "Reduces enemy armor and speed",
            tips = "Place early to soften enemies for damage towers. Essential vs armored."
        ),
        TowerInfo(
            type = TowerType.CHAIN,
            damage = 12f,
            range = 150f,
            fireRate = 1f,
            specialAbility = "Bounces between 4 enemies (100 unit range)",
            tips = "Like Tesla but physical damage. Great vs lightning-immune enemies."
        )
    )

    val enemies: List<EnemyInfo> = listOf(
        EnemyInfo(
            type = EnemyType.BASIC,
            specialAbility = null,
            weaknesses = emptyList(),
            resistances = emptyList(),
            tips = "Standard enemy. No special threat. Good for tower testing."
        ),
        EnemyInfo(
            type = EnemyType.FAST,
            specialAbility = "High speed movement",
            weaknesses = listOf("Low health"),
            resistances = emptyList(),
            tips = "Speed is their defense. Use Slow towers or high fire-rate towers."
        ),
        EnemyInfo(
            type = EnemyType.TANK,
            specialAbility = "High armor",
            weaknesses = listOf("Very slow"),
            resistances = listOf("30% physical"),
            tips = "Use energy or elemental damage. Armor ignores DOT - poison is effective."
        ),
        EnemyInfo(
            type = EnemyType.FLYING,
            specialAbility = "Ignores terrain obstacles",
            weaknesses = listOf("Low health"),
            resistances = emptyList(),
            tips = "May skip portions of the path. Cover the entire map, not just paths."
        ),
        EnemyInfo(
            type = EnemyType.HEALER,
            specialAbility = "Heals nearby enemies (2% health/sec in 80 units)",
            weaknesses = emptyList(),
            resistances = emptyList(),
            tips = "Priority target! Kill before other enemies or healing negates your damage."
        ),
        EnemyInfo(
            type = EnemyType.SHIELDED,
            specialAbility = "Shield (50% HP) + regenerates after 3 sec",
            weaknesses = emptyList(),
            resistances = emptyList(),
            tips = "Focus fire to break shield before it regenerates. Burst damage wins."
        ),
        EnemyInfo(
            type = EnemyType.SPAWNER,
            specialAbility = "Spawns 2 Drones every 5 seconds",
            weaknesses = emptyList(),
            resistances = emptyList(),
            tips = "Kill quickly or face an army. High-damage single target towers best."
        ),
        EnemyInfo(
            type = EnemyType.PHASING,
            specialAbility = "Phases out (untargetable) for 1.5 sec every 4 sec",
            weaknesses = emptyList(),
            resistances = emptyList(),
            tips = "DOT continues during phase. Poison tower counters this enemy perfectly."
        ),
        EnemyInfo(
            type = EnemyType.SPLITTING,
            specialAbility = "Splits into 2 Runners on death",
            weaknesses = emptyList(),
            resistances = emptyList(),
            tips = "Prepare for the split! Keep splash damage ready for the spawns."
        ),
        EnemyInfo(
            type = EnemyType.REGENERATING,
            specialAbility = "Regenerates 2% health/sec",
            weaknesses = emptyList(),
            resistances = emptyList(),
            tips = "Burst damage required. Slow sustained damage gets out-healed."
        ),
        EnemyInfo(
            type = EnemyType.STEALTH,
            specialAbility = "Becomes invisible, revealed for 2 sec when hit",
            weaknesses = listOf("Low health"),
            resistances = emptyList(),
            tips = "AOE towers can hit invisible enemies. Splash and Tesla counter stealth."
        ),
        EnemyInfo(
            type = EnemyType.FIRE_ELEMENTAL,
            specialAbility = "Fire immunity",
            weaknesses = listOf("50% extra ice damage"),
            resistances = listOf("100% fire"),
            tips = "Flame towers useless! Use Slow tower or ice-based attacks."
        ),
        EnemyInfo(
            type = EnemyType.ICE_ELEMENTAL,
            specialAbility = "Ice immunity",
            weaknesses = listOf("50% extra fire damage"),
            resistances = listOf("100% ice"),
            tips = "Slow tower ineffective! Use Flame tower for bonus damage."
        ),
        EnemyInfo(
            type = EnemyType.LIGHTNING_ELEMENTAL,
            specialAbility = "Lightning immunity",
            weaknesses = listOf("25% extra physical damage"),
            resistances = listOf("100% lightning"),
            tips = "Tesla useless! Chain tower (physical) is the direct counter."
        ),
        EnemyInfo(
            type = EnemyType.MINI_BOSS,
            specialAbility = "Elite unit with high stats",
            weaknesses = emptyList(),
            resistances = emptyList(),
            tips = "Prepare burst damage. Debuff tower reduces armor significantly."
        ),
        EnemyInfo(
            type = EnemyType.BOSS,
            specialAbility = "Massive health, high armor, elemental resistances",
            weaknesses = emptyList(),
            resistances = listOf("20% physical/fire/ice/lightning"),
            tips = "All hands on deck! Stack debuffs, use Temporal freeze, and focus fire."
        )
    )

    val synergies: List<SynergyInfo> = listOf(
        SynergyInfo(
            synergy = TowerSynergy.SHATTER,
            detailedEffect = "When Slow and Flame towers are placed within 2 cells, the Flame tower deals 50% bonus damage to slowed/frozen enemies.",
            tips = "Place Slow first to apply debuff, then let Flame tower melt them. Perfect for choke points."
        ),
        SynergyInfo(
            synergy = TowerSynergy.CHAIN_REACTION,
            detailedEffect = "When Tesla and Chain towers are placed within 2 cells, both gain +2 chain targets.",
            tips = "Stack these together near crowded paths for devastating chain damage to groups."
        ),
        SynergyInfo(
            synergy = TowerSynergy.SUPPORT_GRID,
            detailedEffect = "When Buff and Debuff towers are placed within 2 cells, both gain +25% aura radius.",
            tips = "Create a support cluster to cover more towers and enemies. Central placement recommended."
        ),
        SynergyInfo(
            synergy = TowerSynergy.TOXIC_FIRE,
            detailedEffect = "When Poison and Flame towers are placed within 2 cells, enemies with both DOT effects take 2x DOT damage.",
            tips = "Apply poison first, then burn. The combined damage over time is devastating."
        ),
        SynergyInfo(
            synergy = TowerSynergy.PRECISION_STRIKE,
            detailedEffect = "When Sniper and Debuff towers are placed within 2 cells, armor break effects last 5 seconds instead of normal duration.",
            tips = "The extended armor break lets Sniper's high damage punch through armored bosses."
        ),
        SynergyInfo(
            synergy = TowerSynergy.GRAVITY_WELL,
            detailedEffect = "When Gravity and Splash towers are placed within 2 cells, Splash gains +30% splash radius.",
            tips = "Pull enemies together, then blast them. Perfect synergy for crowd control."
        ),
        SynergyInfo(
            synergy = TowerSynergy.TIME_LOCK,
            detailedEffect = "When Temporal and Slow towers are placed within 2 cells, slow effects last 2x as long.",
            tips = "Combined with Temporal's freeze, enemies barely move at all. Ultimate stall strategy."
        ),
        SynergyInfo(
            synergy = TowerSynergy.OVERLOAD_SYNERGY,
            detailedEffect = "When Laser and Tesla towers are placed within 2 cells, both gain +25% damage.",
            tips = "Pure damage boost. Place these together for maximum electrical devastation."
        )
    )

    fun getTowerInfo(type: TowerType): TowerInfo? = towers.find { it.type == type }

    fun getEnemyInfo(type: EnemyType): EnemyInfo? = enemies.find { it.type == type }

    fun getSynergyInfo(synergy: TowerSynergy): SynergyInfo? = synergies.find { it.synergy == synergy }
}
