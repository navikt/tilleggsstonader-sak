package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.util.formatertPeriodeNorskFormat
import no.nav.tilleggsstonader.sak.util.sisteDagenILøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterBeregnUtil.beregnStønadsbeløp
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterBeregnUtil.lagBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterBeregnUtil.splittTilLøpendeMåneder
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.MarkerSomDelAvTidligereUtbetlingUtils.markerSomDelAvTidligereUtbetaling
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.UtgifterValideringUtil.validerUtgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BoutgifterPerUtgiftstype
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning.Companion.mergeSammenhengende
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningUtil.splitFraRevurderFra
import no.nav.tilleggsstonader.sak.vedtak.domain.tilVedtaksperiodeBeregning
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

        vedtaksperiodeValideringService.validerVedtaksperioder(
            vedtaksperioder = vedtaksperioder,
            behandling = behandling,
            typeVedtak = typeVedtak,
        )

        val vedtaksperioderBeregning =
            vedtaksperioder.tilVedtaksperiodeBeregning().sorted().splitFraRevurderFra(behandling.revurderFra)

        val utgifterPerVilkårtype =
            boutgifterUtgiftService
                .hentUtgifterTilBeregning(behandling.id)
                .filtrerBortUtgifterSomIkkeOverlapperVedtaksperioder(vedtaksperioderBeregning)

        validerUtgifter(utgifterPerVilkårtype)

        validerUtgiftHeleVedtaksperioden(vedtaksperioder, utgifterPerVilkårtype)
        validerMidlertidigeUtgifterStrekkerSegUtenforVedtaksperiodene(
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
                revurderFra = behandling.revurderFra ?: feil("Behandlingen mangler revurder fra-dato"),
                nyttBeregningsresultat = beregningsresultat,
                forrigeBeregningsresultat = forrigeVedtak.beregningsresultat,
            )
        } else {
            BeregningsresultatBoutgifter(perioder = beregningsresultat)
        }
    }

    private fun beregnAktuellePerioder(
        vedtaksperioder: List<VedtaksperiodeBeregning>,
        utgifter: BoutgifterPerUtgiftstype,
    ): List<BeregningsresultatForLøpendeMåned> =
        vedtaksperioder
            .sorted()
            .splittTilLøpendeMåneder()
            .map { UtbetalingPeriode(it, skalAvkorteUtbetalingPeriode(utgifter)) }
            .validerIngenUtgifterTilOvernattingKrysserUtbetalingsperioder(utgifter)
            .validerIngenUtbetalingsperioderOverlapperFlereLøpendeUtgifter(utgifter)
            .map { lagBeregningsgrunnlag(periode = it, utgifter = utgifter) }
            .map {
                BeregningsresultatForLøpendeMåned(
                    grunnlag = it,
                    stønadsbeløp = it.beregnStønadsbeløp(),
                )
            }

    private fun skalAvkorteUtbetalingPeriode(utgifter: BoutgifterPerUtgiftstype): Boolean =
        TypeBoutgift.UTGIFTER_OVERNATTING !in utgifter.keys || !unleashService.isEnabled(Toggle.SKAL_VISE_DETALJERT_BEREGNINGSRESULTAT)

    /**
     * Slår sammen perioder fra forrige og nytt vedtak.
     * Beholder perioder fra forrige vedtak frem til revurder fra-datoen.
     * Bruker reberegnede perioder fra og med revurder fra-datoen
     * Dette gjøres for at vi ikke skal reberegne perioder som ikke er med i revurderingen, i tilfelle beregningskoden har endret seg siden sist.
     * Vi trenger derimot å reberegne alle perioder som ligger etter revurder fra-datoen, da utgiftene, antall samlinger osv kan ha endret seg.
     */
    private fun settSammenGamleOgNyePerioder(
        revurderFra: LocalDate,
        nyttBeregningsresultat: List<BeregningsresultatForLøpendeMåned>,
        forrigeBeregningsresultat: BeregningsresultatBoutgifter,
    ): BeregningsresultatBoutgifter {
        val perioderFraForrigeVedtakSomSkalBeholdes =
            forrigeBeregningsresultat.perioder
                .filter { it.grunnlag.fom.sisteDagenILøpendeMåned() < revurderFra }
                .markerSomDelAvTidligereUtbetaling()

        val reberegnedePerioder =
            nyttBeregningsresultat
                .filter {
                    it.fom.sisteDagenILøpendeMåned() >= revurderFra
                }.markerSomDelAvTidligereUtbetaling(forrigeBeregningsresultat.perioder)
        return BeregningsresultatBoutgifter(perioderFraForrigeVedtakSomSkalBeholdes + reberegnedePerioder)
    }

    private fun hentForrigeVedtak(behandling: Saksbehandling): InnvilgelseEllerOpphørBoutgifter? =
        behandling.forrigeIverksatteBehandlingId?.let { hentVedtak(it) }?.data

    private fun hentVedtak(behandlingId: BehandlingId) =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseEllerOpphørBoutgifter>()
}

