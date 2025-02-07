package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBeregningUtilsFelles.tilAktiviteterPerMånedPerType
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBeregningUtilsFelles.tilDagerPerUke
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBeregningUtilsFelles.tilUke
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningFelles.TilsynBeregningUtilsFelles.tilÅrMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.StønadsperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType

object BeregningsgrunnlagUtilsFelles {
    fun lagBeregningsgrunnlagPerMåned(
        perioder: List<TilsynBarnBeregningObjekt>,
        aktiviteter: List<Aktivitet>,
        utgifterPerBarn: Map<BarnId, List<UtgiftBeregning>>,
    ): List<Beregningsgrunnlag> {
        val perioderPerMåned = perioder.tilÅrMåned()
        val utgifterPerMåned = utgifterPerBarn.tilÅrMåned()
        val aktiviteterPerMånedPerType = aktiviteter.tilAktiviteterPerMånedPerType()

        return perioderPerMåned.entries.mapNotNull { (måned, perioder) ->
            val aktiviteterForMåned = aktiviteterPerMånedPerType[måned] ?: error("Ingen aktiviteter for måned $måned")
            utgifterPerMåned[måned]?.let { utgifter ->
                val antallBarn = utgifter.map { it.barnId }.toSet().size
                val makssats = finnMakssats(måned, antallBarn)
                Beregningsgrunnlag(
                    måned = måned,
                    makssats = makssats,
                    stønadsperioderGrunnlag = finnPerioderMedAktiviteter(perioder, aktiviteterForMåned),
                    utgifter = utgifter,
                    utgifterTotal = utgifter.sumOf { it.utgift },
                    antallBarn = antallBarn,
                )
            }
        }
    }

    private fun finnPerioderMedAktiviteter(
        stønadsperioder: List<TilsynBarnBeregningObjekt>,
        aktiviteter: Map<AktivitetType, List<Aktivitet>>,
    ): List<StønadsperiodeGrunnlag> {
        val aktiviteterPerUke = aktiviteter.map { it.key to it.value.tilDagerPerUke() }.toMap()

        return stønadsperioder.map { stønadsperiode ->
            val relevanteAktiviteter = finnAktiviteterForPeriode(stønadsperiode, aktiviteter)

            StønadsperiodeGrunnlag(
                stønadsperiode = stønadsperiode,
                aktiviteter = relevanteAktiviteter,
                antallDager = finnAntallAktivitestdagerIVedtaksperioden(stønadsperiode, aktiviteterPerUke),
            )
        }
    }

    private fun finnAktiviteterForPeriode(
        stønadsperiode: TilsynBarnBeregningObjekt,
        aktiviteter: Map<AktivitetType, List<Aktivitet>>,
    ): List<Aktivitet> =
        aktiviteter[stønadsperiode.aktivitet]?.filter { it.overlapper(stønadsperiode) }
            ?: error(
                "Finnes ingen aktiviteter av type ${stønadsperiode.aktivitet} som passer med stønadsperiode med fom=${stønadsperiode.fom} og tom=${stønadsperiode.tom}",
            )

    private fun finnAntallAktivitestdagerIVedtaksperioden(
        vedtaksperiode: TilsynBarnBeregningObjekt,
        aktiviteterPerType: Map<AktivitetType, Map<Uke, List<PeriodeMedDager>>>,
    ): Int {
        val vedtaksperiodeUker = vedtaksperiode.tilUke()
        val aktiviteterPerUke =
            aktiviteterPerType[vedtaksperiode.aktivitet]
                ?: error("Finner ikke aktiviteter for ${vedtaksperiode.aktivitet}")

        return vedtaksperiodeUker
            .map { (uke, periode) ->
                val aktiviteterForUke =
                    aktiviteterPerUke[uke]
                        ?: error("Ingen aktivitet i uke fom=${uke.fom} og tom=${uke.tom}")

                beregnAntallAktivitetsdagerForUke(periode, aktiviteterForUke)
            }.sum()
    }

    /**
     * Beregner antall dager per uke som kan brukes
     * Hvis antall dager fra stønadsperiode er 1, så kan man maks bruke 1 dag fra aktiviteter
     * Hvis antall dager fra stønadsperiode er 5, men aktiviteter kun har 2 dager så kan man kun bruke 2 dager
     */
    private fun beregnAntallAktivitetsdagerForUke(
        periodeMedDager: PeriodeMedDager,
        aktiviteter: List<PeriodeMedDager>,
    ): Int =
        aktiviteter.filter { it.overlapper(periodeMedDager) }.fold(0) { acc, aktivitet ->
            // Tilgjengelige dager i uke i overlapp mellom stønadsperiode og aktivitet
            val antallTilgjengeligeDager = minOf(periodeMedDager.antallDager, aktivitet.antallDager)

            trekkFraBrukteDager(periodeMedDager, aktivitet, antallTilgjengeligeDager)

            acc + antallTilgjengeligeDager
        }

    /**
     * Skal ikke kunne bruke en dager fra aktivitet eller stønadsperiode flere ganger.
     * Trekker fra tilgjengelige dager fra antallDager
     * Dersom stønadsperioden har 2 dager, og aktiviten 3 dager, så skal man kun totalt kunne bruke 2 dager
     * Dersom stønadsperioden har 3 dager, og aktiviten 2 dager, så skal man kun totalt kunne bruke 2 dager
     */
    private fun trekkFraBrukteDager(
        stønadsperiode: PeriodeMedDager,
        aktivitet: PeriodeMedDager,
        antallTilgjengeligeDager: Int,
    ) {
        aktivitet.antallDager -= antallTilgjengeligeDager
        stønadsperiode.antallDager -= antallTilgjengeligeDager
    }
}
