package com.msa.neontd.engine.ecs

abstract class System(val priority: Int = 0) {
    var isEnabled: Boolean = true

    abstract fun update(world: World, deltaTime: Float)

    open fun onEntityAdded(entity: Entity) {}
    open fun onEntityRemoved(entity: Entity) {}
}

class SystemManager {
    private val systems = mutableListOf<System>()
    private var sorted = false

    fun add(system: System) {
        systems.add(system)
        sorted = false
    }

    fun remove(system: System) {
        systems.remove(system)
    }

    fun update(world: World, deltaTime: Float) {
        if (!sorted) {
            systems.sortBy { it.priority }
            sorted = true
        }

        for (system in systems) {
            if (system.isEnabled) {
                system.update(world, deltaTime)
            }
        }
    }

    fun notifyEntityAdded(entity: Entity) {
        systems.forEach { it.onEntityAdded(entity) }
    }

    fun notifyEntityRemoved(entity: Entity) {
        systems.forEach { it.onEntityRemoved(entity) }
    }

    fun clear() {
        systems.clear()
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : System> get(clazz: Class<T>): T? {
        return systems.firstOrNull { clazz.isInstance(it) } as? T
    }
}
