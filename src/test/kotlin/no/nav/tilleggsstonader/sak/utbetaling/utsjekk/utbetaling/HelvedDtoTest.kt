package no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider.jsonMapper
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.utbetaling.fagomrade.FagsakUtbetalingsvalgService
import no.nav.tilleggsstonader.sak.utbetaling.id.FagsakUtbetalingId
import no.nav.tilleggsstonader.sak.utbetaling.id.FagsakUtbetalingIdService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class HelvedDtoTest {
    @Test
    fun `skal mappe og serialisere alle flytteandeler til riktig utbetalingsstønad`() {
        val behandling = saksbehandling()
        val idService = mockk<FagsakUtbetalingIdService>()
        val utbetalingsvalgService = mockk<FagsakUtbetalingsvalgService>()
        val mapper = UtbetalingV3Mapper(idService, utbetalingsvalgService, mockk())
        every { utbetalingsvalgService.hentEllerSettUtbetalPåNyttFagområde(any(), any()) } returns true

        val forventetMapping =
            mapOf(
                TypeAndel.FLYTTING_AAP to StønadUtbetaling.FLYTTING_AAP,
                TypeAndel.FLYTTING_ENSLIG_FORSØRGER to StønadUtbetaling.FLYTTING_ENSLIG_FORSØRGER,
                TypeAndel.FLYTTING_ETTERLATTE to StønadUtbetaling.FLYTTING_ETTERLATTE,
                TypeAndel.FLYTTING_AKTIVITETSPENGER to StønadUtbetaling.FLYTTING_AKTIVITETSPENGER,
                TypeAndel.FLYTTING_ARBEIDSSØKER to StønadUtbetaling.FLYTTING_ARBEIDSSØKER,
            )
        assertThat(forventetMapping.keys)
            .containsExactlyInAnyOrderElementsOf(TypeAndel.entries.filter { it.name.startsWith("FLYTTING_") })

        forventetMapping.forEach { (typeAndel, stønad) ->
            val utbetalingId =
                FagsakUtbetalingId(fagsakId = behandling.fagsakId, typeAndel = typeAndel, reiseId = null)
            every { idService.hentEllerOpprettUtbetalingId(behandling.fagsakId, typeAndel, null) } returns utbetalingId
            every { idService.hentUtbetalingIderForFagsakId(behandling.fagsakId) } returns listOf(utbetalingId)
            val andel =
                AndelTilkjentYtelse(
                    beløp = 1000,
                    fom = 1 januar 2026,
                    tom = 1 januar 2026,
                    utbetalingsdato = 1 januar 2026,
                    satstype = Satstype.DAG,
                    type = typeAndel,
                )

            val dto = mapper.lagSimuleringDtoer(behandling, listOf(andel))
            assertThat(dto.utbetalinger.single().stønad).isEqualTo(stønad)
            val json = jsonMapper.readTree(jsonMapper.writeValueAsString(dto))
            assertThat(json["utbetalinger"][0]["stønad"].stringValue()).isEqualTo(typeAndel.name)
        }
    }

    private val testUtbetalinger =
        listOf(
            UtbetalingDto(
                id = UtbetalingId(UUID.fromString("7b78b166-9097-4117-b046-5f7e25b91d6b")),
                stønad = StønadUtbetaling.DAGLIG_REISE_ENSLIG_FORSØRGER,
                perioder =
                    listOf(
                        UtbetalingPeriodeDto(
                            fom = 1 januar 2024,
                            tom = 31 januar 2024,
                            beløp = 1000u,
                            betalendeEnhet = null,
                        ),
                        UtbetalingPeriodeDto(
                            fom = 1 februar 2024,
                            tom = 29 februar 2024,
                            beløp = 1500u,
                            betalendeEnhet = null,
                        ),
                    ),
                brukFagområdeTillst = true,
            ),
        )

    @Test
    fun `IverksettingDto skal ha forventet JSON-struktur`() {
        val iverksettingDto =
            IverksettingDto(
                sakId = "SAK123",
                behandlingId = "BEH456",
                personident = "12345678901",
                periodetype = PeriodetypeUtbetaling.UKEDAG,
                vedtakstidspunkt = (15 januar 2024).atTime(12, 30, 0),
                saksbehandler = "Z123456",
                beslutter = "Z654321",
                utbetalinger = testUtbetalinger,
            )

        val faktiskJson = jsonMapper.writeValueAsString(iverksettingDto)

        val forventetJson =
            """
            {
              "sakId": "SAK123",
              "behandlingId": "BEH456",
              "personident": "12345678901",
              "periodetype": "UKEDAG",
              "utbetalinger": [
                {
                  "id": "7b78b166-9097-4117-b046-5f7e25b91d6b",
                  "stønad": "DAGLIG_REISE_ENSLIG_FORSØRGER",
                  "perioder": [
                    {
                      "fom": "2024-01-01",
                      "tom": "2024-01-31",
                      "beløp": 1000,
                      "betalendeEnhet": null
                    },
                    {
                      "fom": "2024-02-01",
                      "tom": "2024-02-29",
                      "beløp": 1500,
                      "betalendeEnhet": null
                    }
                  ],
                  "brukFagområdeTillst": true
                }
              ],
              "saksbehandler": "Z123456",
              "beslutter": "Z654321",
              "vedtakstidspunkt": "2024-01-15T12:30:00",
              "dryrun": false
            }
            """.trimIndent()

        val faktisk = jsonMapper.readTree(faktiskJson)
        val forventet = jsonMapper.readTree(forventetJson)

        assertThat(faktisk).isEqualTo(forventet)
    }

    @Test
    fun `SimuleringDto skal ha forventet JSON-struktur`() {
        val vedtakstidspunkt = (15 januar 2024).atTime(14, 45, 30)

        val simuleringDto =
            SimuleringDto(
                sakId = "SAK123",
                behandlingId = "BEH456",
                personident = "12345678901",
                periodetype = PeriodetypeUtbetaling.UKEDAG,
                vedtakstidspunkt = vedtakstidspunkt,
                utbetalinger = testUtbetalinger,
            )

        val faktiskJson = jsonMapper.writeValueAsString(simuleringDto)

        val forventetJson =
            """
            {
              "sakId": "SAK123",
              "behandlingId": "BEH456",
              "personident": "12345678901",
              "periodetype": "UKEDAG",
              "utbetalinger": [
                {
                  "id": "7b78b166-9097-4117-b046-5f7e25b91d6b",
                  "stønad": "DAGLIG_REISE_ENSLIG_FORSØRGER",
                  "perioder": [
                    {
                      "fom": "2024-01-01",
                      "tom": "2024-01-31",
                      "beløp": 1000,
                      "betalendeEnhet": null
                    },
                    {
                      "fom": "2024-02-01",
                      "tom": "2024-02-29",
                      "beløp": 1500,
                      "betalendeEnhet": null
                    }
                  ],
                  "brukFagområdeTillst": true
                }
              ],
              "vedtakstidspunkt": "2024-01-15T14:45:30",
              "dryrun": true
            }
            """.trimIndent()

        val faktisk = jsonMapper.readTree(faktiskJson)
        val forventet = jsonMapper.readTree(forventetJson)

        assertThat(faktisk).isEqualTo(forventet)
    }
}
