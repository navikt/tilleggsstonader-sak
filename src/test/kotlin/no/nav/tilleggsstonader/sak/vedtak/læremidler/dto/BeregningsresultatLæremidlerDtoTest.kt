package no.nav.tilleggsstonader.sak.vedtak.læremidler.dto

import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.beregningsresultatForPeriodeDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class BeregningsresultatLæremidlerDtoTest {

    @Test
    fun `skal mappe til dto`() {
        val dto = BeregningsresultatLæremidler(
            perioder = listOf(
                beregningsresultatForMåned(
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 1, 31),
                    YearMonth.of(2024, 1),
                ),
                beregningsresultatForMåned(
                    LocalDate.of(2024, 2, 1),
                    LocalDate.of(2024, 2, 29),
                    YearMonth.of(2024, 1),
                ),
                beregningsresultatForMåned(
                    LocalDate.of(2024, 5, 1),
                    LocalDate.of(2024, 5, 31),
                    YearMonth.of(2024, 5),
                ),
            ),
        ).tilDto()

        assertThat(dto.perioder).containsExactlyInAnyOrder(
            beregningsresultatForPeriodeDto(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 2, 29),
                antallMåneder = 2,
                stønadsbeløp = 1750,
            ),
            beregningsresultatForPeriodeDto(
                fom = LocalDate.of(2024, 5, 1),
                tom = LocalDate.of(2024, 5, 31),
            ),
        )
    }
}
