package no.nav.tilleggsstonader.sak.infrastruktur.database.repository

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak

class VedtakRepositoryFake :
    DummyRepository<Vedtak, BehandlingId>({ it.behandlingId }),
    VedtakRepository {
    override fun findByBehandlingId(behandlingId: BehandlingId): Vedtak = findAll().single { it.behandlingId == behandlingId }
}
