package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakTilsynBarn
import org.springframework.stereotype.Repository

@Repository
interface VedtakRepository :
    RepositoryInterface<VedtakTilsynBarn, BehandlingId>,
    InsertUpdateRepository<VedtakTilsynBarn>
