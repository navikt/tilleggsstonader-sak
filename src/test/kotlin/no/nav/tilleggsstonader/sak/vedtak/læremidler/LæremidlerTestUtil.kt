package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.BeregningsresultatForPeriodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate

object LæremidlerTestUtil {

    val defaultInnvilgelseLæremidler = InnvilgelseLæremidler(
        vedtaksperioder = listOf(
            Vedtaksperiode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 7),
            ),
        ),
        beregningsresultat = BeregningsresultatLæremidler(
            perioder = listOf(
                beregningsresultatForMåned(
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 1, 7),
                    utbetalingsdato = LocalDate.of(2024, 1, 1),
                ),
            ),
        ),
    )

    fun innvilgelse(data: InnvilgelseLæremidler = defaultInnvilgelseLæremidler) = GeneriskVedtak(
        behandlingId = BehandlingId.random(),
        type = TypeVedtak.INNVILGELSE,
        data = data,
    )

    fun beregningsresultatForMåned(
        fom: LocalDate,
        tom: LocalDate,
        utbetalingsdato: LocalDate = fom,
    ): BeregningsresultatForMåned {
        return BeregningsresultatForMåned(
            beløp = 875,
            grunnlag = Beregningsgrunnlag(
                fom = fom,
                tom = tom,
                utbetalingsdato = utbetalingsdato,
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
        utbetalingsdato: LocalDate = fom,
    ): BeregningsresultatForPeriodeDto {
        return BeregningsresultatForPeriodeDto(
            fom = fom,
            tom = tom,
            antallMåneder = antallMåneder,
            studienivå = Studienivå.HØYERE_UTDANNING,
            studieprosent = 100,
            beløp = 875,
            stønadsbeløp = stønadsbeløp,
            utbetalingsdato = utbetalingsdato,
        )
    }
}
