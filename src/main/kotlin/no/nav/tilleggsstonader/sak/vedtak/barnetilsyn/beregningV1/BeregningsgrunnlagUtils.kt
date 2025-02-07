package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV1

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.BeregningsgrunnlagUtilsFelles
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.PeriodeMedDager
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBeregningUtilsFelles.tilAktiviteterPerMånedPerType
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBeregningUtilsFelles.tilDagerPerUke
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBeregningUtilsFelles.tilUke
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBeregningUtilsFelles.tilÅrMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.Uke
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.UtgiftBeregning
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.finnMakssats
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.StønadsperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType

object BeregningsgrunnlagUtils {
    fun lagBeregningsgrunnlagPerMåned(
        stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>,
        aktiviteter: List<Aktivitet>,
        utgifterPerBarn: Map<BarnId, List<UtgiftBeregning>>,
    ): List<Beregningsgrunnlag> {
        val stønadsperioderPerMåned = stønadsperioder.tilÅrMåned()
        val utgifterPerMåned = utgifterPerBarn.tilÅrMåned()
        val aktiviteterPerMånedPerType = aktiviteter.tilAktiviteterPerMånedPerType()

        return stønadsperioderPerMåned.entries.mapNotNull { (måned, stønadsperioder) ->
            val aktiviteterForMåned = aktiviteterPerMånedPerType[måned] ?: error("Ingen aktiviteter for måned $måned")
            utgifterPerMåned[måned]?.let { utgifter ->
                val antallBarn = utgifter.map { it.barnId }.toSet().size
                val makssats = finnMakssats(måned, antallBarn)
                val stønadsperioderGrunnlag = finnStønadsperioderMedAktiviteter(stønadsperioder, aktiviteterForMåned)
                Beregningsgrunnlag(
                    måned = måned,
                    makssats = makssats,
                    stønadsperioderGrunnlag = stønadsperioderGrunnlag,
                    utgifter = utgifter,
                    utgifterTotal = utgifter.sumOf { it.utgift },
                    antallBarn = antallBarn,
                )
            }
        }
    }

    private fun finnStønadsperioderMedAktiviteter(
        stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>,
        aktiviteter: Map<AktivitetType, List<Aktivitet>>,
    ): List<StønadsperiodeGrunnlag> {
        val aktiviteterPerUke = aktiviteter.map { it.key to it.value.tilDagerPerUke() }.toMap()

        return stønadsperioder.map { stønadsperiode ->
            val relevanteAktiviteter = finnAktiviteterForStønadsperiode(stønadsperiode, aktiviteter)

            StønadsperiodeGrunnlag(
                stønadsperiode = stønadsperiode,
                aktiviteter = relevanteAktiviteter,
                antallDager = antallDager(stønadsperiode, aktiviteterPerUke),
            )
        }
    }

    private fun finnAktiviteterForStønadsperiode(
        stønadsperiode: StønadsperiodeBeregningsgrunnlag,
        aktiviteter: Map<AktivitetType, List<Aktivitet>>,
    ): List<Aktivitet> =
        aktiviteter[stønadsperiode.aktivitet]?.filter { it.overlapper(stønadsperiode) }
            ?: error(
                "Finnes ingen aktiviteter av type ${stønadsperiode.aktivitet} som passer med stønadsperiode med fom=${stønadsperiode.fom} og tom=${stønadsperiode.tom}",
            )

    private fun antallDager(
        stønadsperiode: StønadsperiodeBeregningsgrunnlag,
        aktiviteterPerType: Map<AktivitetType, Map<Uke, List<PeriodeMedDager>>>,
    ): Int {
        val stønadsperioderUker = stønadsperiode.tilUke()
        val aktiviteterPerUke =
            aktiviteterPerType[stønadsperiode.aktivitet]
                ?: error("Finner ikke aktiviteter for ${stønadsperiode.aktivitet}")

        return stønadsperioderUker
            .map { (uke, periode) ->
                val aktiviteterForUke =
                    aktiviteterPerUke[uke]
                        ?: error("Ingen aktivitet i uke fom=${uke.fom} og tom=${uke.tom}")

                BeregningsgrunnlagUtilsFelles.beregnAntallAktivitetsdagerForUke(periode, aktiviteterForUke)
            }.sum()
    }
}
