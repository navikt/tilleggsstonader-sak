package no.nav.tilleggsstonader.sak.infrastruktur.database.repository

import java.util.Optional

/**
 * Implementerer de vanlige metodene fra [RepositoryInterface] og [InsertUpdateRepository]
 * Sånn at vanlige fakes kan extende denne for å ikke implementere de i hver fake
 *
 * @param getId brukes for å finne id for en entitet
 */
open class DummyRepository<T : Any, ID : Any>(
    val getId: (T) -> ID,
) : RepositoryInterface<T, ID>,
    InsertUpdateRepository<T> {
    private val storage = mutableMapOf<ID, T>()

    override fun findById(id: ID): Optional<T> = Optional.ofNullable(storage[id])

    override fun existsById(id: ID): Boolean = storage.containsKey(id)

    override fun findAll(): Iterable<T> = storage.values

    override fun count(): Long = storage.size.toLong()

    override fun deleteAll() {
        storage.clear()
    }

    override fun deleteAll(entities: Iterable<T>) {
        entities.forEach { deleteById(getId(it)) }
    }

    override fun deleteAllById(ids: Iterable<ID>) {
        ids.forEach { deleteById(it) }
    }

    override fun delete(entity: T) {
        deleteById(getId(entity))
    }

    override fun deleteById(id: ID) {
        storage.remove(id)
    }

    override fun findAllById(ids: Iterable<ID>): Iterable<T> = ids.mapNotNull { storage[it] }

    override fun insert(t: T): T {
        val id = getId(t)
        require(!storage.containsKey(id)) {
            "Entity with id=$id already exists"
        }
        storage[id] = t
        return t
    }

    override fun insertAll(list: List<T>): List<T> = list.map { insert(it) }

    override fun update(t: T): T {
        val id = getId(t)
        require(storage.containsKey(id)) {
            "Cannot update entity with id=$id because it doesn't exist"
        }
        storage[id] = t
        return t
    }

    override fun updateAll(list: List<T>): List<T> = list.map { update(it) }
}
