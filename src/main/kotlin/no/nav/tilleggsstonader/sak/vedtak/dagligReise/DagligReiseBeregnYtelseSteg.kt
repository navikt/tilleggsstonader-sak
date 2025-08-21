package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.vedtak.BeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.OffentligTransportBeregningService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.UtgiftOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.VedtakDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDomene
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import org.springframework.stereotype.Service

@Service
class DagligReiseBeregnYtelseSteg(
    private val beregningService: OffentligTransportBeregningService,
    private val vilkårService: VilkårService,
    vedtakRepository: VedtakRepository,
    tilkjentYtelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
    unleashService: UnleashService,
) : BeregnYtelseSteg<VedtakDagligReiseRequest>(
        // TODO - legge inn TSR
        stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        vedtakRepository = vedtakRepository,
        tilkjentYtelseService = tilkjentYtelseService,
        simuleringService = simuleringService,
        unleashService = unleashService,
    ) {
    override fun lagreVedtak(
        saksbehandling: Saksbehandling,
        vedtak: VedtakDagligReiseRequest,
    ) {
        when (vedtak) {
            is InnvilgelseDagligReiseRequest -> {
                val vilkår = vilkårService.hentVilkår(saksbehandling.id)
                val utgifter =
                    vilkår
                        .filter { it.offentligTransport != null }
                        .map { vilkår ->
                            UtgiftOffentligTransport(
                                fom = vilkår.fom!!,
                                tom = vilkår.tom!!,
                                antallReisedagerPerUke = vilkår.offentligTransport?.reisedagerPerUke!!,
                                prisEnkelbillett = vilkår.offentligTransport.prisEnkelbillett,
                                pris30dagersbillett = vilkår.offentligTransport.prisTrettidagersbillett,
                            )
                        }
                beregningService.beregn(
                    utgifter = utgifter,
                    vedtaksperioder = vedtak.vedtaksperioder.tilDomene().sorted(),
                )
            }
        }
    }
}
