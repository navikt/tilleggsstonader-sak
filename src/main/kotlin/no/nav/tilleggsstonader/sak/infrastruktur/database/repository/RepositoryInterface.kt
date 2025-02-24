package no.nav.tilleggsstonader.sak.infrastruktur.database.repository

import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.NoRepositoryBean

/**
 * På grunn av att vi setter id's på våre entitetet så prøver spring å oppdatere våre entiteter i stedet for å ta insert
 */
@NoRepositoryBean
interface RepositoryInterface<T, ID> : CrudRepository<T, ID> {
    @Deprecated("Støttes ikke, bruk insert/update")
    override fun <S : T> save(entity: S & Any): S & Any {
        error("Not implemented - Use InsertUpdateRepository - insert/update")
    }

    @Deprecated("Støttes ikke, bruk insertAll/updateAll")
    override fun <S : T> saveAll(entities: Iterable<S>): Iterable<S> {
        error("Not implemented - Use InsertUpdateRepository - insertAll/updateAll")
    }
}
