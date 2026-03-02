package no.nav.tilleggsstonader.sak.privatbil

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class BehandleMottattKjørelisteIntegrationTest(
    @Autowired private val behandlingService: BehandlingService,
) : CleanDatabaseIntegrationTest() {
    val fom: LocalDate = 1 januar 2026
    val tom: LocalDate = 31 januar 2026

    @Test
    fun `ta i mot kjøreliste og opprett behandling med kopierte verdier`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true
        val behandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)

                sendInnKjøreliste {
                    periode = Datoperiode(fom, 2 januar 2026)
                    kjørteDager = listOf(
                        1 januar 2026 to 50,
                        2 januar 2026 to 50
                    )
                }
            }
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val alleBehandlinger = behandlingService.hentBehandlinger(behandling.fagsakId)

        val kjørelistebehandling = alleBehandlinger.first { it.type == BehandlingType.KJØRELISTE }

        assertThat(kjørelistebehandling).isNotNull()
        assertThat(kjørelistebehandling.forrigeIverksatteBehandlingId).isEqualTo(behandling.id)
        assertThat(kjørelistebehandling.steg).isEqualTo(StegType.KJØRELISTE)

        // TODO: Sjekk at data blir kopiert riktig?
    }
}