private fun BoutgifterPerUtgiftstype.filtrerBortUtgifterSomIkkeOverlapperVedtaksperioder(
    vedtaksperioder: List<VedtaksperiodeBeregning>,
): BoutgifterPerUtgiftstype =
    mapValues { (_, utgifter) ->
        utgifter.filter { utgift -> vedtaksperioder.any { vedtaksperiode -> vedtaksperiode.overlapper(utgift) } }
    }

private fun validerMidlertidigeUtgifterStrekkerSegUtenforVedtaksperiodene(
    utgifterPerType: BoutgifterPerUtgiftstype,
    vedtaksperioder: List<VedtaksperiodeBeregning>,
) {
    val midlertidigOvernattinger = utgifterPerType[TypeBoutgift.UTGIFTER_OVERNATTING].orEmpty()

    val alleUtgifterErInnenforVedtaksperioder =
        midlertidigOvernattinger.all { utgiftsperiode ->
            vedtaksperioder.mergeSammenhengende().any { it.inneholder(utgiftsperiode) }
        }

    brukerfeilHvisIkke(alleUtgifterErInnenforVedtaksperioder) {
        "Vi har foreløpig ikke støtte for å beregne når utgifter til midlertidig overnatting strekker seg utenfor vedtaksperiodene."
    }
}

private fun List<UtbetalingPeriode>.validerIngenUtbetalingsperioderOverlapperFlereLøpendeUtgifter(
    utgifter: BoutgifterPerUtgiftstype,
): List<UtbetalingPeriode> {
    val fasteUtgifter =
        (utgifter[TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG] ?: emptyList()) +
            (utgifter[TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER] ?: emptyList())

    val detFinnesUtbetalingsperioderSomOverlapperFlereLøpendeUtgifter =
        any { utbetalingsperiode ->
            fasteUtgifter.count { utbetalingsperiode.overlapper(it) } > 1
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
    utgifter: BoutgifterPerUtgiftstype,
): List<UtbetalingPeriode> {
    val utgifterTilOvernatting = utgifter[TypeBoutgift.UTGIFTER_OVERNATTING] ?: emptyList()
    val utbetalingsperioder = this

    val detFinnesUtgiftSomKrysserUtbetalingsperioder =
        utgifterTilOvernatting.any { utgift ->
            utbetalingsperioder.none { it.inneholder(utgift) }
        }

    brukerfeilHvis(detFinnesUtgiftSomKrysserUtbetalingsperioder) {
        buildString {
            appendLine("Utgiftsperioder krysser beregningsperioder")
            appendLine()
            appendLine(
                lagPunktlisteMedOverlappendeUtgifterOgBeregningsperioder(
                    utgifter = utgifterTilOvernatting,
                    utbetalingsperioder = utbetalingsperioder,
                ),
            )
            appendLine()
            appendLine("Utgiftsperioden(e) må splittes.")
        }
    }

    return this
}

private fun UtgiftBeregningBoutgifter.finnOverlappendeUtbetalingsperioder(
    utbetalingsperioder: List<UtbetalingPeriode>,
): List<UtbetalingPeriode> = utbetalingsperioder.filter { it.overlapper(this) }

private fun UtgiftBeregningBoutgifter.overlapperFlereUtbetalingsperioder(utbetalingsperioder: List<UtbetalingPeriode>): Boolean =
    finnOverlappendeUtbetalingsperioder(utbetalingsperioder).size > 1

private fun lagPunktlisteMedOverlappendeUtgifterOgBeregningsperioder(
    utgifter: List<UtgiftBeregningBoutgifter>,
    utbetalingsperioder: List<UtbetalingPeriode>,
): String =
    utgifter
        .sorted()
        .filter { utgift -> utgift.overlapperFlereUtbetalingsperioder(utbetalingsperioder) }
        .joinToString(separator = "\n\n") { utgift ->
            "Utgiftsperiode ${utgift.formatertPeriodeNorskFormat()} krysser beregningsperiodene: \n ${
                utgift.finnOverlappendeUtbetalingsperioder(
                    utbetalingsperioder,
                ).joinToString("\n ") { utbetalingsperiode -> "- ${utbetalingsperiode.formatertPeriodeNorskFormat()}" }
            }"
        }
