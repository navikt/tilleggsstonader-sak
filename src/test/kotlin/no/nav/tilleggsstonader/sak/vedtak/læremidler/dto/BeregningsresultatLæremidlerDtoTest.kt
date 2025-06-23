package no.nav.tilleggsstonader.sak.vedtak.læremidler.dto

import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.beregningsresultatForPeriodeDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BeregningsresultatLæremidlerDtoTest {
    @Test
    fun `skal mappe til dto`() {
        val dto =
            BeregningsresultatLæremidler(
                perioder =
                    listOf(
                        beregningsresultatForMåned(
                            LocalDate.of(2024, 1, 1),
                            LocalDate.of(2024, 1, 31),
                        ),
                        beregningsresultatForMåned(
                            LocalDate.of(2024, 2, 1),
                            LocalDate.of(2024, 2, 29),
                            utbetalingsdato = LocalDate.of(2024, 1, 1),
                        ),
                        beregningsresultatForMåned(
                            LocalDate.of(2024, 5, 1),
                            LocalDate.of(2024, 5, 31),
                        ),
                    ),
            ).tilDto(beregnetFra = null)

        assertThat(dto.perioder).containsExactlyInAnyOrder(
            beregningsresultatForPeriodeDto(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 2, 29),
                antallMåneder = 2,
                stønadsbeløpForPeriode = 1750,
            ),
            beregningsresultatForPeriodeDto(
                fom = LocalDate.of(2024, 5, 1),
                tom = LocalDate.of(2024, 5, 31),
            ),
        )
    }
}
