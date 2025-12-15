package com.msa.neontd.engine.ecs

import kotlin.reflect.KClass

class World {
    private val entityManager = EntityManager()
    private val systemManager = SystemManager()
    @PublishedApi
    internal val componentStorages = mutableMapOf<KClass<*>, ComponentStorage<*>>()
    private val pendingDestroy = mutableListOf<Entity>()

    fun createEntity(): Entity {
        val entity = entityManager.create()
        systemManager.notifyEntityAdded(entity)
        return entity
    }

    fun destroyEntity(entity: Entity) {
        pendingDestroy.add(entity)
    }

    private fun processPendingDestructions() {
        for (entity in pendingDestroy) {
            if (entityManager.isAlive(entity)) {
                systemManager.notifyEntityRemoved(entity)
                // Remove all components
                componentStorages.values.forEach { storage ->
                    @Suppress("UNCHECKED_CAST")
                    (storage as ComponentStorage<Component>).remove(entity)
                }
                entityManager.destroy(entity)
            }
        }
        pendingDestroy.clear()
    }

    fun isAlive(entity: Entity): Boolean = entityManager.isAlive(entity)

    inline fun <reified T : Component> addComponent(entity: Entity, component: T) {
        getOrCreateStorage<T>().add(entity, component)
    }

    inline fun <reified T : Component> getComponent(entity: Entity): T? {
        return getStorage<T>()?.get(entity)
    }

    inline fun <reified T : Component> hasComponent(entity: Entity): Boolean {
        return getStorage<T>()?.has(entity) == true
    }

    inline fun <reified T : Component> removeComponent(entity: Entity): T? {
        return getStorage<T>()?.remove(entity)
    }

    inline fun <reified T : Component> getStorage(): ComponentStorage<T>? {
        @Suppress("UNCHECKED_CAST")
        return componentStorages[T::class] as? ComponentStorage<T>
    }

    inline fun <reified T : Component> getOrCreateStorage(): ComponentStorage<T> {
        @Suppress("UNCHECKED_CAST")
        return componentStorages.getOrPut(T::class) {
            ComponentStorage<T>()
        } as ComponentStorage<T>
    }

    fun addSystem(system: System) {
        systemManager.add(system)
    }

    fun removeSystem(system: System) {
        systemManager.remove(system)
    }

    fun <T : System> getSystem(clazz: Class<T>): T? {
        return systemManager.get(clazz)
    }

    fun update(deltaTime: Float) {
        systemManager.update(this, deltaTime)
        processPendingDestructions()
    }

    fun clear() {
        componentStorages.values.forEach { it.clear() }
        componentStorages.clear()
        entityManager.clear()
        systemManager.clear()
    }

    val entityCount: Int get() = entityManager.count

    // Query helpers for iterating entities with specific components
    inline fun <reified T : Component> forEach(crossinline action: (Entity, T) -> Unit) {
        getStorage<T>()?.forEach { entity, comp -> action(entity, comp) }
    }

    inline fun <reified T1 : Component, reified T2 : Component> forEachWith(
        crossinline action: (Entity, T1, T2) -> Unit
    ) {
        val storage1 = getStorage<T1>() ?: return
        val storage2 = getStorage<T2>() ?: return

        storage1.forEach { entity, comp1 ->
            storage2.get(entity)?.let { comp2 ->
                action(entity, comp1, comp2)
            }
        }
    }

    inline fun <reified T1 : Component, reified T2 : Component, reified T3 : Component> forEachWith(
        crossinline action: (Entity, T1, T2, T3) -> Unit
    ) {
        val storage1 = getStorage<T1>() ?: return
        val storage2 = getStorage<T2>() ?: return
        val storage3 = getStorage<T3>() ?: return

        storage1.forEach { entity, comp1 ->
            val comp2 = storage2.get(entity) ?: return@forEach
            val comp3 = storage3.get(entity) ?: return@forEach
            action(entity, comp1, comp2, comp3)
        }
    }

    inline fun <reified T1 : Component, reified T2 : Component, reified T3 : Component, reified T4 : Component> forEachWith(
        crossinline action: (Entity, T1, T2, T3, T4) -> Unit
    ) {
        val storage1 = getStorage<T1>() ?: return
        val storage2 = getStorage<T2>() ?: return
        val storage3 = getStorage<T3>() ?: return
        val storage4 = getStorage<T4>() ?: return

        storage1.forEach { entity, comp1 ->
            val comp2 = storage2.get(entity) ?: return@forEach
            val comp3 = storage3.get(entity) ?: return@forEach
            val comp4 = storage4.get(entity) ?: return@forEach
            action(entity, comp1, comp2, comp3, comp4)
        }
    }
}
