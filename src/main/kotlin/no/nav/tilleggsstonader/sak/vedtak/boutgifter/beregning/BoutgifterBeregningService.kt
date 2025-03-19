package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterBeregnUtil.grupperVedtaksperioderPerLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.UtgifterValideringUtil.validerUtgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.splitFraRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.domain.tilVedtaksperiodeBeregning
import org.springframework.stereotype.Service

@Service
class BoutgifterBeregningService(
    private val boutgifterUtgiftService: BoutgifterUtgiftService,
//    private val vedtaksperiodeValideringService: BoutgifterVedtaksperiodeValideringService,
    private val vedtakRepository: VedtakRepository,
) {
    /**
     * Kjente begrensninger i beregningen (programmet kaster feil dersom antagelsene ikke stemmer):
     * - Vi antar at satsen ikke endrer seg i vedtaksperioden (TODO: SJEKK AT DET STEMMER)
     * - Vi antar at det er overlapp mellom utgift og vedtaksperiode
     * - Utgiftene krysser ikke overgangen fra én løpende måned til en annen
     * - Det finnes bare én type målgruppe og aktivitet innenfor hver løpende måned
     */
    fun beregn(
        behandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsresultatBoutgifter {
//        val forrigeVedtak = hentForrigeVedtak(behandling)
        // TODO: Deal med revurderFra-datoen

//        vedtaksperiodeValideringService.validerVedtaksperioder(
//            vedtaksperioder = vedtaksperioder,
//            stønadsperioder = stønadsperioder,
//            behandlingId = behandling.id,
//        )

        val utgifterPerVilkårtype = boutgifterUtgiftService.hentUtgifterTilBeregning(behandling.id)
        validerUtgifter(utgifterPerVilkårtype)

        val vedtaksperioderBeregning =
            vedtaksperioder.tilVedtaksperiodeBeregning().sorted().splitFraRevurderFra(behandling.revurderFra)

        validerUtgifterErInnenforVedtaksperiodene(utgifterPerVilkårtype, vedtaksperioderBeregning)

        val beregningsresultat =
            beregnAktuellePerioder(
                vedtaksperioder = vedtaksperioderBeregning,
                utgifter = utgifterPerVilkårtype,
            )

//        return if (forrigeVedtak != null) {
//            settSammenGamleOgNyePerioder(behandling, beregningsresultat, forrigeVedtak)
//        } else {
//            BeregningsresultatBoutgifter(beregningsresultat)
//        }
        return BeregningsresultatBoutgifter(beregningsresultat)
    }

    private fun beregnAktuellePerioder(
        vedtaksperioder: List<VedtaksperiodeBeregning>,
        utgifter: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>,
    ): List<BeregningsresultatForLøpendeMåned> =
        vedtaksperioder
            .sorted()
            .grupperVedtaksperioderPerLøpendeMåned()
            .map { UtbetalingPeriode(it) }
            .validerIngenUtgifterKrysserUtbetalingsperioder(utgifter)
            .map {
                BeregningsresultatForLøpendeMåned(
                    grunnlag = lagBeregningsGrunnlag(periode = it, utgifter = utgifter),
                )
            }

//    fun beregnForOpphør(
//        behandling: Saksbehandling,
//        avkortetVedtaksperioder: List<Vedtaksperiode>,
//    ): BeregningsresultatBoutgifter {
//        feilHvis(behandling.forrigeIverksatteBehandlingId == null) {
//            "Opphør er et ugyldig vedtaksresultat fordi behandlingen er en førstegangsbehandling"
//        }
//        feilHvis(behandling.revurderFra == null) {
//            "revurderFra-dato er påkrevd for opphør"
//        }
//        val forrigeVedtak = hentVedtak(behandling.forrigeIverksatteBehandlingId)
//        val avkortetBeregningsresultat = avkortBeregningsresultatVedOpphør(forrigeVedtak, behandling.revurderFra)
//
//        return beregningsresultatForOpphør(
//            behandling = behandling,
//            avkortetBeregningsresultat = avkortetBeregningsresultat,
//            avkortetVedtaksperioder = avkortetVedtaksperioder,
//        )
//    }

//    /**
//     * Hvis man har avkortet siste måneden må man reberegne den i tilfelle % på aktiviteter har endret seg
//     * Eks at man hadde 2 aktiviteter, 50 og 100% som då gir 100%.
//     * Etter opphør så har man kun 50% og då trenger å omberegne perioden
//     */
//    private fun beregningsresultatForOpphør(
//        behandling: Saksbehandling,
//        avkortetBeregningsresultat: AvkortResult<BeregningsresultatForMåned>,
//        avkortetVedtaksperioder: List<Vedtaksperiode>,
//    ): BeregningsresultatBoutgifter {
//        if (!avkortetBeregningsresultat.harAvkortetPeriode) {
//            return BeregningsresultatBoutgifter(avkortetBeregningsresultat.perioder)
//        }
//
//        return beregningsresultatOpphørMedReberegnetPeriode(
//            behandling = behandling,
//            avkortetBeregningsresultat = avkortetBeregningsresultat,
//            avkortetVedtaksperioder = avkortetVedtaksperioder,
//        )
//    }

//    private fun beregningsresultatOpphørMedReberegnetPeriode(
//        behandling: Saksbehandling,
//        avkortetBeregningsresultat: AvkortResult<BeregningsresultatForMåned>,
//        avkortetVedtaksperioder: List<Vedtaksperiode>,
//    ): BeregningsresultatBoutgifter {
//        val perioderSomBeholdes = avkortetBeregningsresultat.perioder.dropLast(1)
//        val periodeTilReberegning = avkortetBeregningsresultat.perioder.last()
//
//        val reberegnedePerioderKorrigertUtbetalingsdato =
//            reberegnPerioderForOpphør(
//                behandling = behandling,
//                avkortetVedtaksperioder = avkortetVedtaksperioder,
//                beregningsresultatTilReberegning = periodeTilReberegning,
//            )
//
//        val nyePerioder =
//            (perioderSomBeholdes + reberegnedePerioderKorrigertUtbetalingsdato)
//                .map { it.markerSomDelAvTidligereUtbetaling(delAvTidligereUtbetaling = false) }
//        return BeregningsresultatBoutgifter(nyePerioder)
//    }

//    /**
//     * Reberegner siste perioden då den er avkortet og ev skal få annet beløp utbetalt
//     * Korrigerer utbetalingsdatoet då vedtaksperioden sånn at andelen som sen genereres for det utbetales på likt dato
//     */
//    private fun reberegnPerioderForOpphør(
//        behandling: Saksbehandling,
//        avkortetVedtaksperioder: List<Vedtaksperiode>,
//        beregningsresultatTilReberegning: BeregningsresultatForMåned,
//    ): List<BeregningsresultatForMåned> {
//        val stønadsperioder = hentStønadsperioder(behandling.id)
//        val vedtaksperioderSomOmregnes =
//            vedtaksperioderInnenforLøpendeMåned(avkortetVedtaksperioder, beregningsresultatTilReberegning)
//
//        val reberegnedePerioder = beregn(behandling, vedtaksperioderSomOmregnes, stønadsperioder)
//        feilHvisIkke(reberegnedePerioder.size <= 1) {
//            "Når vi reberegner vedtaksperioder innenfor en måned burde vi få maks 1 reberegnet periode, faktiskAntall=${reberegnedePerioder.size}"
//        }
//
//        return reberegnedePerioder.map {
//            it.medKorrigertUtbetalingsdato(beregningsresultatTilReberegning.grunnlag.utbetalingsdato)
//        }
//    }

//    /**
//     * Slår sammen perioder fra forrige og nytt vedtak.
//     * Beholder perioder fra forrige vedtak frem til og med revurder-fra
//     * Bruker reberegnede perioder fra og med revurder-fra dato
//     * Dette gjøres for at vi ikke skal reberegne perioder før revurder-fra datoet
//     * Men vi trenger å reberegne perioder som løper i revurder-fra datoet då en periode kan ha endrer % eller sats
//     */
//    private fun settSammenGamleOgNyePerioder(
//        saksbehandling: Saksbehandling,
//        beregningsresultat: List<BeregningsresultatForMåned>,
//        forrigeVedtak: InnvilgelseEllerOpphørBoutgifter,
//    ): BeregningsresultatBoutgifter {
//        val revurderFra = saksbehandling.revurderFra
//        feilHvis(revurderFra == null) { "Behandling=$saksbehandling mangler revurderFra" }
//
//        val forrigeBeregningsresultat = forrigeVedtak.beregningsresultat
//
//        val perioderFraForrigeVedtakSomSkalBeholdes =
//            forrigeBeregningsresultat
//                .perioder
//                .filter { it.grunnlag.fom.sisteDagenILøpendeMåned() < revurderFra }
//                .map { it.markerSomDelAvTidligereUtbetaling(delAvTidligereUtbetaling = true) }
//        val nyePerioder =
//            beregningsresultat
//                .filter { it.grunnlag.fom.sisteDagenILøpendeMåned() >= revurderFra }
//
//        val nyePerioderMedKorrigertUtbetalingsdato = korrigerUtbetalingsdato(nyePerioder, forrigeBeregningsresultat)
//
//        return BeregningsresultatBoutgifter(
//            perioder = perioderFraForrigeVedtakSomSkalBeholdes + nyePerioderMedKorrigertUtbetalingsdato,
//        )
//    }

//    private fun korrigerUtbetalingsdato(
//        nyePerioder: List<BeregningsresultatForMåned>,
//        forrigeBeregningsresultat: BeregningsresultatBoutgifter,
//    ): List<BeregningsresultatForMåned> {
//        val utbetalingsdatoPerMåned =
//            forrigeBeregningsresultat
//                .perioder
//                .associate { it.grunnlag.fom.toYearMonth() to it.grunnlag.utbetalingsdato }
//
//        return nyePerioder
//            .map {
//                val utbetalingsdato = utbetalingsdatoPerMåned[it.fom.toYearMonth()]
//                if (utbetalingsdato != null) {
//                    it
//                        .medKorrigertUtbetalingsdato(utbetalingsdato)
//                        .markerSomDelAvTidligereUtbetaling(delAvTidligereUtbetaling = true)
//                } else {
//                    it
//                }
//            }
//    }

    private fun hentForrigeVedtak(behandling: Saksbehandling): InnvilgelseEllerOpphørBoutgifter? =
        behandling.forrigeIverksatteBehandlingId?.let { hentVedtak(it) }?.data

    private fun hentVedtak(behandlingId: BehandlingId) =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseEllerOpphørBoutgifter>()

    private fun lagBeregningsGrunnlag(
        periode: UtbetalingPeriode,
        utgifter: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>,
    ): Beregningsgrunnlag {
        val sats = finnMakssatsForPeriode(periode)

        val utgifterIPerioden = finnUtgiftForUtbetalingsperiode(utgifter, periode)

        return Beregningsgrunnlag(
            fom = periode.fom,
            tom = periode.tom,
            utgifter = utgifterIPerioden,
            makssats = sats.beløp,
            makssatsBekreftet = sats.bekreftet,
            utbetalingsdato = periode.utbetalingsdato,
            målgruppe = periode.målgruppe,
            aktivitet = periode.aktivitet,
        )
    }

    private fun finnUtgiftForUtbetalingsperiode(
        utgifter: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>,
        periode: UtbetalingPeriode,
    ) = utgifter.mapValues { it.value.filter { utgift -> periode.inneholder(utgift) } }
}

private fun validerUtgifterErInnenforVedtaksperiodene(
    utgifterPerType: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>,
    vedtaksperioder: List<VedtaksperiodeBeregning>,
) {
    val utgifter = utgifterPerType.values.flatten()

    val alleUtgifterErInnenforEnVedtaksperiode =
        utgifter.all { utgiftsperiode ->
            vedtaksperioder.any { it.inneholder(utgiftsperiode) }
        }

    brukerfeilHvisIkke(alleUtgifterErInnenforEnVedtaksperiode) {
        "Du har lagt inn utgifter som er utenfor vedtaksperioden. Foreløpig støtter vi ikke dette."
    }
}

private fun List<UtbetalingPeriode>.validerIngenUtgifterKrysserUtbetalingsperioder(
    utgifterPerBoutgiftstype: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>,
): List<UtbetalingPeriode> {
    val utbetalingsperioder = this
    val utgifter = utgifterPerBoutgiftstype.values.flatten()

    val detFinnesUtgiftSomKrysserUtbetalingsperioder =
        utgifter.any { utgift ->
            utbetalingsperioder.none { it.inneholder(utgift) }
        }

    brukerfeilHvis(detFinnesUtgiftSomKrysserUtbetalingsperioder) {
        """
        Vi støtter foreløpig ikke at utgifter krysser ulike utbetalingsperioder. 
        Utbetalingsperioder for denne behandlingen er: ${utbetalingsperioder.map { it.formatertPeriodeNorskFormat() }}, 
        mens utgiftsperiodene er: ${utgifter.map { it.formatertPeriodeNorskFormat() }}
        """.trimIndent()
    }

    return this
}
