package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapper
import no.nav.tilleggsstonader.kontrakter.felles.påfølgesAv
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
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
        val stønadsperioder =
            stønadsperiodeRepository.findAllByBehandlingId(behandlingId).tilSortertGrunnlagStønadsperiode()

        validerVedtaksperioder(vedtaksperioder, stønadsperioder)

        val aktiviteter = finnAktiviteter(behandlingId)

        val beregningsresultatForMåned = vedtaksperioder.flatMap { vedtaksperiode ->
            val utbetalingsperioder = vedtaksperiode.delTilUtbetalingsPerioder()

            utbetalingsperioder.map { utbetalingsperiode ->
                val relevantStønadsperiode = utbetalingsperiode.finnRelevantStønadsperiode(stønadsperioder)

                lagBeregningsresultatForMåned(
                    utbetalingsperiode = utbetalingsperiode,
                    stønadsperiode = relevantStønadsperiode,
                    aktivitet = utbetalingsperiode.finnRelevantAktivitet(
                        aktiviteter = aktiviteter,
                        aktivitetType = relevantStønadsperiode.aktivitet,
                    ),
                )
            }
        }

        return BeregningsresultatLæremidler(beregningsresultatForMåned)
    }

    private fun lagBeregningsresultatForMåned(
        utbetalingsperiode: UtbetalingsPeriode,
        stønadsperiode: Stønadsperiode,
        aktivitet: Aktivitet,
    ): BeregningsresultatForMåned {
        val grunnlagsdata =
            lagBeregningsGrunnlag(
                periode = utbetalingsperiode,
                aktivitet = aktivitet,
                målgruppe = stønadsperiode.målgruppe,
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

    private fun validerVedtaksperioderOld(
        vedtaksperioder: List<Vedtaksperiode>,
        stønadsperioder: List<Stønadsperiode>,
    ) {
        feilHvis(vedtaksperioder.overlapper()) {
            "Vedtaksperioder overlapper"
        }

        val sammenslåtteStønadsperioder = stønadsperioder
            .mergeSammenhengende(
                skalMerges = { a, b -> a.påfølgesAv(b) },
                merge = { a, b -> a.copy(tom = b.tom) },
            )

        feilHvis(
            vedtaksperioder.any { vedtaksperiode ->
                sammenslåtteStønadsperioder.none { it.inneholder(vedtaksperiode) }
            },
        ) {
            "Vedtaksperiode er ikke innenfor en stønadsperiode"
        }
    }
}
