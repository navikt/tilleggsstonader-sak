package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tasks.kjørTasksKlareForProsesseringTilIngenTasksIgjen
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tilordneÅpenBehandlingOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.ÅrsakUnderkjent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class TotrinnskontrollKjørelistebehandlingIntegrationTest : CleanDatabaseIntegrationTest() {
    @Autowired
    private lateinit var behandlingService: BehandlingService

    val fom: LocalDate = 1 januar 2026
    val tom: LocalDate = 31 januar 2026

    @BeforeEach
    fun setUp() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true
    }

    @Test
    fun `godkjenne totrinnskontroll for manuell kjørelistebehandling ferdigstiller behandlingen`() {
        val kjørelistebehandling = opprettManuellKjørelistebehandlingOgGjennomførTilBeslutteVedtak()

        medBrukercontext(bruker = "nissemor", roller = listOf(rolleConfig.beslutterRolle)) {
            tilordneÅpenBehandlingOppgaveForBehandling(kjørelistebehandling.id)
            kall.totrinnskontroll.beslutteVedtak(kjørelistebehandling.id, BeslutteVedtakDto(godkjent = true))
        }
        kjørTasksKlareForProsesseringTilIngenTasksIgjen()

        val ferdigstiltBehandling = behandlingService.hentBehandling(kjørelistebehandling.id)
        assertThat(ferdigstiltBehandling.status).isEqualTo(BehandlingStatus.FERDIGSTILT)
        assertThat(ferdigstiltBehandling.steg).isEqualTo(StegType.BEHANDLING_FERDIGSTILT)
    }

    @Test
    fun `avvise totrinnskontroll for manuell kjørelistebehandling sender behandlingen tilbake til saksbehandler`() {
        val kjørelistebehandling = opprettManuellKjørelistebehandlingOgGjennomførTilBeslutteVedtak()

        medBrukercontext(bruker = "nissemor", roller = listOf(rolleConfig.beslutterRolle)) {
            tilordneÅpenBehandlingOppgaveForBehandling(kjørelistebehandling.id)
            kall.totrinnskontroll.beslutteVedtak(
                kjørelistebehandling.id,
                BeslutteVedtakDto(
                    godkjent = false,
                    begrunnelse = "Feil i beregning",
                    årsakerUnderkjent = listOf(ÅrsakUnderkjent.VEDTAK_OG_BEREGNING),
                ),
            )
        }

        val underkjentBehandling = behandlingService.hentBehandling(kjørelistebehandling.id)
        assertThat(underkjentBehandling.status).isEqualTo(BehandlingStatus.UTREDES)
        assertThat(underkjentBehandling.steg).isEqualTo(StegType.SEND_TIL_BESLUTTER)
    }

    private fun opprettManuellKjørelistebehandlingOgGjennomførTilBeslutteVedtak() =
        opprettBehandlingOgGjennomførBehandlingsløp(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        ) {
            defaultDagligReisePrivatBilTsoTestdata(fom, tom)

            sendInnKjøreliste {
                periode = Datoperiode(5 januar 2026, 6 januar 2026)
                kjørteDager =
                    listOf(
                        KjørtDag(dato = 5 januar 2026, parkeringsutgift = null),
                        KjørtDag(dato = 6 januar 2026, parkeringsutgift = null),
                    )
            }
        }.let { behandlingContext ->
            behandlingService
                .hentBehandlinger(behandlingContext.fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE }
        }.also { kjørelistebehandling ->
            gjennomførKjørelisteBehandling(kjørelistebehandling, tilSteg = StegType.BESLUTTE_VEDTAK)
        }
}
