package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregningV2

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.BeregningsgrunnlagUtils
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.PeriodeMedDager
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.tilAktiviteterPerMånedPerType
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.tilDagerPerUke
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBeregningUtil.tilÅrMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.Uke
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.UtgiftBeregning
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.finnMakssats
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.tilÅrMåned
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
    ): List<VedtaksperiodeGrunnlag> =
        vedtaksperioder.map { vedtaksperiode ->
            val aktiviteterPerUkePerAktivitet = aktiviteterForMåned.map { it.key to it.value.tilDagerPerUke() }.toMap()
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

    // Logikk kopiert fra V1, men oppdatert typer
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

                BeregningsgrunnlagUtils.beregnAntallAktivitetsdagerForUke(periode, aktiviteterForUke)
            }.sum()
    }
}
