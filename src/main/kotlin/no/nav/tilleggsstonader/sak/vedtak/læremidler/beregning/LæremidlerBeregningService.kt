package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.tilSortertStønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregningUtil.beregnBeløp
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregningUtil.grupperVedtaksperioderPerLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregningUtil.slåSammenSammenhengende
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregningUtil.tilUtbetalingPeriode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeUtil.validerVedtaksperioder
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.springframework.stereotype.Service

@Service
class LæremidlerBeregningService(
    private val vilkårperiodeRepository: VilkårperiodeRepository,
    private val stønadsperiodeRepository: StønadsperiodeRepository,
) {
    /**
     * Beregning av læremidler har foreløpig noen begrensninger.
     * Vi antar kun en aktivitet gjennom hele vedtaksperioden
     * Vi antar kun en stønadsperiode per vedtaksperiode for å kunne velge ut rett målgruppe
     * Vi antar at satsen ikke endrer seg i vedtaksperioden
     * Programmet kaster feil dersom antagelsene ikke stemmer
     */

    fun beregn(vedtaksperioder: List<Vedtaksperiode>, behandlingId: BehandlingId): BeregningsresultatLæremidler {
        val stønadsperioder = hentStønadsperioder(behandlingId).slåSammenSammenhengende()

        validerVedtaksperioder(vedtaksperioder, stønadsperioder)

        val aktiviteter = finnAktiviteter(behandlingId)
        val beregningsresultatForMåned = beregnLæremidlerPerMåned(vedtaksperioder, stønadsperioder, aktiviteter)

        return BeregningsresultatLæremidler(beregningsresultatForMåned)
    }

    private fun hentStønadsperioder(behandlingId: BehandlingId): List<StønadsperiodeBeregningsgrunnlag> =
        stønadsperiodeRepository.findAllByBehandlingId(behandlingId).tilSortertStønadsperiodeBeregningsgrunnlag()

    private fun finnAktiviteter(behandlingId: BehandlingId): List<Aktivitet> {
        return vilkårperiodeRepository.findByBehandlingIdAndResultat(behandlingId, ResultatVilkårperiode.OPPFYLT)
            .tilAktiviteter()
    }

    private fun beregnLæremidlerPerMåned(
        vedtaksperioder: List<Vedtaksperiode>,
        stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>,
        aktiviteter: List<Aktivitet>,
    ): List<BeregningsresultatForMåned> =
        vedtaksperioder
            .sorted()
            .grupperVedtaksperioderPerLøpendeMåned()
            .tilUtbetalingPeriode(stønadsperioder, aktiviteter)
            .beregn()

    private fun List<UtbetalingPeriode>.beregn(): List<BeregningsresultatForMåned> = this
        .map { periode ->
            val grunnlagsdata = lagBeregningsGrunnlag(periode)

            BeregningsresultatForMåned(
                beløp = beregnBeløp(grunnlagsdata.sats, grunnlagsdata.studieprosent),
                grunnlag = grunnlagsdata,
            )
        }

    private fun lagBeregningsGrunnlag(
        periode: UtbetalingPeriode,
    ): Beregningsgrunnlag {
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
        )
    }
}
