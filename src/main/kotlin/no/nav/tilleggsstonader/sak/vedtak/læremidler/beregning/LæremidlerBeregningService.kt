package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.periode.AvkortResult
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.util.sisteDagenILøpendeMåned
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregnBeløpUtil.beregnBeløp
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregnUtil.splittTilLøpendeMåneder
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeUtil.vedtaksperioderInnenforLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.avkortBeregningsresultatVedOpphør
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class LæremidlerBeregningService(
    private val vilkårperiodeRepository: VilkårperiodeRepository,
    private val vedtaksperiodeValideringService: VedtaksperiodeValideringService,
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
        tidligsteEndring: LocalDate?,
    ): BeregningsresultatLæremidler {
        vedtaksperiodeValideringService.validerVedtaksperioderLæremidler(
            vedtaksperioder = vedtaksperioder,
            behandling = behandling,
            typeVedtak = TypeVedtak.INNVILGELSE,
            tidligsteEndring = tidligsteEndring,
        )

        val vedtaksperioderBeregningsgrunnlag = vedtaksperioder.tilBeregningsgrunnlag()
        val forrigeVedtak = hentForrigeVedtak(behandling)

        val beregningsresultatForMåned = beregn(behandling, vedtaksperioderBeregningsgrunnlag)

        return if (forrigeVedtak != null) {
            settSammenGamleOgNyePerioder(behandling, beregningsresultatForMåned, forrigeVedtak, behandling.revurderFra ?: tidligsteEndring)
        } else {
            BeregningsresultatLæremidler(beregningsresultatForMåned)
        }
    }

    private fun beregn(
        behandling: Saksbehandling,
        vedtaksperioderBeregningsgrunnlag: List<VedtaksperiodeBeregning>,
    ): List<BeregningsresultatForMåned> {
        val aktiviteter = finnAktiviteter(behandling.id)
        return beregnLæremidlerPerMåned(vedtaksperioderBeregningsgrunnlag, aktiviteter)
    }

    fun beregnForOpphør(
        behandling: Saksbehandling,
        avkortetVedtaksperioder: List<Vedtaksperiode>,
        opphørsdato: LocalDate,
    ): BeregningsresultatLæremidler {
        feilHvis(behandling.forrigeIverksatteBehandlingId == null) {
            "Opphør er et ugyldig vedtaksresultat fordi behandlingen er en førstegangsbehandling"
        }
        val forrigeVedtak = hentVedtak(behandling.forrigeIverksatteBehandlingId)
        val avkortetBeregningsresultat = avkortBeregningsresultatVedOpphør(forrigeVedtak, opphørsdato)

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
        val vedtaksperioderForGrunnlag = avkortetVedtaksperioder.tilBeregningsgrunnlag()
        val vedtaksperioderSomOmregnes =
            vedtaksperioderInnenforLøpendeMåned(vedtaksperioderForGrunnlag, beregningsresultatTilReberegning)

        val reberegnedePerioder = beregn(behandling, vedtaksperioderSomOmregnes)
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
        tidligsteEndring: LocalDate?,
    ): BeregningsresultatLæremidler {
        feilHvis(tidligsteEndring == null) { "Behandling=$saksbehandling mangler revurderFra eller dato for tidligste endring" }

        val forrigeBeregningsresultat = forrigeVedtak.beregningsresultat

        val perioderFraForrigeVedtakSomSkalBeholdes =
            forrigeBeregningsresultat
                .perioder
                .filter { it.grunnlag.fom.sisteDagenILøpendeMåned() < tidligsteEndring }
                .map { it.markerSomDelAvTidligereUtbetaling(delAvTidligereUtbetaling = true) }
        val nyePerioder =
            beregningsresultat
                .filter { it.grunnlag.fom.sisteDagenILøpendeMåned() >= tidligsteEndring }

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

    private fun List<Vedtaksperiode>.tilBeregningsgrunnlag() =
        this
            .map {
                VedtaksperiodeBeregning(
                    fom = it.fom,
                    tom = it.tom,
                    målgruppe = it.målgruppe,
                    aktivitet = it.aktivitet,
                )
            }.sorted()

    private fun finnAktiviteter(behandlingId: BehandlingId): List<AktivitetLæremidlerBeregningGrunnlag> =
        vilkårperiodeRepository
            .findByBehandlingIdAndResultat(behandlingId, ResultatVilkårperiode.OPPFYLT)
            .tilAktiviteter()

    private fun beregnLæremidlerPerMåned(
        vedtaksperioderBeregningsgrunnlag: List<VedtaksperiodeBeregning>,
        aktiviteter: List<AktivitetLæremidlerBeregningGrunnlag>,
    ): List<BeregningsresultatForMåned> =
        vedtaksperioderBeregningsgrunnlag
            .sorted()
            .splittTilLøpendeMåneder()
            .map { it.tilUtbetalingPeriode(vedtaksperioderBeregningsgrunnlag, aktiviteter) }
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
