package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV2

import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.YEAR_MONTH_MIN
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.DEKNINGSGRAD_TILSYN_BARN
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnBeregningValideringUtil.validerPerioderForInnvilgelseV2
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnUtgiftService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.splitFraRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.tilAktiviteter
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.beregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

// Kopiert fra V1
private val SNITT_ANTALL_VIRKEDAGER_PER_MÅNED = BigDecimal("21.67")

class TilsynBarnBeregningServiceV2(
    private val tilsynBarnUtgiftService: TilsynBarnUtgiftService,
    private val vilkårperiodeRepository: VilkårperiodeRepository,
    private val vedtakRepository: VedtakRepository,
) {
    fun beregn(
        vedtaksperioder: List<VedtaksperiodeDto>,
        behandling: Behandling,
        typeVedtak: TypeVedtak,
    ): BeregningsresultatTilsynBarn {
        // Valider ingen overlapp mellom vedtaksperioder

        val perioder = beregnAktuellePerioder(behandling, vedtaksperioder, typeVedtak)
        val relevantePerioderFraForrigeVedtak = finnRelevantePerioderFraForrigeVedtak(behandling)
        return BeregningsresultatTilsynBarn(relevantePerioderFraForrigeVedtak + perioder)
    }

    private fun beregnAktuellePerioder(
        behandling: Behandling,
        vedtaksperioder: List<VedtaksperiodeDto>,
        typeVedtak: TypeVedtak,
    ): List<BeregningsresultatForMåned> {
        val utgifterPerBarn = tilsynBarnUtgiftService.hentUtgifterTilBeregning(behandling.id)
        val aktiviteter = finnAktiviteter(behandling.id)
        val vedtaksperioderEtterRevurderFra = vedtaksperioder.sorted().splitFraRevurderFra(behandling.revurderFra)

        validerPerioderForInnvilgelseV2(
            vedtaksperioder = vedtaksperioderEtterRevurderFra,
            aktiviteter = aktiviteter,
            utgifter = utgifterPerBarn,
            typeVedtak = typeVedtak,
            revurderFra = behandling.revurderFra,
        )

        val beregningsgrunnlag =
            BeregingsgrunnlagUtilsV2
                .lagBeregningsgrunnlagPerMåned(
                    vedtaksperioder = vedtaksperioderEtterRevurderFra,
                    aktiviteter = aktiviteter,
                    utgifterPerBarn = utgifterPerBarn,
                ).brukPerioderFraOgMedRevurderFra(behandling.revurderFra)
        return beregn(beregningsgrunnlag)
    }

    // Kopiert fra V1
    private fun finnRelevantePerioderFraForrigeVedtak(behandling: Behandling): List<BeregningsresultatForMåned> =
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

    // Kopiert fra V1

    /**
     * Dersom man har satt revurderFra så skal man kun beregne perioder fra og med den måneden
     * Hvis vi eks innvilget 1000kr for 1-31 august, så mappes hele beløpet til 1 august.
     * Dvs det lages en andel som har fom-tom 1-1 aug
     * Når man revurderer fra midten på måneden og eks skal endre målgruppe eller aktivitetsdager,
     * så har man allerede utbetalt 500kr for 1-14 august, men hele beløpet er ført på 1 aug.
     * For at beregningen då skal bli riktig må man ha med grunnlaget til hele måneden og beregne det på nytt, sånn at man får en ny periode som er
     * 1-14 aug, 500kr, 15-30 aug 700kr.
     */
    private fun List<Beregningsgrunnlag>.brukPerioderFraOgMedRevurderFra(revurderFra: LocalDate?): List<Beregningsgrunnlag> {
        val revurderFraMåned = revurderFra?.toYearMonth() ?: return this

        return this.filter { it.måned >= revurderFraMåned }
    }

    // Kopiert fra V1
    private fun finnAktiviteter(behandlingId: BehandlingId): List<Aktivitet> =
        vilkårperiodeRepository
            .findByBehandlingIdAndResultat(behandlingId, ResultatVilkårperiode.OPPFYLT)
            .tilAktiviteter()

    // Kopiert fra V1
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

    // Kopiert logikk fra V1, oppdatert navngivning
    private fun lagBeløpsperioder(
        dagsats: BigDecimal,
        beregningsgrunnlag: Beregningsgrunnlag,
    ): List<Beløpsperiode> =
        beregningsgrunnlag.vedtaksperioderGrunnlag.map {
            val dato = it.fom.datoEllerNesteMandagHvisLørdagEllerSøndag()
            Beløpsperiode(
                dato = dato,
                beløp = beregnBeløp(dagsats, it.antallAktivitetsDager),
                målgruppe = it.målgruppeType,
            )
        }

    // Kopiert fra V1

    /**
     * Divide trenger en scale som gir antall desimaler på resultatet fra divideringen
     * Sånn sett blir `setScale(2, RoundingMode.HALF_UP)` etteråt unødvendig
     * Tar likevel med den for å gjøre det tydelig at resultatet skal maks ha 2 desimaler
     */
    private fun beregnDagsats(grunnlag: Beregningsgrunnlag): BigDecimal {
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
    private fun beregnBeløp(
        dagsats: BigDecimal,
        antallDager: Int,
    ) = dagsats
        .multiply(antallDager.toBigDecimal())
        .setScale(0, RoundingMode.HALF_UP)
        .toInt()
}
