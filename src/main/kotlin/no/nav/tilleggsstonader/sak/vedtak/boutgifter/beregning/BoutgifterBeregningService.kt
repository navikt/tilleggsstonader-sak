package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterBeregnBeløpUtil.beregnBeløp
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterBeregnUtil.grupperVedtaksperioderPerLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
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
     * - Vi antar at satsen ikke endrer seg i vedtaksperioden
     */
    fun beregn(
        behandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsresultatBoutgifter {
//        val stønadsperioder = hentStønadsperioder(behandling.id)
//        val forrigeVedtak = hentForrigeVedtak(behandling)

        val utgifterPerVilkårtype = boutgifterUtgiftService.hentUtgifterTilBeregning(behandling.id)

//        vedtaksperiodeValideringService.validerVedtaksperioder(
//            vedtaksperioder = vedtaksperioder,
//            stønadsperioder = stønadsperioder,
//            behandlingId = behandling.id,
//        )

        // TODO: Deal med revurderFra-datoen

        val vedtaksperioderBeregning =
            vedtaksperioder.tilVedtaksperiodeBeregning().sorted().splitFraRevurderFra(behandling.revurderFra)

//        val beregningsresultat = beregnAktuellePerioder(behandling, vedtaksperioder, stønadsperioder)
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
        utgifter: Map<Unit, List<UtgiftBeregning>>,
    ): List<BeregningsresultatForLøpendeMåned> {
//        validerPerioderForInnvilgelse(vedtaksperioder, utgifterPerBarn, typeVedtak)

        return vedtaksperioder
            .sorted()
            .grupperVedtaksperioderPerLøpendeMåned()
            .map { UtbetalingPeriode(it) }
            .map { utbetalingPeriode ->
                val grunnlagsdata = lagBeregningsGrunnlag(periode = utbetalingPeriode, utgifter = utgifter)
                BeregningsresultatForLøpendeMåned(
                    stønadsbeløp = beregnBeløp(utgifter = utgifter, makssats = grunnlagsdata.makssats),
                    grunnlag = grunnlagsdata,
                )
            }
    }

//    fun beregnForOpphør(
//        behandling: Saksbehandling,
//        avkortetVedtaksperioder: List<Vedtaksperiode>,
//    ): BeregningsresultatBoutgifter {
//        feilHvis(behandling.forrigeBehandlingId == null) {
//            "Opphør er et ugyldig vedtaksresultat fordi behandlingen er en førstegangsbehandling"
//        }
//        feilHvis(behandling.revurderFra == null) {
//            "revurderFra-dato er påkrevd for opphør"
//        }
//        val forrigeVedtak = hentVedtak(behandling.forrigeBehandlingId)
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
        behandling.forrigeBehandlingId?.let { hentVedtak(it) }?.data

    private fun hentVedtak(behandlingId: BehandlingId) =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseEllerOpphørBoutgifter>()

    private fun lagBeregningsGrunnlag(
        periode: UtbetalingPeriode,
        utgifter: Map<Unit, List<UtgiftBeregning>>,
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
        utgifter: Map<Unit, List<UtgiftBeregning>>,
        periode: UtbetalingPeriode,
    ): Map<Unit, List<UtgiftBeregning>> {
        require(utgifter.values.all { utgifterListe -> utgifterListe.all { periode.inneholder(it) } }) {
            "Per nå krever vi at alle utgiftene havner innefor samme utgiftsperiode."
        }

        return utgifter
    }
}
