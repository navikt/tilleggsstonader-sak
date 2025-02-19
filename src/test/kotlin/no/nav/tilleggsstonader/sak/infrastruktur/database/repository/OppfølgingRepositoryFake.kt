package no.nav.tilleggsstonader.sak.infrastruktur.database.repository

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.oppfølging.Oppfølging
import no.nav.tilleggsstonader.sak.oppfølging.OppfølgingMedDetaljer
import no.nav.tilleggsstonader.sak.oppfølging.OppfølgingRepository
import java.util.UUID

class OppfølgingRepositoryFake :
    DummyRepository<Oppfølging, UUID>({ it.id }),
    OppfølgingRepository {
    override fun markerAlleAktiveSomIkkeAktive() {
        updateAll(findAll().map { it.copy(aktiv = false) })
    }

    /**
     * Finner siste for behandling i Fake, då den joiner med fagsak i riktig implementering
     */
    override fun finnSisteForFagsak(behandlingId: BehandlingId): Oppfølging? =
        findAll()
            .filter { it.behandlingId == behandlingId }
            .maxByOrNull { it.opprettetTidspunkt }

    override fun finnAktiveMedDetaljer(): List<OppfølgingMedDetaljer> {
        TODO("Denne joiner med andre tabeller så er ikke tilgjengelig i fake")
    }

    override fun finnAktivMedDetaljer(behandlingId: BehandlingId): OppfølgingMedDetaljer {
        TODO("Denne joiner med andre tabeller så er ikke tilgjengelig i fake")
    }
}
