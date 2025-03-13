package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterBeregnBeløpUtil.beregnBeløp
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterBeregnUtil.grupperVedtaksperioderPerLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterVedtaksperiodeUtil.sisteDagenILøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.slåSammenSammenhengende
import no.nav.tilleggsstonader.sak.vedtak.domain.tilSortertStønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.springframework.stereotype.Service
import kotlin.collections.plus

@Service
class BoutgifterBeregningService(
    private val vilkårperiodeRepository: VilkårperiodeRepository,
    private val stønadsperiodeRepository: StønadsperiodeRepository,
    private val vedtaksperiodeValideringService: BoutgifterVedtaksperiodeValideringService,
    private val vedtakRepository: VedtakRepository,
) {
    /**
     * TODO Oppdater denne, stemmer ikke lenger for læremidler
     * Beregning av boutgifter har foreløpig noen begrensninger.
     * Vi antar kun en aktivitet gjennom hele vedtaksperioden
     * Vi antar kun en stønadsperiode per vedtaksperiode for å kunne velge ut rett målgruppe
     * Vi antar at satsen ikke endrer seg i vedtaksperioden
     * Programmet kaster feil dersom antagelsene ikke stemmer
     */
    fun beregn(
        behandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsresultatBoutgifter {
        val stønadsperioder = hentStønadsperioder(behandling.id)
        val forrigeVedtak = hentForrigeVedtak(behandling)

        vedtaksperiodeValideringService.validerVedtaksperioder(
            vedtaksperioder = vedtaksperioder,
            stønadsperioder = stønadsperioder,
            behandlingId = behandling.id,
        )

        val beregningsresultatForMåned = beregn(behandling, vedtaksperioder, stønadsperioder)

        return if (forrigeVedtak != null) {
            settSammenGamleOgNyePerioder(behandling, beregningsresultatForMåned, forrigeVedtak)
        } else {
            BeregningsresultatBoutgifter(beregningsresultatForMåned)
        }
    }

    private fun beregn(
        behandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
        stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>,
    ): List<BeregningsresultatForMåned> {
        val aktiviteter = finnAktiviteter(behandling.id)
        return beregnBoutgifterPerMåned(vedtaksperioder, stønadsperioder, aktiviteter)
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

    /**
     * Slår sammen perioder fra forrige og nytt vedtak.
     * Beholder perioder fra forrige vedtak frem til og med revurder-fra
     * Bruker reberegnede perioder fra og med revurder-fra dato
     * Dette gjøres for at vi ikke skal reberegne perioder før revurder-fra datoet
     * Men vi trenger å reberegne perioder som løper i revurder-fra datoet då en periode kan ha endrer % eller sats
     */
    private fun settSammenGamleOgNyePerioder(
        saksbehandling: Saksbehandling,
        beregningsresultat: List<BeregningsresultatForMåned>,
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

        val nyePerioderMedKorrigertUtbetalingsdato = korrigerUtbetalingsdato(nyePerioder, forrigeBeregningsresultat)

        return BeregningsresultatBoutgifter(
            perioder = perioderFraForrigeVedtakSomSkalBeholdes + nyePerioderMedKorrigertUtbetalingsdato,
        )
    }

    private fun korrigerUtbetalingsdato(
        nyePerioder: List<BeregningsresultatForMåned>,
        forrigeBeregningsresultat: BeregningsresultatBoutgifter,
    ): List<BeregningsresultatForMåned> {
        val utbetalingsdatoPerMåned =
            forrigeBeregningsresultat
                .perioder
                .associate { it.grunnlag.fom.toYearMonth() to it.grunnlag.utbetalingsdato }

        return nyePerioder
            .map {
                val utbetalingsdato = utbetalingsdatoPerMåned[it.fom.toYearMonth()]
                if (utbetalingsdato != null) {
                    it
                        .medKorrigertUtbetalingsdato(utbetalingsdato)
                        .markerSomDelAvTidligereUtbetaling(delAvTidligereUtbetaling = true)
                } else {
                    it
                }
            }
    }

    private fun hentForrigeVedtak(behandling: Saksbehandling): InnvilgelseEllerOpphørBoutgifter? =
        behandling.forrigeBehandlingId?.let { hentVedtak(it) }?.data

    private fun hentVedtak(behandlingId: BehandlingId) =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseEllerOpphørBoutgifter>()

    private fun hentStønadsperioder(behandlingId: BehandlingId): List<StønadsperiodeBeregningsgrunnlag> =
        stønadsperiodeRepository
            .findAllByBehandlingId(behandlingId)
            .tilSortertStønadsperiodeBeregningsgrunnlag()
            .slåSammenSammenhengende()

    private fun finnAktiviteter(behandlingId: BehandlingId): List<AktivitetBoutgifterBeregningGrunnlag> =
        vilkårperiodeRepository
            .findByBehandlingIdAndResultat(behandlingId, ResultatVilkårperiode.OPPFYLT)
            .tilAktiviteter()

    private fun beregnBoutgifterPerMåned(
        vedtaksperioder: List<Vedtaksperiode>,
        stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>,
        aktiviteter: List<AktivitetBoutgifterBeregningGrunnlag>,
    ): List<BeregningsresultatForMåned> =
        vedtaksperioder
            .sorted()
            .grupperVedtaksperioderPerLøpendeMåned()
            .map { it.tilUtbetalingPeriode(stønadsperioder, aktiviteter) }
            .beregn()

    private fun List<UtbetalingPeriode>.beregn(): List<BeregningsresultatForMåned> =
        this
            .map { utbetalingPeriode ->
                val grunnlagsdata = lagBeregningsGrunnlag(periode = utbetalingPeriode)

                BeregningsresultatForMåned(
//                    beløp = beregnBeløp(grunnlagsdata.sats, grunnlagsdata.studieprosent),
                    beløp = beregnBeløp(),
                    grunnlag = grunnlagsdata,
                )
            }

    private fun lagBeregningsGrunnlag(periode: UtbetalingPeriode): Beregningsgrunnlag {
        val sats = finnSatsForPeriode(periode)

        return Beregningsgrunnlag(
            fom = periode.fom,
            tom = periode.tom,
//            studienivå = periode.studienivå,
//            studieprosent = periode.prosent,
//            sats = finnSatsForStudienivå(sats, periode.studienivå),
            sats = finnSatsForStudienivå(),
            satsBekreftet = sats.bekreftet,
            utbetalingsdato = periode.utbetalingsdato,
            målgruppe = periode.målgruppe,
            aktivitet = periode.aktivitet,
        )
    }
}
