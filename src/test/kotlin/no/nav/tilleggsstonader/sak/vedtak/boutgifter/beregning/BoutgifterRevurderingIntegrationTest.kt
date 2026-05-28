package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.august
import no.nav.tilleggsstonader.libs.utils.dato.juni
import no.nav.tilleggsstonader.libs.utils.dato.mai
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.integrasjonstest.BehandlingContext
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettRevurderingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status.UtbetalingStatus
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status.UtbetalingStatusHåndterer
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.status.UtbetalingStatusRecord
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarPåVilkårDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class BoutgifterRevurderingIntegrationTest(
    @Autowired private val utbetalingStatusHåndterer: UtbetalingStatusHåndterer,
) : IntegrationTest() {
    /**
     * Dette er en reell case fra prod, der utgiften ble lavere midt i en beregningsperiode. Det ble før stoppet av validering, men skal gå
     * gjennom ettersom begge periodene uansett er over makssatsen.
     */
    @Test
    fun `tillater flere løpende utgifter i samme utbetalingsperiode når en enkeltutgift alene når makssats`() {
        val førstegangsbehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.BOUTGIFTER,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakBoutgifter(10 august 2025, 20 juni 2028)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(30 september 2024, 30 juni 2027)
                    }
                }
                vilkår {
                    opprett {
                        løpendeutgifterEnBolig(1 august 2025, 30 juni 2026, utgift = 7_400)
                    }
                }
            }

        kvitterOkFraOppdrag(førstegangsbehandlingContext)

        val revurderingId =
            opprettRevurderingOgGjennomførBehandlingsløp(
                fraBehandlingId = førstegangsbehandlingContext.behandlingId,
            ) {
                vilkår {
                    oppdater { vilkårsvurdering ->
                        with(vilkårsvurdering.vilkårsett.single { it.opphavsvilkår != null }) {
                            SvarPåVilkårDto(
                                id = id,
                                behandlingId = behandlingId,
                                delvilkårsett = delvilkårsett,
                                fom = 1 august 2025,
                                tom = 31 mai 2026,
                                utgift = 7_400,
                                erFremtidigUtgift = erFremtidigUtgift,
                                offentligTransport = null,
                            )
                        }
                    }
                    opprett {
                        løpendeutgifterEnBolig(1 juni 2026, 30 juni 2027, utgift = 5_600)
                    }
                }
            }

        val revurdering = kall.behandling.hent(revurderingId)

        assertThat(revurdering.type).isEqualTo(BehandlingType.REVURDERING)
        assertThat(revurdering.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
        assertThat(revurdering.steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
    }

    private fun kvitterOkFraOppdrag(førstegangsbehandlingContext: BehandlingContext) {
        utbetalingStatusHåndterer.behandleStatusoppdatering(
            iverksettingId = førstegangsbehandlingContext.behandlingId.toString(),
            melding =
                UtbetalingStatusRecord(
                    status = UtbetalingStatus.OK,
                    detaljer = null,
                    error = null,
                ),
            utbetalingGjelderFagsystem = UtbetalingStatusHåndterer.FAGSYSTEM_TILLEGGSSTØNADER,
        )
    }
}
