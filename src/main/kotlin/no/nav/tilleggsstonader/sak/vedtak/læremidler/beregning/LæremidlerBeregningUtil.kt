package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.splitPerLøpendeMåneder
import no.nav.tilleggsstonader.kontrakter.felles.splitPerÅr
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeUtil.ofType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaAktivitetLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.FaktaOgVurderingUtil.takeIfFaktaOrThrow

object LæremidlerBeregningUtil {
    fun Periode<LocalDate>.delTilUtbetalingsPerioder(): List<UtbetalingsPeriode> {
        return splitPerÅr { fom, tom -> Vedtaksperiode(fom, tom) }
            .flatMap { periode ->
                periode.splitPerLøpendeMåneder { fom, tom ->
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
