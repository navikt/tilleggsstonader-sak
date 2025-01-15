package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.påfølgesAv
import no.nav.tilleggsstonader.kontrakter.felles.sisteDagIÅret
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerPeriodeUtil.delVedtaksperiodePerÅr
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerPeriodeUtil.sisteDagenILøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerPeriodeUtil.splitPerLøpendeMåneder
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeUtil.ofType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfFaktaOrThrow
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

object LæremidlerBeregningUtil {

    fun List<StønadsperiodeBeregningsgrunnlag>.slåSammenSammenhengende(): List<StønadsperiodeBeregningsgrunnlag> =
        mergeSammenhengende(
            skalMerges = { a, b -> a.påfølgesAv(b) && a.målgruppe == b.målgruppe && a.aktivitet == b.aktivitet },
            merge = { a, b -> a.copy(tom = b.tom) },
        )

    fun beregnBeløp(sats: Int, studieprosent: Int): Int {
        val PROSENT_50 = BigDecimal(0.5)
        val PROSENTGRENSE_HALV_SATS = 50

        if (studieprosent <= PROSENTGRENSE_HALV_SATS) {
            return BigDecimal(sats).multiply(PROSENT_50).setScale(0, RoundingMode.HALF_UP).toInt()
        }
        return sats
    }

    fun List<Vedtaksperiode>.grupperVedtaksperioderPerLøpendeMåned(): List<GrunnlagForUtbetalingPeriode> = this
        .sorted()
        .delVedtaksperiodePerÅr()
        .fold(listOf<GrunnlagForUtbetalingPeriode>()) { acc, vedtaksperiode ->
            if (acc.isEmpty()) {
                val nyeUtbetalingsperioder = vedtaksperiode.delTilUtbetalingPerioder()
                acc + nyeUtbetalingsperioder
            } else {
                val håndterNyUtbetalingsperiode = vedtaksperiode.håndterNyUtbetalingsperiode(acc)
                acc + håndterNyUtbetalingsperiode
            }
        }.toList()

    fun List<GrunnlagForUtbetalingPeriode>.tilUtbetalingPeriode(
        stønadsperioder: List<StønadsperiodeBeregningsgrunnlag>,
        aktiviteter: List<Aktivitet>
    ): List<UtbetalingPeriode> = this
        .map { it.finnMålgruppeOgAktivitet(stønadsperioder, aktiviteter) }


    /**
     * Legger til periode som overlapper med forrige utbetalingsperiode
     * Returnerer utbetalingsperioder som løper etter forrige utbetalingsperiode
     */
    private fun VedtaksperiodeDeltForÅr.håndterNyUtbetalingsperiode(
        acc: List<GrunnlagForUtbetalingPeriode>,
    ): List<GrunnlagForUtbetalingPeriode> {
        val forrigeUtbetalingsperide = acc.last()
        this.overlappendeDelMed(forrigeUtbetalingsperide)?.let {
            forrigeUtbetalingsperide.medVedtaksperiode(it)
        }
        return this
            .delEtterUtbetalingsperiode(forrigeUtbetalingsperide)
            .delTilUtbetalingPerioder()
    }

    /**
     * Splitter en vedtaksperiode i forrige utbetalingsperiode hvis de overlapper
     */
    private fun VedtaksperiodeDeltForÅr.overlappendeDelMed(utbetalingPeriode: GrunnlagForUtbetalingPeriode): Vedtaksperiode? {
        return if (this.fom <= utbetalingPeriode.tom) {
            Vedtaksperiode(
                fom = utbetalingPeriode.fom,
                tom = minOf(utbetalingPeriode.tom, this.tom),
            )
        } else {
            null
        }
    }

    /**
     * Splitter vedtaksperiode som løper etter forrige utbetalingsperiode til nye vedtaksperioder
     */
    private fun VedtaksperiodeDeltForÅr.delEtterUtbetalingsperiode(
        utbetalingPeriode: GrunnlagForUtbetalingPeriode,
    ): VedtaksperiodeDeltForÅr = this.copy(fom = maxOf(this.fom, utbetalingPeriode.tom.plusDays(1)))

    /**
     * tom settes til minOf tom og årets tom for å håndtere at den ikke går over 2 år
     */
    private fun VedtaksperiodeDeltForÅr.delTilUtbetalingPerioder(): List<GrunnlagForUtbetalingPeriode> {
        return this.splitPerLøpendeMåneder { fom, tom ->
            GrunnlagForUtbetalingPeriode(
                fom = fom,
                tom = minOf(this.fom.sisteDagenILøpendeMåned(), this.tom.sisteDagIÅret()),
                utbetalingsdato = this.fom.datoEllerNesteMandagHvisLørdagEllerSøndag(),
            ).medVedtaksperiode(Vedtaksperiode(fom = fom, tom = tom))
        }
    }
}

data class Aktivitet(
    val id: UUID,
    val type: AktivitetType,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val prosent: Int,
    val studienivå: Studienivå,
) : Periode<LocalDate>

fun List<Vilkårperiode>.tilAktiviteter(): List<Aktivitet> =
    ofType<AktivitetLæremidler>()
        .map {
            val fakta = it.faktaOgVurdering.fakta
            Aktivitet(
                id = it.id,
                type = it.faktaOgVurdering.type.vilkårperiodeType,
                fom = it.fom,
                tom = it.tom,
                prosent = fakta.takeIfFaktaOrThrow<FaktaAktivitetLæremidler>().prosent,
                studienivå = fakta.takeIfFaktaOrThrow<FaktaAktivitetLæremidler>().studienivå!!,
            )
        }
