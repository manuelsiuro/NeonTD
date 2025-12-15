package com.msa.neontd.engine.ecs

interface Component

class ComponentStorage<T : Component> {
    private val components = mutableMapOf<Int, T>()

    fun add(entity: Entity, component: T) {
        components[entity.id] = component
    }

    fun get(entity: Entity): T? = components[entity.id]

    fun has(entity: Entity): Boolean = entity.id in components

    fun remove(entity: Entity): T? = components.remove(entity.id)

    fun getAll(): Map<Int, T> = components

    fun forEach(action: (Entity, T) -> Unit) {
        // Create a snapshot of entries to avoid ConcurrentModificationException
        val snapshot = components.entries.toList()
        for ((id, component) in snapshot) {
            action(Entity(id), component)
        }
    }

    fun clear() {
        components.clear()
    }

    val size: Int get() = components.size
}
