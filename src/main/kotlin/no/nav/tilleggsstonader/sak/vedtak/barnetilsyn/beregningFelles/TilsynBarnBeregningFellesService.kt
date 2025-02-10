package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.YEAR_MONTH_MIN
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBarnBeregningValideringUtilFelles.validerPerioderForInnvilgelse
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBeregningUtilsFelles.brukBeregningsgrunnlagFraOgMedRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV1.DEKNINGSGRAD_TILSYN_BARN
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.tilAktiviteter
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.beregningsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

private val SNITT_ANTALL_VIRKEDAGER_PER_MÅNED = BigDecimal("21.67")

@Service
class TilsynBarnBeregningFellesService(
    private val vilkårperiodeRepository: VilkårperiodeRepository,
    private val vedtakRepository: VedtakRepository,
    private val tilsynBarnUtgiftService: TilsynBarnUtgiftService,
) {
    /**
     * Dersom behandling er en revurdering beregnes perioder fra og med måneden for revurderFra
     * Ellers beregnes perioder for hele perioden som man har stønadsperioder og utgifter
     */
    fun beregnAktuellePerioder(
        behandling: Saksbehandling,
        typeVedtak: TypeVedtak,
        vedtaksperiodeBeregningsgrunnlag: List<VedtaksperiodeBeregningsgrunnlag>,
    ): List<BeregningsresultatForMåned> {
        val utgifterPerBarn = tilsynBarnUtgiftService.hentUtgifterTilBeregning(behandling.id)

        val aktiviteter = finnAktiviteter(behandling.id)

        validerPerioderForInnvilgelse(vedtaksperiodeBeregningsgrunnlag, aktiviteter, utgifterPerBarn, typeVedtak, behandling.revurderFra)

        val beregningsgrunnlag =
            BeregningsgrunnlagUtilsFelles
                .lagBeregningsgrunnlagPerMåned(vedtaksperiodeBeregningsgrunnlag, aktiviteter, utgifterPerBarn)
                .brukBeregningsgrunnlagFraOgMedRevurderFra(behandling.revurderFra)
        return beregn(beregningsgrunnlag)
    }

    private fun beregn(beregningsgrunnlag: List<Beregningsgrunnlag>): List<BeregningsresultatForMåned> =
        beregningsgrunnlag.map {
            val dagsats = beregnDagsats(it)
            val beløpsperioder = lagBeløpsperioder(dagsats, it)

            BeregningsresultatForMåned(
                dagsats = dagsats,
                månedsbeløp = beløpsperioder.sumOf { it.beløp },
                grunnlag = it,
                beløpsperioder = beløpsperioder,
            )
        }

    private fun lagBeløpsperioder(
        dagsats: BigDecimal,
        it: Beregningsgrunnlag,
    ): List<Beløpsperiode> =
        it.stønadsperioderGrunnlag.map {
            // Datoer som treffer helger må endres til neste mandag fordi andeler med type dagsats betales ikke ut i helger
            val dato = it.stønadsperiode.fom.datoEllerNesteMandagHvisLørdagEllerSøndag()
            Beløpsperiode(
                dato = dato,
                beløp = beregnBeløp(dagsats, it.antallDager),
                målgruppe = it.stønadsperiode.målgruppe,
            )
        }

    // Kopiert fra V1
    fun finnRelevantePerioderFraForrigeVedtak(behandling: Saksbehandling): List<BeregningsresultatForMåned> =
        behandling.forrigeBehandlingId?.let { forrigeBehandlingId ->
            val beregningsresultat =
                vedtakRepository
                    .findByIdOrThrow(forrigeBehandlingId)
                    .withTypeOrThrow<VedtakTilsynBarn>()
                    .data
                    .beregningsresultat()
                    ?: error("Finner ikke beregningsresultat på vedtak for behandling=$forrigeBehandlingId")
            val revurderFraMåned = behandling.revurderFra?.toYearMonth() ?: YEAR_MONTH_MIN

            beregningsresultat.perioder.filter { it.grunnlag.måned < revurderFraMåned }
        } ?: emptyList()

    fun finnAktiviteter(behandlingId: BehandlingId): List<Aktivitet> =
        vilkårperiodeRepository
            .findByBehandlingIdAndResultat(behandlingId, ResultatVilkårperiode.OPPFYLT)
            .tilAktiviteter()

    // Kopiert fra V1

    /**
     * Divide trenger en scale som gir antall desimaler på resultatet fra divideringen
     * Sånn sett blir `setScale(2, RoundingMode.HALF_UP)` etteråt unødvendig
     * Tar likevel med den for å gjøre det tydelig at resultatet skal maks ha 2 desimaler
     */
    fun beregnDagsats(grunnlag: Beregningsgrunnlag): BigDecimal {
        val utgifter = grunnlag.utgifterTotal.toBigDecimal()
        val utgifterSomDekkes =
            utgifter
                .multiply(DEKNINGSGRAD_TILSYN_BARN)
                .setScale(0, RoundingMode.HALF_UP)
                .toInt()
        val satsjusterteUtgifter = minOf(utgifterSomDekkes, grunnlag.makssats).toBigDecimal()
        return satsjusterteUtgifter
            .divide(SNITT_ANTALL_VIRKEDAGER_PER_MÅNED, 2, RoundingMode.HALF_UP)
            .setScale(2, RoundingMode.HALF_UP)
    }

    // Kopiert fra V1
    fun beregnBeløp(
        dagsats: BigDecimal,
        antallDager: Int,
    ) = dagsats
        .multiply(antallDager.toBigDecimal())
        .setScale(0, RoundingMode.HALF_UP)
        .toInt()
}
