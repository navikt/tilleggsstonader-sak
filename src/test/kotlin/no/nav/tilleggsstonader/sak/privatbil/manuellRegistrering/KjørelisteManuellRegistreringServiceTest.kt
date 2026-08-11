package no.nav.tilleggsstonader.sak.privatbil.manuellRegistrering

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verifyOrder
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.ekstern.stønad.DagligReisePrivatBilService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.privatbil.InnsendtKjøreliste
import no.nav.tilleggsstonader.sak.privatbil.Kjøreliste
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteDag
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteId
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteService
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørelisteService
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class KjørelisteManuellRegistreringServiceTest {
    private val kjørelisteService = mockk<KjørelisteService>()
    private val behandlingService = mockk<BehandlingService>()
    private val dagligReisePrivatBilService = mockk<DagligReisePrivatBilService>()
    private val avklartKjørelisteService = mockk<AvklartKjørelisteService>(relaxed = true)
    private val kjørelisteJournalpostValidering = mockk<KjørelisteJournalpostValidering>()

    private val service =
        KjørelisteManuellRegistreringService(
            kjørelisteService = kjørelisteService,
            behandlingService = behandlingService,
            dagligReisePrivatBilService = dagligReisePrivatBilService,
            avklartKjørelisteService = avklartKjørelisteService,
            kjørelisteJournalpostValidering = kjørelisteJournalpostValidering,
        )

    @Test
    fun `skal validere journalpost før kjøreliste lagres`() {
        val behandlingId = BehandlingId.random()
        val fagsak = fagsak()
        val behandling = behandling(fagsak = fagsak, id = behandlingId)
        val reiseId = ReiseId.random()
        val fom = 5 januar 2026
        val tom = 11 januar 2026
        val reisedager =
            (0..6).map { offset ->
                KjørelisteDag(
                    dato = fom.plusDays(offset.toLong()),
                    harKjørt = true,
                    parkeringsutgift = 10,
                )
            }
        val request =
            LagreManuellKjørelisteRequest(
                journalpostId = "journalpost-1",
                reiseId = reiseId,
                begrunnelse = "begrunnelse",
                reisedager = reisedager,
            )
        val rammevedtak = RammevedtakPrivatBilUtil.rammeForReiseMedPrivatBil(reiseId = reiseId, fom = fom, tom = tom)
        val forventetKjøreliste =
            Kjøreliste(
                id = KjørelisteId.random(),
                journalpostId = request.journalpostId,
                fagsakId = fagsak.id,
                datoMottatt = LocalDateTime.now(),
                begrunnelse = request.begrunnelse,
                manueltLagretIBehandling = behandlingId,
                data = InnsendtKjøreliste(reiseId = reiseId, reisedager = reisedager),
            )

        justRun { behandlingService.markerBehandlingSomPåbegyntHvisDenHarStatusOpprettet(behandlingId) }
        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { dagligReisePrivatBilService.hentRammevedtakForReiseIBehandling(behandlingId, reiseId) } returns rammevedtak
        every { kjørelisteService.hentForFagsakId(fagsak.id) } returns emptyList()
        justRun { kjørelisteJournalpostValidering.validerJournalpost(behandlingId, request.journalpostId) }
        every {
            kjørelisteService.lagre(
                any(),
                fagsak.id,
                request.journalpostId,
                request.begrunnelse,
                true,
                behandlingId,
            )
        } returns forventetKjøreliste

        val result = service.lagreManuellKjøreliste(behandlingId, request)

        assertThat(result).isEqualTo(forventetKjøreliste)
        verifyOrder {
            behandlingService.markerBehandlingSomPåbegyntHvisDenHarStatusOpprettet(behandlingId)
            behandlingService.hentBehandling(behandlingId)
            kjørelisteJournalpostValidering.validerJournalpost(behandlingId, request.journalpostId)
            kjørelisteService.lagre(any(), fagsak.id, request.journalpostId, request.begrunnelse, true, behandlingId)
        }
    }
}
