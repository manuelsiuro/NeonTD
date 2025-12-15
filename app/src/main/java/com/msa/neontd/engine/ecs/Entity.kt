package com.msa.neontd.engine.ecs

@JvmInline
value class Entity(val id: Int) {
    companion object {
        val INVALID = Entity(-1)
    }

    fun isValid(): Boolean = id >= 0
}

class EntityManager {
    private var nextId = 0
    private val recycledIds = mutableListOf<Int>()
    private val aliveEntities = mutableSetOf<Int>()

    fun create(): Entity {
        val id = if (recycledIds.isNotEmpty()) {
            recycledIds.removeAt(recycledIds.lastIndex)
        } else {
            nextId++
        }
        aliveEntities.add(id)
        return Entity(id)
    }

    fun destroy(entity: Entity) {
        if (entity.id in aliveEntities) {
            aliveEntities.remove(entity.id)
            recycledIds.add(entity.id)
        }
    }

    fun isAlive(entity: Entity): Boolean = entity.id in aliveEntities

    fun getAllEntities(): Set<Int> = aliveEntities.toSet()

    fun clear() {
        aliveEntities.clear()
        recycledIds.clear()
        nextId = 0
    }

    val count: Int get() = aliveEntities.size
}
