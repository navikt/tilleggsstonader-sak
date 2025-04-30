package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.UtgiftBeregningDato
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterBeregnUtil.splittTilLøpendeMåneder
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
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerVedtaksperiodeUtil.sisteDagenILøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import org.springframework.stereotype.Service

@Service
class BoutgifterBeregningService(
    private val boutgifterUtgiftService: BoutgifterUtgiftService,
    private val vedtaksperiodeValideringService: VedtaksperiodeValideringService,
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
        typeVedtak: TypeVedtak,
    ): BeregningsresultatBoutgifter {
        val forrigeVedtak = hentForrigeVedtak(behandling)

        val utgifterPerVilkårtype = boutgifterUtgiftService.hentUtgifterTilBeregning(behandling.id)

        validerUtgifter(utgifterPerVilkårtype)

        validerUtgiftHeleVedtaksperioden(vedtaksperioder, utgifterPerVilkårtype)

        vedtaksperiodeValideringService.validerVedtaksperioder(
            vedtaksperioder = vedtaksperioder,
            behandling = behandling,
            typeVedtak = typeVedtak,
        )

        val vedtaksperioderBeregning =
            vedtaksperioder.tilVedtaksperiodeBeregning().sorted().splitFraRevurderFra(behandling.revurderFra)

        validerUtgifterTilMidlertidigOvernattingErInnenforVedtaksperiodene(
            utgifterPerVilkårtype,
            vedtaksperioderBeregning,
        )

        val beregningsresultat =
            beregnAktuellePerioder(
                vedtaksperioder = vedtaksperioderBeregning,
                utgifter = utgifterPerVilkårtype,
            )

        return if (forrigeVedtak != null) {
            settSammenGamleOgNyePerioder(
                saksbehandling = behandling,
                beregningsresultat = beregningsresultat,
                forrigeVedtak = forrigeVedtak,
            )
        } else {
            BeregningsresultatBoutgifter(perioder = beregningsresultat)
        }
    }

    private fun beregnAktuellePerioder(
        vedtaksperioder: List<VedtaksperiodeBeregning>,
        utgifter: Map<TypeBoutgift, List<UtgiftBeregningDato>>,
    ): List<BeregningsresultatForLøpendeMåned> =
        vedtaksperioder
            .sorted()
            .splittTilLøpendeMåneder()
            .map { UtbetalingPeriode(it) }
            .validerIngenUtgifterTilOvernattingKrysserUtbetalingsperioder(utgifter)
            .map {
                BeregningsresultatForLøpendeMåned(
                    grunnlag = lagBeregningsGrunnlag(periode = it, utgifter = utgifter),
                )
            }

    /**
     * Slår sammen perioder fra forrige og nytt vedtak.
     * Beholder perioder fra forrige vedtak frem til og med revurder-fra
     * Bruker reberegnede perioder fra og med revurder-fra dato
     * Dette gjøres for at vi ikke skal reberegne perioder før revurder-fra datoet
     * Men vi trenger å reberegne perioder som løper i revurder-fra datoet da en periode kan ha endret utgift
     */
    private fun settSammenGamleOgNyePerioder(
        saksbehandling: Saksbehandling,
        beregningsresultat: List<BeregningsresultatForLøpendeMåned>,
        forrigeVedtak: InnvilgelseEllerOpphørBoutgifter,
    ): BeregningsresultatBoutgifter {
        val revurderFra = saksbehandling.revurderFra
        feilHvis(revurderFra == null) { "Behandling=$saksbehandling mangler revurderFra" }

        val forrigeBeregningsresultat = forrigeVedtak.beregningsresultat

        val perioderFraForrigeVedtakSomSkalBeholdes =
            forrigeBeregningsresultat
                .perioder
                .filter { it.grunnlag.fom.sisteDagenILøpendeMåned() < revurderFra }
                .map { it.markerSomDelAvTidligereUtbetaling(delAvTidligereUtbetaling = true) }
        val nyePerioder =
            beregningsresultat
                .filter { it.grunnlag.fom.sisteDagenILøpendeMåned() >= revurderFra }

        return BeregningsresultatBoutgifter(
            perioder = perioderFraForrigeVedtakSomSkalBeholdes + nyePerioder,
        )
    }

    private fun hentForrigeVedtak(behandling: Saksbehandling): InnvilgelseEllerOpphørBoutgifter? =
        behandling.forrigeIverksatteBehandlingId?.let { hentVedtak(it) }?.data

    private fun hentVedtak(behandlingId: BehandlingId) =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseEllerOpphørBoutgifter>()

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

    private fun lagBeregningsGrunnlag(
        periode: UtbetalingPeriode,
        utgifter: Map<TypeBoutgift, List<UtgiftBeregningDato>>,
    ): Beregningsgrunnlag {
        val sats = finnMakssats(periode.fom)

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
        utgifter: Map<TypeBoutgift, List<UtgiftBeregningDato>>,
        periode: UtbetalingPeriode,
    ) = utgifter.mapValues { (_, utgifter) ->
        utgifter.filter {
            periode.overlapper(it)
        }
    }
}

private fun validerUtgifterTilMidlertidigOvernattingErInnenforVedtaksperiodene(
    utgifterPerType: Map<TypeBoutgift, List<UtgiftBeregningDato>>,
    vedtaksperioder: List<VedtaksperiodeBeregning>,
) {
    val utgifterMidlertidigOvernatting = utgifterPerType[TypeBoutgift.UTGIFTER_OVERNATTING].orEmpty()
    val sammenslåtteVedtaksperioder =
        vedtaksperioder.mergeSammenhengende(
            { v1, v2 -> v1.overlapperEllerPåfølgesAv(v2) },
            { v1, v2 -> v1.medPeriode(fom = minOf(v1.fom, v2.fom), tom = maxOf(v1.tom, v2.tom)) },
        )

    val alleUtgifterErInnenforVedtaksperioder =
        utgifterMidlertidigOvernatting.all { utgiftsperiode ->
            sammenslåtteVedtaksperioder.any { it.inneholder(utgiftsperiode) }
        }

    brukerfeilHvisIkke(alleUtgifterErInnenforVedtaksperioder) {
        "Du har lagt inn utgifter til midlertidig overnatting som ikke er inneholdt i en vedtaksperiode. Foreløpig støtter vi ikke dette."
    }
}

private fun List<UtbetalingPeriode>.validerIngenUtgifterTilOvernattingKrysserUtbetalingsperioder(
    utgifter: Map<TypeBoutgift, List<UtgiftBeregningDato>>,
): List<UtbetalingPeriode> {
    val utgifterTilOvernatting = utgifter[TypeBoutgift.UTGIFTER_OVERNATTING] ?: emptyList()
    val utbetalingsperioder = this

    val detFinnesUtgiftSomKrysserUtbetalingsperioder =
        utgifterTilOvernatting.any { utgift ->
            utbetalingsperioder.none { it.inneholder(utgift) }
        }

    brukerfeilHvis(detFinnesUtgiftSomKrysserUtbetalingsperioder) {
        """
        Vi støtter foreløpig ikke at utgifter krysser ulike utbetalingsperioder. 
        Utbetalingsperioder for denne behandlingen er: ${utbetalingsperioder.map { it.formatertPeriodeNorskFormat() }}, 
        mens utgiftsperiodene er: ${utgifterTilOvernatting.map { it.formatertPeriodeNorskFormat() }}
        """.trimIndent()
    }

    return this
}
