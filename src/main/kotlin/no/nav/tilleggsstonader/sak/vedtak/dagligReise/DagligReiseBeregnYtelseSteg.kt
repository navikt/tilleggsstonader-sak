package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.BehandlingSteg
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import org.springframework.stereotype.Service

@Service
class DagligReiseBeregnYtelseSteg(
    private val vedtakRepository: VedtakRepository,
    private val tilkjentYtelseService: TilkjentYtelseService,
) : BehandlingSteg<Void?> {
    override fun utførSteg(
        saksbehandling: Saksbehandling,
        data: Void?,
    ) {
        opprettAndeler(saksbehandling)
    }

    override fun stegType(): StegType = StegType.BEREGNE_YTELSE

    private fun opprettAndeler(saksbehandling: Saksbehandling) {
        val vedtak =
            vedtakRepository.findByIdOrThrow(saksbehandling.id).withTypeOrThrow<InnvilgelseDagligReise>()

        val beregningsresultat = vedtak.data.beregningsresultat

        if (beregningsresultat?.offentligTransport != null) {
            // TODO støtte generering av andeler for privat bil
            tilkjentYtelseService.lagreTilkjentYtelse(
                behandlingId = saksbehandling.id,
                andeler = beregningsresultat.offentligTransport.mapTilAndelTilkjentYtelse(saksbehandling),
            )
        }
    }
}
