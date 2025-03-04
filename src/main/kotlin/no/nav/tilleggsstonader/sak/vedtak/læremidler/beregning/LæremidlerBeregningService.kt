package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.domain.slåSammenSammenhengende
import no.nav.tilleggsstonader.sak.vedtak.domain.tilSortertStønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregnBeløpUtil.beregnBeløp
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregnUtil.grupperVedtaksperioderPerLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.LæremidlerVedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
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
     * Beregning av læremidler har foreløpig noen begrensninger.
     * Vi antar kun en aktivitet gjennom hele vedtaksperioden
     * Vi antar kun en stønadsperiode per vedtaksperiode for å kunne velge ut rett målgruppe
     * Vi antar at satsen ikke endrer seg i vedtaksperioden
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

        val aktiviteter = finnAktiviteter(behandling.id)
        val beregningsresultatForMåned = beregnLæremidlerPerMåned(vedtaksperioder, stønadsperioder, aktiviteter)

        return if (forrigeVedtak != null) {
            val revurderFra = behandling.revurderFra
            feilHvis(revurderFra == null) { "Behandling=$behandling mangler revurderFra" }
            BeregningsresultatLæremidler(beregningsresultatForMåned)
        } else {
            BeregningsresultatLæremidler(beregningsresultatForMåned)
        }
    }

    private fun hentForrigeVedtak(saksbehandling: Saksbehandling): InnvilgelseEllerOpphørLæremidler? =
        saksbehandling.forrigeBehandlingId
            ?.let {
                vedtakRepository.findByIdOrThrow(it).withTypeOrThrow<InnvilgelseEllerOpphørLæremidler>()
            }?.data

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
            .grupperVedtaksperioderPerLøpendeMåned()
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
