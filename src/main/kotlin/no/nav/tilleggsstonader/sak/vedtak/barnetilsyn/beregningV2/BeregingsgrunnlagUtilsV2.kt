package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV2

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
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.YearMonth

object BeregingsgrunnlagUtilsV2 {
    fun lagBeregningsgrunnlagPerMåned(
        vedtaksperioder: List<VedtaksperiodeDto>,
        aktiviteter: List<Aktivitet>,
        utgifterPerBarn: Map<BarnId, List<UtgiftBeregning>>,
    ): List<Beregningsgrunnlag> {
        // Denne tar foreløbig kun hele månender. Må tillate at vedtaksperioder er fra en gitt dag i måneden
        // VedtaksperiodeGrunnlag er ny, ellers er denne ganske lik som tidligere

        val vedtaksperioderPerMåned: Map<YearMonth, List<VedtaksperiodeDto>> = vedtaksperioder.tilÅrMåned()
        val utgifterPerMåned = utgifterPerBarn.tilÅrMåned()
        val aktiviteterPerMånedPerType = aktiviteter.tilAktiviteterPerMånedPerType()

        return vedtaksperioderPerMåned.entries.mapNotNull { (måned, vedtaksperioder) ->
            val aktiviteterForMåned = aktiviteterPerMånedPerType[måned] ?: error("Ingen aktiviteter for måned $måned")
            utgifterPerMåned[måned]?.let { utgifter ->
                val antallBarn = utgifter.map { it.barnId }.toSet().size
                val makssats = finnMakssats(måned, antallBarn)
                Beregningsgrunnlag(
                    måned = måned,
                    makssats = makssats,
                    stønadsperioderGrunnlag = emptyList(),
                    vedtaksperioderGrunnlag = lagVedtaksperiodeGrunnlag(vedtaksperioder, aktiviteterForMåned),
                    utgifter = utgifter,
                    utgifterTotal = utgifter.sumOf { it.utgift },
                    antallBarn = antallBarn,
                )
            }
        }
    }

    fun lagVedtaksperiodeGrunnlag(
        vedtaksperioder: List<VedtaksperiodeDto>,
        aktiviteterForMåned: Map<AktivitetType, List<Aktivitet>>,
    ): List<VedtaksperiodeGrunnlag> {
        val aktiviteterPerUkePerAktivitet = aktiviteterForMåned.map { it.key to it.value.tilDagerPerUke() }.toMap()

        return vedtaksperioder.map { vedtaksperiode ->
            val aktivitetsDager =
                finnAntallAktivitestdagerIVedtaksperioden(
                    vedtaksperiode = vedtaksperiode,
                    aktiviteterPerType = aktiviteterPerUkePerAktivitet,
                )

            VedtaksperiodeGrunnlag(
                vedtaksperiodeDto = vedtaksperiode,
                aktiviteter = aktiviteterForMåned.values.flatten(),
                antallAktivitetsDager = aktivitetsDager,
            )
        }
    }

    // TODO denne finnes også i V1 service som `antallDager`. Kan vi generalisere disse så vi kun trenger en av de?
    private fun finnAntallAktivitestdagerIVedtaksperioden(
        vedtaksperiode: VedtaksperiodeDto,
        aktiviteterPerType: Map<AktivitetType, Map<Uke, List<PeriodeMedDager>>>,
    ): Int {
        val vedtaksperiodeUker = vedtaksperiode.tilUke()
        val aktiviteterPerUke =
            aktiviteterPerType[vedtaksperiode.aktivitetType]
                ?: error("Finner ikke aktiviteter for ${vedtaksperiode.aktivitetType}")

        return vedtaksperiodeUker
            .map { (uke, periode) ->
                val aktiviteterForUke =
                    aktiviteterPerUke[uke]
                        ?: error("Ingen aktivitet i uke fom=${uke.fom} og tom=${uke.tom}")

                BeregningsgrunnlagUtilsFelles.beregnAntallAktivitetsdagerForUke(periode, aktiviteterForUke)
            }.sum()
    }
}
