package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.periode.AvkortResult
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.slåSammenSammenhengende
import no.nav.tilleggsstonader.sak.vedtak.domain.tilSortertStønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregnBeløpUtil.beregnBeløp
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregnUtil.splittTilLøpendeMåneder
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerVedtaksperiodeUtil.sisteDagenILøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.LæremidlerVedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeUtil.vedtaksperioderInnenforLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.avkortBeregningsresultatVedOpphør
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.springframework.stereotype.Service

@Service
class LæremidlerBeregningService(
    private val vilkårperiodeRepository: VilkårperiodeRepository,
    private val stønadsperiodeRepository: StønadsperiodeRepository,
    private val læremidlerVedtaksperiodeValideringService: LæremidlerVedtaksperiodeValideringService,
    private val vedtakRepository: VedtakRepository,
) {
    /**
     * Kjente begrensninger i beregningen:
     * 1. Vi antar at satsen ikke endrer seg i vedtaksperioden
     * Programmet kaster feil dersom antagelsene ikke stemmer
     */
    fun beregn(
        behandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsresultatLæremidler {
        val stønadsperioder = hentStønadsperioder(behandling.id)
        val forrigeVedtak = hentForrigeVedtak(behandling)

        læremidlerVedtaksperiodeValideringService.validerVedtaksperioder(
            vedtaksperioder = vedtaksperioder,
            stønadsperioder = stønadsperioder,
            behandlingId = behandling.id,
        )

        val beregningsresultatForMåned = beregn(behandling, vedtaksperioder, stønadsperioder)

        return if (forrigeVedtak != null) {
            settSammenGamleOgNyePerioder(behandling, beregningsresultatForMåned, forrigeVedtak)
        } else {
            BeregningsresultatLæremidler(beregningsresultatForMåned)
        }
    }

    private fun beregn(
        behandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
        stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>,
    ): List<BeregningsresultatForMåned> {
        val aktiviteter = finnAktiviteter(behandling.id)
        return beregnLæremidlerPerMåned(vedtaksperioder, stønadsperioder, aktiviteter)
    }

    fun beregnForOpphør(
        behandling: Saksbehandling,
        avkortetVedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsresultatLæremidler {
        feilHvis(behandling.forrigeIverksatteBehandlingId == null) {
            "Opphør er et ugyldig vedtaksresultat fordi behandlingen er en førstegangsbehandling"
        }
        feilHvis(behandling.revurderFra == null) {
            "revurderFra-dato er påkrevd for opphør"
        }
        val forrigeVedtak = hentVedtak(behandling.forrigeIverksatteBehandlingId)
        val avkortetBeregningsresultat = avkortBeregningsresultatVedOpphør(forrigeVedtak, behandling.revurderFra)

        return beregningsresultatForOpphør(
            behandling = behandling,
            avkortetBeregningsresultat = avkortetBeregningsresultat,
            avkortetVedtaksperioder = avkortetVedtaksperioder,
        )
    }

    /**
     * Hvis man har avkortet siste måneden må man reberegne den i tilfelle % på aktiviteter har endret seg
     * Eks at man hadde 2 aktiviteter, 50 og 100% som då gir 100%.
     * Etter opphør så har man kun 50% og då trenger å omberegne perioden
     */
    private fun beregningsresultatForOpphør(
        behandling: Saksbehandling,
        avkortetBeregningsresultat: AvkortResult<BeregningsresultatForMåned>,
        avkortetVedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsresultatLæremidler {
        if (!avkortetBeregningsresultat.harAvkortetPeriode) {
            return BeregningsresultatLæremidler(avkortetBeregningsresultat.perioder)
        }

        return beregningsresultatOpphørMedReberegnetPeriode(
            behandling = behandling,
            avkortetBeregningsresultat = avkortetBeregningsresultat,
            avkortetVedtaksperioder = avkortetVedtaksperioder,
        )
    }

    private fun beregningsresultatOpphørMedReberegnetPeriode(
        behandling: Saksbehandling,
        avkortetBeregningsresultat: AvkortResult<BeregningsresultatForMåned>,
        avkortetVedtaksperioder: List<Vedtaksperiode>,
    ): BeregningsresultatLæremidler {
        val perioderSomBeholdes = avkortetBeregningsresultat.perioder.dropLast(1)
        val periodeTilReberegning = avkortetBeregningsresultat.perioder.last()

        val reberegnedePerioderKorrigertUtbetalingsdato =
            reberegnPerioderForOpphør(
                behandling = behandling,
                avkortetVedtaksperioder = avkortetVedtaksperioder,
                beregningsresultatTilReberegning = periodeTilReberegning,
            )

        val nyePerioder =
            (perioderSomBeholdes + reberegnedePerioderKorrigertUtbetalingsdato)
                .map { it.markerSomDelAvTidligereUtbetaling(delAvTidligereUtbetaling = false) }
        return BeregningsresultatLæremidler(nyePerioder)
    }

    /**
     * Reberegner siste perioden då den er avkortet og ev skal få annet beløp utbetalt
     * Korrigerer utbetalingsdatoet då vedtaksperioden sånn at andelen som sen genereres for det utbetales på likt dato
     */
    private fun reberegnPerioderForOpphør(
        behandling: Saksbehandling,
        avkortetVedtaksperioder: List<Vedtaksperiode>,
        beregningsresultatTilReberegning: BeregningsresultatForMåned,
    ): List<BeregningsresultatForMåned> {
        val stønadsperioder = hentStønadsperioder(behandling.id)
        val vedtaksperioderSomOmregnes =
            vedtaksperioderInnenforLøpendeMåned(avkortetVedtaksperioder, beregningsresultatTilReberegning)

        val reberegnedePerioder = beregn(behandling, vedtaksperioderSomOmregnes, stønadsperioder)
        feilHvisIkke(reberegnedePerioder.size <= 1) {
            "Når vi reberegner vedtaksperioder innenfor en måned burde vi få maks 1 reberegnet periode, faktiskAntall=${reberegnedePerioder.size}"
        }

        return reberegnedePerioder.map {
            it.medKorrigertUtbetalingsdato(beregningsresultatTilReberegning.grunnlag.utbetalingsdato)
        }
    }

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
        forrigeVedtak: InnvilgelseEllerOpphørLæremidler,
    ): BeregningsresultatLæremidler {
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

        return BeregningsresultatLæremidler(
            perioder = perioderFraForrigeVedtakSomSkalBeholdes + nyePerioderMedKorrigertUtbetalingsdato,
        )
    }

    private fun korrigerUtbetalingsdato(
        nyePerioder: List<BeregningsresultatForMåned>,
        forrigeBeregningsresultat: BeregningsresultatLæremidler,
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

    private fun hentForrigeVedtak(behandling: Saksbehandling): InnvilgelseEllerOpphørLæremidler? =
        behandling.forrigeIverksatteBehandlingId?.let { hentVedtak(it) }?.data

    private fun hentVedtak(behandlingId: BehandlingId) =
        vedtakRepository
            .findByIdOrThrow(behandlingId)
            .withTypeOrThrow<InnvilgelseEllerOpphørLæremidler>()

    private fun hentStønadsperioder(behandlingId: BehandlingId): List<StønadsperiodeBeregningsgrunnlag> =
        stønadsperiodeRepository
            .findAllByBehandlingId(behandlingId)
            .tilSortertStønadsperiodeBeregningsgrunnlag()
            .slåSammenSammenhengende()

    private fun finnAktiviteter(behandlingId: BehandlingId): List<AktivitetLæremidlerBeregningGrunnlag> =
        vilkårperiodeRepository
            .findByBehandlingIdAndResultat(behandlingId, ResultatVilkårperiode.OPPFYLT)
            .tilAktiviteter()

    private fun beregnLæremidlerPerMåned(
        vedtaksperioder: List<Vedtaksperiode>,
        stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>,
        aktiviteter: List<AktivitetLæremidlerBeregningGrunnlag>,
    ): List<BeregningsresultatForMåned> =
        vedtaksperioder
            .sorted()
            .splittTilLøpendeMåneder()
            .map { it.tilUtbetalingPeriode(stønadsperioder, aktiviteter) }
            .beregn()

    private fun List<UtbetalingPeriode>.beregn(): List<BeregningsresultatForMåned> =
        this
            .map { utbetalingPeriode ->
                val grunnlagsdata = lagBeregningsGrunnlag(periode = utbetalingPeriode)

                BeregningsresultatForMåned(
                    beløp = beregnBeløp(grunnlagsdata.sats, grunnlagsdata.studieprosent),
                    grunnlag = grunnlagsdata,
                )
            }

    private fun lagBeregningsGrunnlag(periode: UtbetalingPeriode): Beregningsgrunnlag {
        val sats = finnSatsForPeriode(periode)

        return Beregningsgrunnlag(
            fom = periode.fom,
            tom = periode.tom,
            studienivå = periode.studienivå,
            studieprosent = periode.prosent,
            sats = finnSatsForStudienivå(sats, periode.studienivå),
            satsBekreftet = sats.bekreftet,
            utbetalingsdato = periode.utbetalingsdato,
            målgruppe = periode.målgruppe,
            aktivitet = periode.aktivitet,
        )
    }
}
