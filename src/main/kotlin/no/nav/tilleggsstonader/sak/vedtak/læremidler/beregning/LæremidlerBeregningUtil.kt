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

    // TODO flytt til Kontrakter
    fun <P : Periode<LocalDate>, VAL> P.delIDatoTilDatoMåneder(value: (fom: LocalDate, tom: LocalDate) -> VAL): List<VAL> {
        val perioder = mutableListOf<VAL>()
        var gjeldeneFom = fom
        while (gjeldeneFom < tom) {
            val nyTom = minOf(gjeldeneFom.plusMonths(1).minusDays(1), tom)
            perioder.add(value(gjeldeneFom, nyTom))
            gjeldeneFom = gjeldeneFom.plusMonths(1)
        }
        return perioder
    }

    // TODO flytt til Kontrakter
    fun <P : Periode<LocalDate>> P.delIÅr(value: (fom: LocalDate, tom: LocalDate) -> P): List<P> {
        val perioder = mutableListOf<P>()
        var gjeldeneFom = fom
        while (gjeldeneFom < tom) {
            val nyTom = minOf(LocalDate.of(gjeldeneFom.year, 12, 31), tom)
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
