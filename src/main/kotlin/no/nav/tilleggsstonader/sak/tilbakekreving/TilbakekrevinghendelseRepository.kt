package no.nav.tilleggsstonader.sak.tilbakekreving

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import no.nav.tilleggsstonader.sak.tilbakekreving.domene.Tilbakekrevinghendelse

interface TilbakekrevinghendelseRepository :
    RepositoryInterface<Tilbakekrevinghendelse, Long>,
    InsertUpdateRepository<Tilbakekrevinghendelse> {
    fun findAllByBehandlingId(behandlingId: BehandlingId): List<Tilbakekrevinghendelse>
}
