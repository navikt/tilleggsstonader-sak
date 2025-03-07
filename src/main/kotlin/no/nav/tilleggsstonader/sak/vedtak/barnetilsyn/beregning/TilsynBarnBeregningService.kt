package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.YEAR_MONTH_MIN
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnBeregningValideringUtil.validerPerioderForInnvilgelse
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.splitFraRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.tilAktiviteterPerMånedPerType
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.tilDagerPerUke
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.tilUke
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.tilÅrMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.tilAktiviteter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.tilVedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.tilVedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * Stønaden dekker 64% av utgifterne til barnetilsyn
 */
val DEKNINGSGRAD_TILSYN_BARN = BigDecimal("0.64")
private val SNITT_ANTALL_VIRKEDAGER_PER_MÅNED = BigDecimal("21.67")

@Service
class TilsynBarnBeregningService(
    private val stønadsperiodeRepository: StønadsperiodeRepository,
    private val vilkårperiodeRepository: VilkårperiodeRepository,
    private val vedtakRepository: VedtakRepository,
    private val tilsynBarnUtgiftService: TilsynBarnUtgiftService,
    private val tilsynBarnVedtaksperiodeValidingerService: TilsynBarnVedtaksperiodeValidingerService,
) {
    fun beregn(
        behandling: Saksbehandling,
        typeVedtak: TypeVedtak,
    ): BeregningsresultatTilsynBarn {
        feilHvis(typeVedtak == TypeVedtak.AVSLAG) {
            "Skal ikke beregne for avslag"
        }

        val vedtaksperioder =
            stønadsperiodeRepository
                .findAllByBehandlingId(behandling.id)
                .tilVedtaksperiodeBeregning()
                .sorted()
                .splitFraRevurderFra(behandling.revurderFra)
        val perioder = beregnAktuellePerioder(behandling, typeVedtak, vedtaksperioder)
        val relevantePerioderFraForrigeVedtak =
            finnRelevantePerioderFraForrigeVedtak(behandling)
        return BeregningsresultatTilsynBarn(relevantePerioderFraForrigeVedtak + perioder)
    }

    fun beregnV2(
        vedtaksperioder: List<Vedtaksperiode>,
        behandling: Saksbehandling,
        typeVedtak: TypeVedtak,
    ): BeregningsresultatTilsynBarn {
        feilHvis(typeVedtak == TypeVedtak.AVSLAG) {
            "Skal ikke beregne for avslag"
        }

        val utgifterPerBarn = tilsynBarnUtgiftService.hentUtgifterTilBeregning(behandling.id)

        tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
            vedtaksperioder = vedtaksperioder,
            behandling = behandling,
            utgifter = utgifterPerBarn,
            typeVedtak = typeVedtak,
        )

        val vedtaksperioderBeregning =
            vedtaksperioder.tilVedtaksperiodeBeregning().sorted().splitFraRevurderFra(behandling.revurderFra)

        val perioder = beregnAktuellePerioder(behandling, typeVedtak, vedtaksperioderBeregning)
        val relevantePerioderFraForrigeVedtak =
            finnRelevantePerioderFraForrigeVedtak(behandling)
        return BeregningsresultatTilsynBarn(relevantePerioderFraForrigeVedtak + perioder)
    }

    /**
     * Dersom behandling er en revurdering beregnes perioder fra og med måneden for revurderFra
     * Ellers beregnes perioder for hele perioden som man har stønadsperioder og utgifter
     */
    private fun beregnAktuellePerioder(
        behandling: Saksbehandling,
        typeVedtak: TypeVedtak,
        vedtaksperioder: List<VedtaksperiodeBeregning>,
    ): List<BeregningsresultatForMåned> {
        val utgifterPerBarn = tilsynBarnUtgiftService.hentUtgifterTilBeregning(behandling.id)

        val aktiviteter = finnAktiviteter(behandling.id)

        validerPerioderForInnvilgelse(vedtaksperioder, utgifterPerBarn, typeVedtak)

        val beregningsgrunnlag =
            lagBeregningsgrunnlagPerMåned(vedtaksperioder, aktiviteter, utgifterPerBarn)
                .brukPerioderFraOgMedRevurderFra(behandling.revurderFra)
        return beregn(beregningsgrunnlag)
    }

    private fun finnRelevantePerioderFraForrigeVedtak(behandling: Saksbehandling): List<BeregningsresultatForMåned> =
        behandling.forrigeBehandlingId?.let { forrigeBehandlingId ->
            val beregningsresultat =
                vedtakRepository
                    .findByIdOrThrow(forrigeBehandlingId)
                    .withTypeOrThrow<InnvilgelseEllerOpphørTilsynBarn>()
                    .data
                    .beregningsresultat
            val revurderFraMåned = behandling.revurderFra?.toYearMonth() ?: YEAR_MONTH_MIN

            beregningsresultat.perioder.filter { it.grunnlag.måned < revurderFraMåned }
        } ?: emptyList()

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
        it.vedtaksperiodeGrunnlag.map {
            // Datoer som treffer helger må endres til neste mandag fordi andeler med type dagsats betales ikke ut i helger
            val dato = it.vedtaksperiode.fom.datoEllerNesteMandagHvisLørdagEllerSøndag()
            Beløpsperiode(
                dato = dato,
                beløp = beregnBeløp(dagsats, it.antallDager),
                målgruppe = it.vedtaksperiode.målgruppe,
            )
        }

    private fun beregnBeløp(
        dagsats: BigDecimal,
        antallDager: Int,
    ) = dagsats
        .multiply(antallDager.toBigDecimal())
        .setScale(0, RoundingMode.HALF_UP)
        .toInt()

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

    private fun lagBeregningsgrunnlagPerMåned(
        vedtaksperioder: List<VedtaksperiodeBeregning>,
        aktiviteter: List<Aktivitet>,
        utgifterPerBarn: Map<BarnId, List<UtgiftBeregning>>,
    ): List<Beregningsgrunnlag> {
        val vedtaksperioderPerMåned = vedtaksperioder.tilÅrMåned()
        val utgifterPerMåned = utgifterPerBarn.tilÅrMåned()
        val aktiviteterPerMånedPerType = aktiviteter.tilAktiviteterPerMånedPerType()

        return vedtaksperioderPerMåned.entries.mapNotNull { (måned, vedtaksperiode) ->
            val aktiviteterForMåned = aktiviteterPerMånedPerType[måned] ?: error("Ingen aktiviteter for måned $måned")
            utgifterPerMåned[måned]?.let { utgifter ->
                val antallBarn = utgifter.map { it.barnId }.toSet().size
                val makssats = finnMakssats(måned, antallBarn)
                val vedtaksperiodeGrunnlag = finnVedtaksperiodeGrunnlag(vedtaksperiode, aktiviteterForMåned)
                Beregningsgrunnlag(
                    måned = måned,
                    makssats = makssats,
                    vedtaksperiodeGrunnlag = vedtaksperiodeGrunnlag,
                    utgifter = utgifter,
                    utgifterTotal = utgifter.sumOf { it.utgift },
                    antallBarn = antallBarn,
                )
            }
        }
    }

    private fun finnVedtaksperiodeGrunnlag(
        vedtaksperioder: List<VedtaksperiodeBeregning>,
        aktiviteter: Map<AktivitetType, List<Aktivitet>>,
    ): List<VedtaksperiodeGrunnlag> {
        val aktiviteterPerUke = aktiviteter.map { it.key to it.value.tilDagerPerUke() }.toMap()

        return vedtaksperioder.map { vedtaksperiode ->
            val relevanteAktiviteter = finnAktiviteterForVedtaksperiode(vedtaksperiode, aktiviteter)

            VedtaksperiodeGrunnlag(
                vedtaksperiode = vedtaksperiode,
                aktiviteter = relevanteAktiviteter,
                antallDager = antallDager(vedtaksperiode, aktiviteterPerUke),
            )
        }
    }

    private fun finnAktiviteterForVedtaksperiode(
        vedtaksperiode: VedtaksperiodeBeregning,
        aktiviteter: Map<AktivitetType, List<Aktivitet>>,
    ): List<Aktivitet> =
        aktiviteter[vedtaksperiode.aktivitet]?.filter { it.overlapper(vedtaksperiode) }
            ?: error(
                "Finnes ingen aktiviteter av type ${vedtaksperiode.aktivitet} som passer med vedtaksperiode med fom=${vedtaksperiode.fom} og tom=${vedtaksperiode.tom}",
            )

    private fun antallDager(
        vedtaksperiode: VedtaksperiodeBeregning,
        aktiviteterPerType: Map<AktivitetType, Map<Uke, List<PeriodeMedDager>>>,
    ): Int {
        val stønadsperioderUker = vedtaksperiode.tilUke()
        val aktiviteterPerUke =
            aktiviteterPerType[vedtaksperiode.aktivitet]
                ?: error("Finner ikke aktiviteter for ${vedtaksperiode.aktivitet}")

        return stønadsperioderUker
            .map { (uke, periode) ->
                val aktiviteterForUke =
                    aktiviteterPerUke[uke]
                        ?: error("Ingen aktivitet i uke fom=${uke.fom} og tom=${uke.tom}")

                BeregningsgrunnlagUtils.beregnAntallAktivitetsdagerForUke(periode, aktiviteterForUke)
            }.sum()
    }

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

    private fun finnAktiviteter(behandlingId: BehandlingId): List<Aktivitet> =
        vilkårperiodeRepository
            .findByBehandlingIdAndResultat(behandlingId, ResultatVilkårperiode.OPPFYLT)
            .tilAktiviteter()
}
