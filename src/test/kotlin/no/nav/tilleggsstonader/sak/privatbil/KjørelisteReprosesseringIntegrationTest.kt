package no.nav.tilleggsstonader.sak.privatbil

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.HenleggService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.HenlagtÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.HenlagtDto
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectOkWithBody
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.tilordneÅpenBehandlingOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class KjørelisteReprosesseringIntegrationTest : IntegrationTest() {
    @Autowired
    lateinit var henleggService: HenleggService

    @Test
    fun `skal reprosessere kjøreliste fra henlagt kjørelistebehandling`() {
        val fom = 5 januar 2026
        val tom = 11 januar 2026
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)
                sendInnKjøreliste {
                    periode = Datoperiode(fom, tom)
                    kjørteDager = listOf(KjørtDag(dato = fom, parkeringsutgift = 50))
                }
            }

        val kjørelistebehandling =
            testoppsettService
                .hentBehandlinger(behandlingContext.fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE }
        val eksternFagsakId = testoppsettService.hentFagsak(behandlingContext.fagsakId).eksternId.id
        val kjørelisteId =
            kall.privatBil
                .hentReisevurderingForBehandling(kjørelistebehandling.id)
                .single()
                .uker
                .single()
                .kjørelisteId!!

        tilordneÅpenBehandlingOppgaveForBehandling(kjørelistebehandling.id)
        kall.behandling.henlegg(
            kjørelistebehandling.id,
            HenlagtDto(årsak = HenlagtÅrsak.FEILREGISTRERT),
        )

        val tilgjengeligeKjørelisterEtterHenleggelse =
            medBrukercontext(roller = listOf(rolleConfig.utvikler)) {
                kall.testklient
                    .get("/api/forvaltning/kjoreliste/fagsak/$eksternFagsakId/tilgjengelige")
                    .expectOkWithBody<List<TilgjengeligKjøreliste>>()
            }
        assertThat(tilgjengeligeKjørelisterEtterHenleggelse.map { it.kjørelisteId })
            .containsExactly(kjørelisteId)

        medBrukercontext(roller = listOf(rolleConfig.utvikler)) {
            kall.testklient
                .post("/api/forvaltning/kjoreliste/$kjørelisteId/behandle-pa-nytt")
                .expectStatus()
                .isOk
                .expectBody()
                .isEmpty()
        }

        val kjørelistebehandlinger =
            testoppsettService
                .hentBehandlinger(behandlingContext.fagsakId)
                .filter { it.type == BehandlingType.KJØRELISTE }
        assertThat(kjørelistebehandlinger).hasSize(2)
        assertThat(kjørelistebehandlinger.single { it.id == kjørelistebehandling.id }.erHenlagt()).isTrue()

        val nyKjørelistebehandling = kjørelistebehandlinger.single { it.id != kjørelistebehandling.id }
        val nyReisevurdering =
            kall.privatBil
                .hentReisevurderingForBehandling(nyKjørelistebehandling.id)
                .single()
        val nyUke = nyReisevurdering.uker.single()

        assertThat(nyUke.kjørelisteId).isEqualTo(kjørelisteId)
        assertThat(nyUke.avklartUkeId).isNotNull()

        val tilgjengeligeKjørelisterEtterReprosessering =
            medBrukercontext(roller = listOf(rolleConfig.utvikler)) {
                kall.testklient
                    .get("/api/forvaltning/kjoreliste/fagsak/$eksternFagsakId/tilgjengelige")
                    .expectOkWithBody<List<TilgjengeligKjøreliste>>()
            }
        assertThat(tilgjengeligeKjørelisterEtterReprosessering).isEmpty()
    }
}
