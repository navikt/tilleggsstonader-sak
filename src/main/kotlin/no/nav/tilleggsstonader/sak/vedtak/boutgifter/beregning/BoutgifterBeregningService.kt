package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
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
import java.time.LocalDate

@Service
class BoutgifterBeregningService(
    private val boutgifterUtgiftService: BoutgifterUtgiftService,
    private val vedtaksperiodeValideringService: VedtaksperiodeValideringService,
    private val vedtakRepository: VedtakRepository,
    private val unleashService: UnleashService,
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
        utgifter: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>,
    ): List<BeregningsresultatForLøpendeMåned> =
        vedtaksperioder
            .sorted()
            .splittTilLøpendeMåneder()
            .map { UtbetalingPeriode(it, skalAvkorteUtbetalingPeriode(utgifter)) }
            .validerIngenUtgifterTilOvernattingKrysserUtbetalingsperioder(utgifter)
            .validerIngenUtbetalingsperioderOverlapperFlereLøpendeUtgifter(utgifter)
            .map {
                BeregningsresultatForLøpendeMåned(
                    grunnlag = lagBeregningsGrunnlag(periode = it, utgifter = utgifter),
                )
            }

    private fun skalAvkorteUtbetalingPeriode(utgifter: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>): Boolean =
        TypeBoutgift.UTGIFTER_OVERNATTING !in utgifter.keys || !unleashService.isEnabled(Toggle.SKAL_VISE_DETALJERT_BEREGNINGSRESULTAT)

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
                .filter { it.grunnlag.tom < revurderFra }
                .map { it.markerSomDelAvTidligereUtbetaling(delAvTidligereUtbetaling = true) }

        val perioderSomSkalReberegnes =
            beregningsresultat
                .filter { revurderFra.erInneholdILøpendeMåned(it) }
                .map { it.markerSomDelAvTidligereUtbetaling(true) }

        val nyePerioder = beregningsresultat.filter { it.grunnlag.fom >= revurderFra }

        return BeregningsresultatBoutgifter(
            perioder = perioderFraForrigeVedtakSomSkalBeholdes + perioderSomSkalReberegnes + nyePerioder,
        )
    }

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
        utgifter: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>,
        periode: UtbetalingPeriode,
    ) = utgifter.mapValues { (_, utgifter) ->
        utgifter.filter {
            periode.overlapper(it)
        }
    }

    private fun LocalDate.erInneholdILøpendeMåned(løpendeMåned: Periode<LocalDate>) =
        løpendeMåned.fom < this && løpendeMåned.fom.sisteDagenILøpendeMåned() > this
}

private fun validerUtgifterTilMidlertidigOvernattingErInnenforVedtaksperiodene(
    utgifterPerType: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>,
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

private fun List<UtbetalingPeriode>.validerIngenUtbetalingsperioderOverlapperFlereLøpendeUtgifter(
    utgifter: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>,
): List<UtbetalingPeriode> {
    val fasteUtgifter =
        (utgifter[TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG] ?: emptyList()) +
            (utgifter[TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER] ?: emptyList())

    val detFinnesUtbetalingsperioderSomOverlapperFlereLøpendeUtgifter =
        any { utbetalingsperiode ->
            fasteUtgifter.filter { utbetalingsperiode.overlapper(it) }.size > 1
        }

    feilHvis(detFinnesUtbetalingsperioderSomOverlapperFlereLøpendeUtgifter) {
        """
        Vi støtter foreløpig ikke at utbetalingsperioder overlapper mer enn én løpende utgift. 
        Utbetalingsperioder for denne behandlingen er: ${map { it.formatertPeriodeNorskFormat() }}, 
        mens utgiftsperiodene er: ${fasteUtgifter.map { it.formatertPeriodeNorskFormat() }}
        """.trimIndent()
    }

    return this
}

private fun List<UtbetalingPeriode>.validerIngenUtgifterTilOvernattingKrysserUtbetalingsperioder(
    utgifter: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>,
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
