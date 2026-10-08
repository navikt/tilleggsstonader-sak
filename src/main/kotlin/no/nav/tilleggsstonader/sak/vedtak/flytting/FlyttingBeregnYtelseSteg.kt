package no.nav.tilleggsstonader.sak.vedtak.flytting

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vedtak.BeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.BeregningsplanUtleder
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseFlytting
import no.nav.tilleggsstonader.sak.vedtak.flytting.beregning.FlyttingBeregningService
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.InnvilgelseFlyttingRequest
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class FlyttingBeregnYtelseSteg(
    private val beregningService: FlyttingBeregningService,
    private val beregningsplanUtleder: BeregningsplanUtleder,
    vedtakRepository: VedtakRepository,
    tilkjentYtelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
) : BeregnYtelseSteg<InnvilgelseFlyttingRequest>(
        stønadstype = listOf(Stønadstype.FLYTTING_TSO, Stønadstype.FLYTTING_TSR),
        vedtakRepository = vedtakRepository,
        tilkjentYtelseService = tilkjentYtelseService,
        simuleringService = simuleringService,
    ) {
    override fun lagreVedtakForSatsjustering(
        saksbehandling: Saksbehandling,
        vedtak: InnvilgelseFlyttingRequest,
        satsjusteringFra: LocalDate,
    ) {
        feil(
            "Det ble forsøkt å lagre vedtak for satsjustering for behandling med id ${saksbehandling.id}. " +
                "Satsjustering for flytting er ikke implementert",
        )
    }

    override fun lagreVedtak(
        saksbehandling: Saksbehandling,
        vedtak: InnvilgelseFlyttingRequest,
    ) {
        val vedtaksperioder = vedtak.vedtaksperioder().sorted()
        val beregningsplan =
            beregningsplanUtleder.utledForInnvilgelse(
                saksbehandling = saksbehandling,
                vedtaksperioder = vedtaksperioder,
            )
        val beregningsresultat = beregningService.beregn(saksbehandling, vedtaksperioder, beregningsplan)
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.INNVILGELSE,
                data =
                    InnvilgelseFlytting(
                        vedtaksperioder = vedtaksperioder,
                        beregningsplan = beregningsplan,
                        beregningsresultat = beregningsresultat,
                        begrunnelse = vedtak.begrunnelse,
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = beregningsplan.legacyTidligsteEndring(),
            ),
        )
        tilkjentYtelseService.lagreTilkjentYtelse(
            behandlingId = saksbehandling.id,
            andeler = beregningsresultat.mapTilAndeler(saksbehandling.stønadstype),
        )
    }
}
