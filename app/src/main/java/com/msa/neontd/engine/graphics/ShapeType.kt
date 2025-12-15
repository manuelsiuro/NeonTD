package com.msa.neontd.engine.graphics

/**
 * Defines the different shape types that can be rendered
 * for towers, enemies, and other game entities.
 */
enum class ShapeType {
    // Basic shapes
    RECTANGLE,
    CIRCLE,
    DIAMOND,
    TRIANGLE,
    TRIANGLE_DOWN,
    HEXAGON,
    OCTAGON,
    PENTAGON,
    STAR,
    STAR_6,

    // Special shapes
    CROSS,
    RING,
    ARROW,
    LIGHTNING_BOLT,
    HEART,
    HOURGLASS,

    // Tower-specific
    TOWER_PULSE,      // Circle with inner rings
    TOWER_SNIPER,     // Diamond with crosshairs
    TOWER_SPLASH,     // Hexagon with burst
    TOWER_TESLA,      // Octagon with lightning
    TOWER_LASER,      // Elongated diamond with beam
    TOWER_SUPPORT     // Cross/plus shape
}
