package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.InsertUpdateRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.RepositoryInterface
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.VedtakTilsynBarn
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface VedtakRepository : RepositoryInterface<VedtakTilsynBarn, BehandlingId>, InsertUpdateRepository<VedtakTilsynBarn>
