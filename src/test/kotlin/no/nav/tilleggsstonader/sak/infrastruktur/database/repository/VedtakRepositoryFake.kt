package no.nav.tilleggsstonader.sak.infrastruktur.database.repository

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.takeIfType

class VedtakRepositoryFake :
    DummyRepository<Vedtak, BehandlingId>({ it.behandlingId }),
    VedtakRepository {
    override fun harRammevedtak(behandlingIder: List<BehandlingId>): Boolean =
        findAll()
            .filter { it.behandlingId in behandlingIder }
            .any { vedtak ->
                vedtak
                    .takeIfType<InnvilgelseEllerOpphørDagligReise>()
                    ?.data
                    ?.rammevedtakPrivatBil
                    ?.reiser
                    ?.isNotEmpty() == true
            }
}
