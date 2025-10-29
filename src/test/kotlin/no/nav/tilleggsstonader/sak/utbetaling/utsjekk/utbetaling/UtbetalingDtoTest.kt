package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class UtbetalingDtoTest {
    private val testUtbetalingsgrunnlag =
        UtbetalingGrunnlagDto(
            id = UUID.fromString("12345678-1234-1234-1234-123456789abc"),
            sakId = "SAK123",
            behandlingId = "BEH456",
            personident = "12345678901",
            periodetype = PeriodetypeUtbetaling.UKEDAG,
            stønad = StønadUtbetaling.DAGLIG_REISE_ENSLIG_FORSØRGER,
            perioder =
                listOf(
                    PerioderUtbetaling(
                        fom = 1 januar 2024,
                        tom = 31 januar 2024,
                        beløp = 1000u,
                    ),
                    PerioderUtbetaling(
                        fom = 1 februar 2024,
                        tom = 29 februar 2024,
                        beløp = 1500u,
                    ),
                ),
            brukFagområdeTillst = false,
        )

    @Test
    fun `IverksettingDto skal ha forventet JSON-struktur`() {
        val iverksettingDto =
            IverksettingDto(
                utbetalingsgrunnlag = testUtbetalingsgrunnlag,
                vedtakstidspunkt = (15 januar 2024).atTime(12, 30, 0),
                saksbehandler = "Z123456",
                beslutter = "Z654321",
            )

        val faktiskJson = objectMapper.writeValueAsString(iverksettingDto)

        val forventetJson =
            """
            {
              "id": "12345678-1234-1234-1234-123456789abc",
              "sakId": "SAK123",
              "behandlingId": "BEH456",
              "personident": "12345678901",
              "periodetype": "UKEDAG",
              "stønad": "DAGLIG_REISE_ENSLIG_FORSØRGER",
              "perioder": [
                {
                  "fom": "2024-01-01",
                  "tom": "2024-01-31",
                  "beløp": 1000
                },
                {
                  "fom": "2024-02-01",
                  "tom": "2024-02-29",
                  "beløp": 1500
                }
              ],
              "brukFagområdeTillst": false,
              "vedtakstidspunkt": "2024-01-15T12:30:00",
              "saksbehandler": "Z123456",
              "beslutter": "Z654321",
              "dryrun": false
            }
            """.trimIndent()

        val faktisk = objectMapper.readTree(faktiskJson)
        val forventet = objectMapper.readTree(forventetJson)

        assertThat(faktisk).isEqualTo(forventet)
    }

    @Test
    fun `SimuleringDto skal ha forventet JSON-struktur`() {
        val vedtakstidspunkt = (15 januar 2024).atTime(14, 45, 30)

        val simuleringDto =
            SimuleringDto(
                utbetalingsgrunnlag = testUtbetalingsgrunnlag,
            ).copy(vedtakstidspunkt = vedtakstidspunkt)

        val faktiskJson = objectMapper.writeValueAsString(simuleringDto)

        val forventetJson =
            """
            {
              "id": "12345678-1234-1234-1234-123456789abc",
              "sakId": "SAK123",
              "behandlingId": "BEH456",
              "personident": "12345678901",
              "periodetype": "UKEDAG",
              "stønad": "DAGLIG_REISE_ENSLIG_FORSØRGER",
              "perioder": [
                {
                  "fom": "2024-01-01",
                  "tom": "2024-01-31",
                  "beløp": 1000
                },
                {
                  "fom": "2024-02-01",
                  "tom": "2024-02-29",
                  "beløp": 1500
                }
              ],
              "brukFagområdeTillst": false,
              "vedtakstidspunkt": "2024-01-15T14:45:30",
              "dryrun": true
            }
            """.trimIndent()

        val faktisk = objectMapper.readTree(faktiskJson)
        val forventet = objectMapper.readTree(forventetJson)

        assertThat(faktisk).isEqualTo(forventet)
    }
}
