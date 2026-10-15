package no.nav.tilleggsstonader.sak.vedtak.flytting

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.periode.avkortFraOgMed
import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vedtak.BeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.BeregningsplanUtleder
import no.nav.tilleggsstonader.sak.vedtak.OpphørValideringService
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagFlytting
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørFlytting
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseFlytting
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørFlytting
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.flytting.beregning.FlyttingBeregningService
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.AvslagFlyttingDto
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.InnvilgelseFlyttingRequest
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.OpphørFlyttingRequest
import no.nav.tilleggsstonader.sak.vedtak.flytting.dto.VedtakFlyttingRequest
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class FlyttingBeregnYtelseSteg(
    private val beregningService: FlyttingBeregningService,
    private val beregningsplanUtleder: BeregningsplanUtleder,
    private val opphørValideringService: OpphørValideringService,
    vedtakRepository: VedtakRepository,
    tilkjentYtelseService: TilkjentYtelseService,
    simuleringService: SimuleringService,
) : BeregnYtelseSteg<VedtakFlyttingRequest>(
        stønadstype = listOf(Stønadstype.FLYTTING_TSO, Stønadstype.FLYTTING_TSR),
        vedtakRepository = vedtakRepository,
        tilkjentYtelseService = tilkjentYtelseService,
        simuleringService = simuleringService,
    ) {
    override fun lagreVedtakForSatsjustering(
        saksbehandling: Saksbehandling,
        vedtak: VedtakFlyttingRequest,
        satsjusteringFra: LocalDate,
    ) {
        logger.info("Lagrer vedtak for satsjustering for behandling=${saksbehandling.id}, satsjusteringFra=$satsjusteringFra")

        val innvilgelse = vedtak as InnvilgelseFlyttingRequest
        val vedtaksperioder = innvilgelse.vedtaksperioder()
        val beregningsplan = BeregningsplanUtleder.utledForOpphørEllerSatsjustering(satsjusteringFra)
        val beregningsresultat =
            beregningService.beregn(
                behandling = saksbehandling,
                vedtaksperioder = vedtaksperioder,
                beregningsplan = beregningsplan,
                typeVedtak = TypeVedtak.INNVILGELSE,
            )

        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.INNVILGELSE,
                data =
                    InnvilgelseFlytting(
                        vedtaksperioder = vedtaksperioder,
                        beregningsplan = beregningsplan,
                        beregningsresultat = beregningsresultat,
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

    override fun lagreVedtak(
        saksbehandling: Saksbehandling,
        vedtak: VedtakFlyttingRequest,
    ) {
        when (vedtak) {
            is AvslagFlyttingDto -> lagreAvslag(saksbehandling, vedtak)
            is InnvilgelseFlyttingRequest -> lagreInnvilgelse(saksbehandling, vedtak)
            is OpphørFlyttingRequest -> lagreOpphør(saksbehandling, vedtak)
        }
    }

    private fun lagreInnvilgelse(
        saksbehandling: Saksbehandling,
        vedtak: InnvilgelseFlyttingRequest,
    ) {
        val vedtaksperioder = vedtak.vedtaksperioder().sorted()
        val beregningsplan =
            beregningsplanUtleder.utledForInnvilgelse(
                saksbehandling = saksbehandling,
                vedtaksperioder = vedtaksperioder,
            )
        val typeVedtak = TypeVedtak.INNVILGELSE

        val beregningsresultat = beregningService.beregn(saksbehandling, vedtaksperioder, beregningsplan, typeVedtak)

        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = typeVedtak,
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

    private fun lagreAvslag(
        saksbehandling: Saksbehandling,
        vedtak: AvslagFlyttingDto,
    ) {
        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.AVSLAG,
                data = AvslagFlytting(årsaker = vedtak.årsakerAvslag, begrunnelse = vedtak.begrunnelse),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = null,
            ),
        )
    }

    private fun lagreOpphør(
        saksbehandling: Saksbehandling,
        vedtak: OpphørFlyttingRequest,
    ) {
        feilHvis(saksbehandling.forrigeIverksatteBehandlingId == null) {
            "Opphør er et ugyldig vedtaksresultat fordi behandlingen er en førstegangsbehandling"
        }
        feilHvis(vedtak.opphørsdato == null) {
            "Opphørsdato er ikke satt"
        }
        val opphørsdato = vedtak.opphørsdato
        val forrigeVedtak =
            vedtakRepository
                .findByIdOrThrow(saksbehandling.forrigeIverksatteBehandlingId)
                .withTypeOrThrow<InnvilgelseEllerOpphørFlytting>()

        opphørValideringService.validerVilkårperioder(saksbehandling, opphørsdato)
        opphørValideringService.validerVedtaksperioderAvkortetVedOpphør(
            forrigeBehandlingsVedtaksperioder = forrigeVedtak.data.vedtaksperioder,
            opphørsdato = opphørsdato,
        )

        val avkortetVedtaksperioder = forrigeVedtak.data.vedtaksperioder.avkortFraOgMed(opphørsdato.minusDays(1))
        val beregningsplan = BeregningsplanUtleder.utledForOpphørEllerSatsjustering(opphørsdato)
        val beregningsresultat =
            beregningService.beregn(
                behandling = saksbehandling,
                vedtaksperioder = avkortetVedtaksperioder,
                beregningsplan = beregningsplan,
                typeVedtak = TypeVedtak.OPPHØR,
            )
        val andeler = beregningsresultat.mapTilAndeler(saksbehandling.stønadstype)
        opphørValideringService.validerIngenUtbetalingEtterOpphørsdatoFlytting(andeler, opphørsdato)

        vedtakRepository.insert(
            GeneriskVedtak(
                behandlingId = saksbehandling.id,
                type = TypeVedtak.OPPHØR,
                data =
                    OpphørFlytting(
                        vedtaksperioder = avkortetVedtaksperioder,
                        beregningsplan = beregningsplan,
                        beregningsresultat = beregningsresultat,
                        årsaker = vedtak.årsakerOpphør,
                        begrunnelse = vedtak.begrunnelse,
                    ),
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = null,
                opphørsdato = opphørsdato,
            ),
        )
        tilkjentYtelseService.lagreTilkjentYtelse(
            behandlingId = saksbehandling.id,
            andeler = andeler,
        )
    }
}
