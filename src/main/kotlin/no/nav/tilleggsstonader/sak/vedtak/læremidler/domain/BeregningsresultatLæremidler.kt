package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate
import java.time.YearMonth

data class BeregningsresultatLæremidler(
    val perioder: List<BeregningsresultatForMåned>,
) {
    fun tilDto(): BeregningsresultatLæremidlerDto {
        return BeregningsresultatLæremidlerDto(
            perioder = perioder.fold(mutableListOf()) { acc, periode ->
                val forrigePeriode = acc.lastOrNull()
                if (forrigePeriode != null && skalSlåsSammen(forrigePeriode, periode)) {
                    val nyBeregningsresultatForPeriodeDto = forrigePeriode.copy(
                        tom = periode.grunnlag.tom,
                        stønadsbeløp = forrigePeriode.stønadsbeløp + periode.beløp,
                        antallMåneder = forrigePeriode.antallMåneder + 1,
                    )
                    acc.removeLast()
                    acc.add(nyBeregningsresultatForPeriodeDto)
                } else {
                    acc.add(periode.tilDto())
                }
                return@fold acc
            },
        )
    }

    private fun skalSlåsSammen(
        gjeldenePeriode: BeregningsresultatForPeriodeDto,
        nestePeriode: BeregningsresultatForMåned,
    ): Boolean {
        return gjeldenePeriode.studienivå == nestePeriode.grunnlag.studienivå &&
            gjeldenePeriode.studieprosent == nestePeriode.grunnlag.studieprosent &&
            gjeldenePeriode.sats == nestePeriode.grunnlag.sats &&
            gjeldenePeriode.utbetalingsmåned == nestePeriode.grunnlag.utbetalingsMåned
    }
}

data class BeregningsresultatForMåned(
    val beløp: Int,
    val grunnlag: Beregningsgrunnlag,
) {
    fun tilDto(): BeregningsresultatForPeriodeDto {
        return BeregningsresultatForPeriodeDto(
            fom = grunnlag.fom,
            tom = grunnlag.tom,
            antallMåneder = 1,
            studienivå = grunnlag.studienivå,
            studieprosent = grunnlag.studieprosent,
            sats = grunnlag.sats,
            stønadsbeløp = beløp,
            utbetalingsmåned = grunnlag.utbetalingsMåned,
        )
    }
}

data class Beregningsgrunnlag(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val utbetalingsMåned: YearMonth,
    val studienivå: Studienivå,
    val studieprosent: Int,
    val sats: Int,
    val målgruppe: MålgruppeType,
) : Periode<LocalDate>

data class BeregningsresultatLæremidlerDto(
    val perioder: List<BeregningsresultatForPeriodeDto>,
)

data class BeregningsresultatForPeriodeDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val antallMåneder: Int,
    val studienivå: Studienivå,
    val studieprosent: Int,
    val sats: Int,
    val stønadsbeløp: Int,
    val utbetalingsmåned: YearMonth,
)
