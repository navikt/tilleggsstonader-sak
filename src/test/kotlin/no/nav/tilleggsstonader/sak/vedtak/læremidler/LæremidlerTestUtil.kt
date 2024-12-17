package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.BeregningsresultatForPeriodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate
import java.time.YearMonth

object LæremidlerTestUtil {

    fun beregningsresultatForMåned(
        fom: LocalDate,
        tom: LocalDate,
        utbetalingMåned: YearMonth,
    ): BeregningsresultatForMåned {
        return BeregningsresultatForMåned(
            beløp = 875,
            grunnlag = Beregningsgrunnlag(
                fom = fom,
                tom = tom,
                utbetalingsmåned = utbetalingMåned,
                studienivå = Studienivå.HØYERE_UTDANNING,
                studieprosent = 100,
                sats = 875,
                satsBekreftet = true,
                målgruppe = MålgruppeType.AAP,
            ),
        )
    }

    fun beregningsresultatForPeriodeDto(
        fom: LocalDate,
        tom: LocalDate,
        antallMåneder: Int = 1,
        stønadsbeløp: Int = 875,
    ): BeregningsresultatForPeriodeDto {
        return BeregningsresultatForPeriodeDto(
            fom = fom,
            tom = tom,
            antallMåneder = antallMåneder,
            studienivå = Studienivå.HØYERE_UTDANNING,
            studieprosent = 100,
            beløp = 875,
            stønadsbeløp = stønadsbeløp,
            utbetalingsmåned = fom.toYearMonth(),
        )
    }
}
