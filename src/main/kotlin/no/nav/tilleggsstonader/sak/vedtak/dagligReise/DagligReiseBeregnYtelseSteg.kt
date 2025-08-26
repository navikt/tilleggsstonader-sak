package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vedtak.BeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.OffentligTransportBeregningService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Beregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.UtgiftOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.VedtakDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDomene
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class DagligReiseBeregnYtelseSteg(
    private val beregningService: OffentligTransportBeregningService,
    private val vilkårService: VilkårService,
    private val utledTidligsteEndringService: UtledTidligsteEndringService,
    vedtakRepository: VedtakRepository,
    tilkjentYtelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
    unleashService: UnleashService,
) : BeregnYtelseSteg<VedtakDagligReiseRequest>(
        stønadstype = listOf(Stønadstype.DAGLIG_REISE_TSO, Stønadstype.DAGLIG_REISE_TSR),
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
            is InnvilgelseDagligReiseRequest -> beregnOgLagreInnvilgelse(saksbehandling, vedtak)
            is AvslagDagligReise -> TODO()
            is OpphørDagligReise -> TODO()
        }
    }

    private fun beregnOgLagreInnvilgelse(
        saksbehandling: Saksbehandling,
        vedtak: InnvilgelseDagligReiseRequest,
    ) {
        val vedtaksperioder = vedtak.vedtaksperioder.tilDomene()
        val utgifter =
            vilkårService.hentVilkår(saksbehandling.id).map { vilkår ->
                val offentligTransport = vilkår.offentligTransport!!
                UtgiftOffentligTransport(
                    fom = vilkår.fom!!,
                    tom = vilkår.tom!!,
                    antallReisedagerPerUke = offentligTransport.reisedagerPerUke,
                    prisEnkelbillett = offentligTransport.prisEnkelbillett,
                    pris30dagersbillett = offentligTransport.prisTrettidagersbillett,
                )
            }

        val tidligsteEndring =
            utledTidligsteEndringService.utledTidligsteEndringForBeregning(
                saksbehandling.id,
                vedtaksperioder,
            )
        val beregningsresultat =
            beregningService.beregn(
                utgifter = utgifter,
                vedtaksperioder = vedtaksperioder,
            )
        lagreInnvilgetVedtak(
            behandling = saksbehandling,
            beregningsresultat = beregningsresultat,
            vedtaksperioder = vedtaksperioder,
            begrunnelse = vedtak.begrunnelse,
            tidligsteEndring = tidligsteEndring,
        )
    }

    private fun lagreInnvilgetVedtak(
        behandling: Saksbehandling,
        beregningsresultat: Beregningsresultat,
        vedtaksperioder: List<Vedtaksperiode>,
        begrunnelse: String?,
        tidligsteEndring: LocalDate?,
    ) {
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = behandling.id,
                type = TypeVedtak.INNVILGELSE,
                data =
                    InnvilgelseDagligReise(
                        vedtaksperioder = vedtaksperioder,
                        begrunnelse = begrunnelse,
                        beregningsresultat = beregningsresultat,
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                // TODO - få denne forklart
                tidligsteEndring = if (unleashService.isEnabled(Toggle.SKAL_UTLEDE_ENDRINGSDATO_AUTOMATISK)) tidligsteEndring else null,
            ),
        )
    }
}
