package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
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
import java.time.YearMonth
import java.util.UUID

object LæremidlerBeregningUtil {
    fun Periode<LocalDate>.delTilUtbetalingsPerioder(): List<UtbetalingsPeriode> =
        splitPerÅr { fom, tom -> Vedtaksperiode(fom, tom) }
            .flatMap { periode ->
                periode.splitPerLøpendeMåneder { fom, tom ->
                    UtbetalingsPeriode(
                        fom = fom,
                        tom = tom,
                        utbetalingsmåned = periode.fom.toYearMonth(),
                    )
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
}

data class Aktivitet(
    val id: UUID,
    val type: AktivitetType,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val prosent: Int,
    val studienivå: Studienivå,
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

// TODO bruk disse fra kontrakter når nyeste version er merget inn:
fun <P : Periode<LocalDate>> P.splitPerÅr(medNyPeriode: (fom: LocalDate, tom: LocalDate) -> P): List<P> {
    val perioder = mutableListOf<P>()
    var gjeldeneFom = fom
    while (gjeldeneFom <= tom) {
        val nyTom = minOf(gjeldeneFom.sisteDagIÅret(), tom)
        perioder.add(medNyPeriode(gjeldeneFom, nyTom))
        gjeldeneFom = gjeldeneFom.førsteDagNesteÅr()
    }
    return perioder
}

fun <P : Periode<LocalDate>, VAL> P.splitPerLøpendeMåneder(medNyPeriode: (fom: LocalDate, tom: LocalDate) -> VAL): List<VAL> {
    val perioder = mutableListOf<VAL>()
    var gjeldendeFom = fom
    while (gjeldendeFom <= tom) {
        val nyTom = minOf(gjeldendeFom.sisteDagenILøpendeMåned(), tom)

        perioder.add(medNyPeriode(gjeldendeFom, nyTom))

        gjeldendeFom = nyTom.plusDays(1)
    }
    return perioder
}

fun LocalDate.sisteDagenILøpendeMåned(): LocalDate {
    val nesteMåned = this.plusMonths(1)
    return if (this.dayOfMonth >= nesteMåned.lengthOfMonth()) {
        nesteMåned.tilSisteDagIMåneden()
    } else {
        nesteMåned.minusDays(1)
    }
}

fun LocalDate.tilSisteDagIMåneden() = YearMonth.from(this).atEndOfMonth()
fun LocalDate.sisteDagIÅret() = LocalDate.of(year, 12, 31)
fun LocalDate.førsteDagNesteÅr() = LocalDate.of(year + 1, 1, 1)
