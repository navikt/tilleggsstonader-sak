package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.util.tilSisteDagIMåneden
import no.nav.tilleggsstonader.sak.util.toYearMonth
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
    fun Periode<LocalDate>.delTilUtbetalingsPerioder(): List<UtbetalingsPeriode> {
        return delIÅr { fom, tom -> Vedtaksperiode(fom, tom) }
            .flatMap { periode ->
                periode.delIDatoTilDatoMåneder { fom, tom ->
                    UtbetalingsPeriode(
                        fom = fom,
                        tom = tom,
                        utbetalingsMåned = periode.fom.toYearMonth(),
                    )
                }
            }
    }

    fun beregnBeløp(sats: Int, studieprosent: Int): Int {
        val PROSENT_50 = BigDecimal(0.5)
        val PROSENTGRENSE_HALV_SATS = 50

        if (studieprosent <= PROSENTGRENSE_HALV_SATS) {
            return BigDecimal(sats).multiply(PROSENT_50).setScale(0, RoundingMode.HALF_UP).toInt()
        }
        return sats
    }

    // TODO flytt til Kontrakter
    private fun <P : Periode<LocalDate>, VAL> P.delIDatoTilDatoMåneder(value: (fom: LocalDate, tom: LocalDate) -> VAL): List<VAL> {
        val perioder = mutableListOf<VAL>()
        var gjeldendeFom = fom
        while (gjeldendeFom < tom) {
            val nyTom = gjeldendeFom.plusMonths(1).let {
                if (gjeldendeFom.dayOfMonth >= it.lengthOfMonth()) it.tilSisteDagIMåneden() else it.minusDays(1)
            }.coerceAtMost(tom)

            perioder.add(value(gjeldendeFom, nyTom))

            gjeldendeFom = nyTom.plusDays(1)
        }
        return perioder
    }

    // TODO flytt til Kontrakter
    private fun <P : Periode<LocalDate>> P.delIÅr(value: (fom: LocalDate, tom: LocalDate) -> P): List<P> {
        val perioder = mutableListOf<P>()
        var gjeldeneFom = fom
        while (gjeldeneFom < tom) {
            val nyTom = LocalDate.of(gjeldeneFom.year, 12, 31).coerceAtMost(tom)
            perioder.add(value(gjeldeneFom, nyTom))
            gjeldeneFom = LocalDate.of(gjeldeneFom.year + 1, 1, 1)
        }
        return perioder
    }
}

data class Aktivitet(
    val id: UUID,
    val type: AktivitetType,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val prosent: Int,
    val studienivå: Studienivå?, // TODO burde ikke være nullable?
) : Periode<LocalDate>

fun List<Vilkårperiode>.tilAktiviteter(): List<Aktivitet> {
    return this
        .ofType<AktivitetLæremidler>()
        .map {
            val fakta = it.faktaOgVurdering.fakta
            Aktivitet(
                id = it.id,
                type = it.faktaOgVurdering.type.vilkårperiodeType,
                fom = it.fom,
                tom = it.tom,
                prosent = fakta.takeIfFaktaOrThrow<FaktaAktivitetLæremidler>().prosent,
                studienivå = fakta.takeIfFaktaOrThrow<FaktaAktivitetLæremidler>().studienivå,
            )
        }
}
