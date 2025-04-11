package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift.UTGIFTER_OVERNATTING
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month.JANUARY
import java.util.UUID

class HarUtgifterHeleVedtaksperiodenValideringTest {
    val førsteJanuar: LocalDate = LocalDate.of(2025, JANUARY, 1)
    val andreJanuar: LocalDate = førsteJanuar.plusDays(1)
    val tiendeJanuar: LocalDate = førsteJanuar.plusDays(9)

    val ingenUtgifter = emptyMap<TypeBoutgift, List<UtgiftBeregningBoutgifter>>()

    @Nested
    inner class HappyCase {
        @Test
        fun `Er fornøyd hvis vi mangler både utgifter og vedtaksperioder`() {
            // Skal kjøre uten valideringfeil:
            validerUtgiftHeleVedtaksperioden(
                vedtaksperioder = emptyList(),
                utgifter = ingenUtgifter,
            )
        }

        @Test
        fun `Er fornøyd over tom liste med vedtaksperioder selv hvis vi har utgifter`() {
            val utgift = lagDummyUtgift(førsteJanuar)

            // Skal kjøre uten valideringfeil:
            validerUtgiftHeleVedtaksperioden(
                vedtaksperioder = emptyList(),
                utgifter = mapOf(UTGIFTER_OVERNATTING to listOf(utgift)),
            )
        }

        @Test
        fun `Er fornøyd hvis vi har én vedtaksperiode og én utgift som dekker hele perioden`() {
            val utgift = lagDummyUtgift(fom = førsteJanuar, tom = andreJanuar)

            val vedtaksperiode = lagDummyVedtaksperiode(fom = førsteJanuar, tom = andreJanuar)

            // Skal kjøre uten valideringfeil:
            validerUtgiftHeleVedtaksperioden(
                vedtaksperioder = listOf(vedtaksperiode),
                utgifter = mapOf(UTGIFTER_OVERNATTING to listOf(utgift)),
            )
        }

        @Test
        fun `Er fornøyd hvis vi har én vedtaksperiode og to utgifter som til sammen dekker hele perioden`() {
            val utgifter = listOf(lagDummyUtgift(førsteJanuar), lagDummyUtgift(andreJanuar))

            val vedtaksperiode = lagDummyVedtaksperiode(fom = førsteJanuar, tom = andreJanuar)

            // Skal kjøre uten valideringfeil:
            validerUtgiftHeleVedtaksperioden(
                vedtaksperioder = listOf(vedtaksperiode),
                utgifter = mapOf(UTGIFTER_OVERNATTING to utgifter),
            )
        }

        @Test
        fun `Er fornøyd hvis vi har flere vedtaksperioder og flere utgifter som dekker periodene`() {
            val utgifter =
                listOf(
                    lagDummyUtgift(førsteJanuar),
                    lagDummyUtgift(tiendeJanuar),
                )

            val vedtaksperioder =
                listOf(
                    lagDummyVedtaksperiode(fom = førsteJanuar),
                    lagDummyVedtaksperiode(fom = tiendeJanuar),
                )

            // Skal kjøre uten valideringfeil:
            validerUtgiftHeleVedtaksperioden(
                vedtaksperioder = vedtaksperioder,
                utgifter = mapOf(UTGIFTER_OVERNATTING to utgifter),
            )
        }
    }

    @Nested
    inner class SadCase {
        @Test
        fun `Skal klage hvis vi har en vedtaksperiode men ingen utgift`() {
            val vedtaksperiode = listOf(lagDummyVedtaksperiode(førsteJanuar))

            assertThatThrownBy {
                validerUtgiftHeleVedtaksperioden(
                    vedtaksperioder = vedtaksperiode,
                    utgifter = ingenUtgifter,
                )
            }.hasMessageContaining("Vedtaksperioden 01.01.2025–01.01.2025")
        }

        @Test
        fun `Skal klage hvis vi har to vedtaksperioder, men bare én av dem har overlappende utgift`() {
            val vedtaksperioder =
                listOf(
                    lagDummyVedtaksperiode(førsteJanuar),
                    lagDummyVedtaksperiode(tiendeJanuar),
                )
            val utgifter = lagDummyUtgift(førsteJanuar)

            assertThatThrownBy {
                validerUtgiftHeleVedtaksperioden(
                    vedtaksperioder = vedtaksperioder,
                    utgifter = mapOf(UTGIFTER_OVERNATTING to listOf(utgifter)),
                )
            }.hasMessageContaining("Vedtaksperioden 10.01.2025–10.01.2025")
        }

        @Test
        fun `Skal formatere feilmelding på en fin måte når det er flere vedtaksperioder uten utgift`() {
            val vedtaksperioder =
                listOf(
                    lagDummyVedtaksperiode(førsteJanuar),
                    lagDummyVedtaksperiode(andreJanuar),
                    lagDummyVedtaksperiode(tiendeJanuar),
                )
            assertThatThrownBy {
                validerUtgiftHeleVedtaksperioden(
                    vedtaksperioder = vedtaksperioder,
                    utgifter = ingenUtgifter,
                )
            }.hasMessageContaining(
                "Vedtaksperiodene 01.01.2025–01.01.2025, 02.01.2025–02.01.2025 og 10.01.2025–10.01.2025",
            )
        }
    }
}

private fun lagDummyVedtaksperiode(
    fom: LocalDate,
    tom: LocalDate = fom,
) = Vedtaksperiode(
    id = UUID.randomUUID(),
    fom = fom,
    tom = tom,
    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
    aktivitet = AktivitetType.TILTAK,
)

private fun lagDummyUtgift(
    fom: LocalDate,
    tom: LocalDate = fom,
) = UtgiftBeregningBoutgifter(
    fom = fom,
    tom = tom,
    utgift = 1000,
)
