package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.arena.sak.Målgruppe
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.påfølgesAv
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.tilSortertGrunnlagStønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregningUtil.beregnBeløp
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregningUtil.delTilUtbetalingsPerioder
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeUtil.validerVedtaksperioder
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.springframework.stereotype.Service

@Service
class LæremidlerBeregningService(
    private val vilkårperiodeRepository: VilkårperiodeRepository,
    private val stønadsperiodeRepository: StønadsperiodeRepository,
) {
    /**
     * Beregning av læremidler er foreløpig kun basert på antakelser.
     * Nå beregnes det med hele måneder, splittet i månedsskifte, men dette er ikke avklart som korrekt.
     * Det er ikke tatt hensyn til begrensninger på semester og maks antall måneder i et år som skal dekkes.
     */

    // ANTAR: En aktitivet hele vedtaksperioden
    // ANTAR: En stønadsperiode per vedtaksperiode
    // ANTAR: Sats ikke endrer seg i perioden
    fun beregn(vedtaksperioder: List<Vedtaksperiode>, behandlingId: BehandlingId): BeregningsresultatLæremidler {
        val stønadsperioder = hentStønadsperioder(behandlingId).slåSammenSammenhengende()

        validerVedtaksperioder(vedtaksperioder, stønadsperioder)

        val aktiviteter = finnAktiviteter(behandlingId)
        val beregningsresultatForMåned = beregnLæremidlerPerMåned(vedtaksperioder, stønadsperioder, aktiviteter)

        return BeregningsresultatLæremidler(beregningsresultatForMåned)
    }

    private fun hentStønadsperioder(behandlingId: BehandlingId): List<Stønadsperiode> =
        stønadsperiodeRepository.findAllByBehandlingId(behandlingId).tilSortertGrunnlagStønadsperiode()

    private fun List<Stønadsperiode>.slåSammenSammenhengende(): List<Stønadsperiode> =
        mergeSammenhengende(
            skalMerges = { a, b -> a.påfølgesAv(b) && a.målgruppe == b.målgruppe && a.aktivitet == b.aktivitet },
            merge = { a, b -> a.copy(tom = b.tom) },
        )

    private fun beregnLæremidlerPerMåned(
        vedtaksperioder: List<Vedtaksperiode>,
        stønadsperioder: List<Stønadsperiode>,
        aktiviteter: List<Aktivitet>,
    ): List<BeregningsresultatForMåned> =
        vedtaksperioder.flatMap { it.delTilUtbetalingsPerioder() }
            .map { utbetalingsperiode ->
                val relevantStønadsperiode = utbetalingsperiode.finnRelevantStønadsperiode(stønadsperioder)
                lagBeregningsresultatForMåned(
                    utbetalingsperiode = utbetalingsperiode,
                    målgruppe = relevantStønadsperiode.målgruppe,
                    aktivitet = utbetalingsperiode.finnRelevantAktivitet(
                        aktiviteter = aktiviteter,
                        aktivitetType = relevantStønadsperiode.aktivitet,
                    ),
                )
            }

    private fun lagBeregningsresultatForMåned(
        utbetalingsperiode: UtbetalingsPeriode,
        målgruppe: MålgruppeType,
        aktivitet: Aktivitet,
    ): BeregningsresultatForMåned {
        val grunnlagsdata =
            lagBeregningsGrunnlag(
                periode = utbetalingsperiode,
                aktivitet = aktivitet,
                målgruppe = målgruppe,
            )

        return BeregningsresultatForMåned(
            beløp = beregnBeløp(grunnlagsdata.sats, grunnlagsdata.studieprosent),
            grunnlag = grunnlagsdata,
        )
    }

    private fun lagBeregningsGrunnlag(
        periode: UtbetalingsPeriode,
        aktivitet: Aktivitet,
        målgruppe: MålgruppeType,
    ): Beregningsgrunnlag {
        return Beregningsgrunnlag(
            fom = periode.fom,
            tom = periode.tom,
            studienivå = aktivitet.studienivå,
            studieprosent = aktivitet.prosent,
            sats = finnSatsForStudienivå(periode, aktivitet.studienivå),
            utbetalingsMåned = periode.utbetalingsMåned,
            målgruppe = målgruppe,
        )
    }

    private fun finnAktiviteter(behandlingId: BehandlingId): List<Aktivitet> {
        return vilkårperiodeRepository.findByBehandlingIdAndResultat(behandlingId, ResultatVilkårperiode.OPPFYLT)
            .tilAktiviteter()
    }
}
