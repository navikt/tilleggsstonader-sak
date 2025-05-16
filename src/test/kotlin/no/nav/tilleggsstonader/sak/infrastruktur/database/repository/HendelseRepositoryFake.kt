package no.nav.tilleggsstonader.sak.infrastruktur.database.repository

import no.nav.tilleggsstonader.sak.hendelser.Hendelse
import no.nav.tilleggsstonader.sak.hendelser.HendelseRepository
import no.nav.tilleggsstonader.sak.hendelser.TypeHendelse

class HendelseRepositoryFake :
    DummyRepository<Hendelse, String>({ it.id }),
    HendelseRepository {
    override fun existsByTypeAndId(
        type: TypeHendelse,
        id: String,
    ): Boolean = findAll().any { it.type == type && it.id == id }

    override fun findByTypeAndId(
        type: TypeHendelse,
        id: String,
    ): Hendelse? = findAll().firstOrNull { it.type == type && it.id == id }
}
