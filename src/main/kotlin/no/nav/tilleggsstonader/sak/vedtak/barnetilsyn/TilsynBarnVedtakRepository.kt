package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import org.springframework.stereotype.Repository

@Repository
interface TilsynBarnVedtakRepository :
    VedtakRepository<VedtakTilsynBarn>,
    RepositoryInterface<VedtakTilsynBarn, BehandlingId>,
    InsertUpdateRepository<VedtakTilsynBarn>
