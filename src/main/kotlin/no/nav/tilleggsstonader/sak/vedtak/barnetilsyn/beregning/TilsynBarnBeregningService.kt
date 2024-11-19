package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.util.YEAR_MONTH_MIN
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.tilAktiviteterPerMånedPerType
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.tilDagerPerUke
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.tilUke
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.tilÅrMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.StønadsperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.tilAktiviteter
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.tilSortertGrunnlagStønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
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
private val DEKNINGSGRAD = BigDecimal("0.64")
private val SNITT_ANTALL_VIRKEDAGER_PER_MÅNED = BigDecimal("21.67")

@Service
class TilsynBarnBeregningService(
    private val stønadsperiodeRepository: StønadsperiodeRepository,
    private val vilkårperiodeRepository: VilkårperiodeRepository,
    private val tilsynBarnUtgiftService: TilsynBarnUtgiftService,
    private val repository: VedtakRepository,
    private val unleashService: UnleashService,
) {

    fun beregn(behandling: Saksbehandling): BeregningsresultatTilsynBarn {
        val perioder = beregnAktuellePerioder(behandling)
        val relevantePerioderFraForrigeVedtak = finnRelevantePerioderFraForrigeVedtak(behandling)
        return BeregningsresultatTilsynBarn(relevantePerioderFraForrigeVedtak + perioder)
    }

    /**
     * Dersom behandling er en revurdering beregnes perioder fra og med måneden for revurderFra
     * Ellers beregnes perioder for hele perioden som man har stønadsperioder og utgifter
     */
    private fun beregnAktuellePerioder(behandling: Saksbehandling): List<BeregningsresultatForMåned> {
        val utgifterPerBarn = tilsynBarnUtgiftService.hentUtgifterTilBeregning(behandling.id)
        val stønadsperioder = stønadsperiodeRepository.findAllByBehandlingId(behandling.id)
            .tilSortertGrunnlagStønadsperiode()
            .splitFraRevurderFra(behandling.revurderFra)

        val aktiviteter = finnAktiviteter(behandling.id)

        validerPerioder(stønadsperioder, aktiviteter, utgifterPerBarn)

        val beregningsgrunnlag = lagBeregningsgrunnlagPerMåned(stønadsperioder, aktiviteter, utgifterPerBarn)
            .brukPerioderFraOgMedRevurderFra(behandling.revurderFra)
        return beregn(beregningsgrunnlag)
    }

    private fun finnRelevantePerioderFraForrigeVedtak(behandling: Saksbehandling): List<BeregningsresultatForMåned> {
        return behandling.forrigeBehandlingId?.let { forrigeBehandlingId ->
            val beregningsresultat = repository.findByIdOrThrow(forrigeBehandlingId).beregningsresultat
                ?: error("Finner ikke beregningsresultat på vedtak for behandling=$forrigeBehandlingId")
            val revurderFraMåned = behandling.revurderFra?.toYearMonth() ?: YEAR_MONTH_MIN

            beregningsresultat.perioder.filter { it.grunnlag.måned < revurderFraMåned }
        } ?: emptyList()
    }

    private fun beregn(beregningsgrunnlag: List<Beregningsgrunnlag>): List<BeregningsresultatForMåned> {
        return beregningsgrunnlag.map {
            val dagsats = beregnDagsats(it)
            val beløpsperioder = lagBeløpsperioder(dagsats, it)

            BeregningsresultatForMåned(
                dagsats = dagsats,
                månedsbeløp = beløpsperioder.sumOf { it.beløp },
                grunnlag = it,
                beløpsperioder = beløpsperioder,
            )
        }
    }

    private fun lagBeløpsperioder(dagsats: BigDecimal, it: Beregningsgrunnlag): List<Beløpsperiode> {
        return it.stønadsperioderGrunnlag.map {
            // Datoer som treffer helger må endres til neste mandag fordi andeler med type dagsats betales ikke ut i helger
            val dato = it.stønadsperiode.fom.datoEllerNesteMandagHvisLørdagEllerSøndag()
            Beløpsperiode(
                dato = dato,
                beløp = beregnBeløp(dagsats, it.antallDager),
                målgruppe = it.stønadsperiode.målgruppe,
            )
        }
    }

    private fun beregnBeløp(dagsats: BigDecimal, antallDager: Int) =
        dagsats.multiply(antallDager.toBigDecimal())
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()

    /**
     * Divide trenger en scale som gir antall desimaler på resultatet fra divideringen
     * Sånn sett blir `setScale(2, RoundingMode.HALF_UP)` etteråt unødvendig
     * Tar likevel med den for å gjøre det tydelig at resultatet skal maks ha 2 desimaler
     */
    private fun beregnDagsats(grunnlag: Beregningsgrunnlag): BigDecimal {
        val utgifter = grunnlag.utgifterTotal.toBigDecimal()
        val utgifterSomDekkes = utgifter.multiply(DEKNINGSGRAD)
            .setScale(0, RoundingMode.HALF_UP)
            .toInt()
        val satsjusterteUtgifter = minOf(utgifterSomDekkes, grunnlag.makssats).toBigDecimal()
        return satsjusterteUtgifter
            .divide(SNITT_ANTALL_VIRKEDAGER_PER_MÅNED, 2, RoundingMode.HALF_UP)
            .setScale(2, RoundingMode.HALF_UP)
    }

    private fun lagBeregningsgrunnlagPerMåned(
        stønadsperioder: List<Stønadsperiode>,
        aktiviteter: List<Aktivitet>,
        utgifterPerBarn: Map<BarnId, List<UtgiftBeregning>>,
    ): List<Beregningsgrunnlag> {
        val stønadsperioderPerMåned = stønadsperioder.tilÅrMåned()
        val utgifterPerMåned = utgifterPerBarn.tilÅrMåned()
        val aktiviteterPerMånedPerType = aktiviteter.tilAktiviteterPerMånedPerType()

        return stønadsperioderPerMåned.entries.mapNotNull { (måned, stønadsperioder) ->
            val aktiviteterForMåned = aktiviteterPerMånedPerType[måned] ?: error("Ingen aktiviteter for måned $måned")
            utgifterPerMåned[måned]?.let { utgifter ->
                val antallBarn = utgifter.map { it.barnId }.toSet().size
                val makssats = finnMakssats(måned, antallBarn)
                val stønadsperioderGrunnlag = finnStønadsperioderMedAktiviteter(stønadsperioder, aktiviteterForMåned)
                Beregningsgrunnlag(
                    måned = måned,
                    makssats = makssats,
                    stønadsperioderGrunnlag = stønadsperioderGrunnlag,
                    utgifter = utgifter,
                    utgifterTotal = utgifter.sumOf { it.utgift },
                    antallBarn = antallBarn,
                )
            }
        }
    }

    private fun finnStønadsperioderMedAktiviteter(
        stønadsperioder: List<Stønadsperiode>,
        aktiviteter: Map<AktivitetType, List<Aktivitet>>,
    ): List<StønadsperiodeGrunnlag> {
        val aktiviteterPerUke = aktiviteter.map { it.key to it.value.tilDagerPerUke() }.toMap()

        return stønadsperioder.map { stønadsperiode ->
            val relevanteAktiviteter = finnAktiviteterForStønadsperiode(stønadsperiode, aktiviteter)

            StønadsperiodeGrunnlag(
                stønadsperiode = stønadsperiode,
                aktiviteter = relevanteAktiviteter,
                antallDager = antallDager(stønadsperiode, aktiviteterPerUke),
            )
        }
    }

    private fun finnAktiviteterForStønadsperiode(
        stønadsperiode: Stønadsperiode,
        aktiviteter: Map<AktivitetType, List<Aktivitet>>,
    ): List<Aktivitet> {
        return aktiviteter[stønadsperiode.aktivitet]?.filter { it.overlapper(stønadsperiode) }
            ?: error("Finnes ingen aktiviteter av type ${stønadsperiode.aktivitet} som passer med stønadsperiode med fom=${stønadsperiode.fom} og tom=${stønadsperiode.tom}")
    }

    private fun antallDager(
        stønadsperiode: Stønadsperiode,
        aktiviteterPerType: Map<AktivitetType, Map<Uke, List<PeriodeMedDager>>>,
    ): Int {
        val stønadsperioderUker = stønadsperiode.tilUke()
        val aktiviteterPerUke = aktiviteterPerType[stønadsperiode.aktivitet]
            ?: error("Finner ikke aktiviteter for ${stønadsperiode.aktivitet}")

        return stønadsperioderUker.map { (uke, periode) ->
            val aktiviteterForUke = aktiviteterPerUke[uke]
                ?: error("Ingen aktivitet i uke fom=${uke.fom} og tom=${uke.tom}")

            beregnAntallAktivitetsdagerForUke(periode, aktiviteterForUke)
        }.sum()
    }

    /**
     * Beregner antall dager per uke som kan brukes
     * Hvis antall dager fra stønadsperiode er 1, så kan man maks bruke 1 dag fra aktiviteter
     * Hvis antall dager fra stønadsperiode er 5, men aktiviteter kun har 2 dager så kan man kun bruke 2 dager
     */
    private fun beregnAntallAktivitetsdagerForUke(
        stønadsperiode: PeriodeMedDager,
        aktiviteter: List<PeriodeMedDager>,
    ): Int {
        return aktiviteter.filter { it.overlapper(stønadsperiode) }.fold(0) { acc, aktivitet ->
            // Tilgjengelige dager i uke i overlapp mellom stønadsperiode og aktivitet
            val antallTilgjengeligeDager = minOf(stønadsperiode.antallDager, aktivitet.antallDager)

            trekkFraBrukteDager(stønadsperiode, aktivitet, antallTilgjengeligeDager)

            acc + antallTilgjengeligeDager
        }
    }

    /**
     * Skal ikke kunne bruke en dager fra aktivitet eller stønadsperiode flere ganger.
     * Trekker fra tilgjengelige dager fra antallDager
     * Dersom stønadsperioden har 2 dager, og aktiviten 3 dager, så skal man kun totalt kunne bruke 2 dager
     * Dersom stønadsperioden har 3 dager, og aktiviten 2 dager, så skal man kun totalt kunne bruke 2 dager
     */
    private fun trekkFraBrukteDager(
        stønadsperiode: PeriodeMedDager,
        aktivitet: PeriodeMedDager,
        antallTilgjengeligeDager: Int,
    ) {
        aktivitet.antallDager -= antallTilgjengeligeDager
        stønadsperiode.antallDager -= antallTilgjengeligeDager
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
    private fun List<Beregningsgrunnlag>.brukPerioderFraOgMedRevurderFra(
        revurderFra: LocalDate?,
    ): List<Beregningsgrunnlag> {
        val revurderFraMåned = revurderFra?.toYearMonth() ?: return this

        return this.filter { it.måned >= revurderFraMåned }
    }

    /**
     * Dersom man har en lang stønadsperiode for 1.1 - 31.1 så skal den splittes opp fra revurderFra sånn at man får 2 perioder
     * Eks for revurderFra=15.1 så får man 1.1 - 14.1 og 15.1 - 31.1
     * Dette for å kunne filtrere vekk perioder som begynner før revurderFra og beregne beløp som skal utbetales i gitt måned
     */
    private fun List<Stønadsperiode>.splitFraRevurderFra(revurderFra: LocalDate?): List<Stønadsperiode> {
        if (revurderFra == null) return this
        return this.flatMap {
            if (it.fom < revurderFra && revurderFra <= it.tom) {
                listOf(
                    it.copy(tom = revurderFra.minusDays(1)),
                    it.copy(fom = revurderFra),
                )
            } else {
                listOf(it)
            }
        }
    }

    private fun finnAktiviteter(behandlingId: BehandlingId): List<Aktivitet> {
        return vilkårperiodeRepository.findByBehandlingIdAndResultat(behandlingId, ResultatVilkårperiode.OPPFYLT)
            .tilAktiviteter()
    }

    private fun validerPerioder(
        stønadsperioder: List<Stønadsperiode>,
        aktiviteter: List<Aktivitet>,
        utgifter: Map<BarnId, List<UtgiftBeregning>>,
    ) {
        if (unleashService.isEnabled(Toggle.OPPHØR_IGNORER_VALIDERING)) {
            return
        }
        validerStønadsperioder(stønadsperioder)
        validerAktiviteter(aktiviteter)
        validerUtgifter(utgifter)
    }

    private fun validerStønadsperioder(stønadsperioder: List<Stønadsperiode>) {
        brukerfeilHvis(stønadsperioder.isEmpty()) {
            "Kan ikke innvilge når det ikke finnes noen stønadsperioder"
        }
    }

    private fun validerAktiviteter(aktiviteter: List<Aktivitet>) {
        feilHvis(aktiviteter.isEmpty()) {
            "Aktiviteter mangler"
        }
    }

    private fun validerUtgifter(utgifter: Map<BarnId, List<UtgiftBeregning>>) {
        feilHvis(utgifter.values.flatten().isEmpty()) {
            "Utgiftsperioder mangler"
        }
        utgifter.entries.forEach { (_, utgifterForBarn) ->
            feilHvis(utgifterForBarn.overlapper()) {
                "Utgiftsperioder overlapper"
            }

            val ikkePositivUtgift = utgifterForBarn.firstOrNull { it.utgift < 0 }?.utgift
            feilHvis(ikkePositivUtgift != null) {
                "Utgiftsperioder inneholder ugyldig utgift: $ikkePositivUtgift"
            }
        }
    }
}
