package no.nav.tilleggsstonader.sak.klage

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.klage.FagsystemType
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandling.domain.EksternBehandlingId
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class EksternKlageVedtakServiceTest {

    private val fagsakService = mockk<FagsakService>()
    private val behandlingService = mockk<BehandlingService>()

    private val service = EksternKlageVedtakService(
        fagsakService = fagsakService,
        behandlingService = behandlingService,
    )

    private val fagsakId = FagsakId.random()
    private val fagsak = fagsak(id = fagsakId)
    private val eksternFagsakId = fagsak.eksternId.id

    private val eksternBehandlingId = "100"

    @BeforeEach
    internal fun setUp() {
        every { fagsakService.hentFagsakPåEksternId(eksternFagsakId) } returns fagsak
        every { behandlingService.hentEksternBehandlingId(any<BehandlingId>()) } returns EksternBehandlingId(
            eksternBehandlingId.toLong(),
            BehandlingId.random(),
        )
    }

    @Test
    internal fun `skal mappe ferdigstilte behandlinger til fagsystemVedtak`() {
        val vedtakstidspunkt = LocalDateTime.now()
        val behandling = ferdigstiltBehandling(vedtakstidspunkt)

        every { behandlingService.hentBehandlinger(fagsak.id) } returns listOf(behandling)

        val vedtak = service.hentVedtak(eksternFagsakId)
        assertThat(vedtak).hasSize(1)
        assertThat(vedtak[0].resultat).isEqualTo("Avslått")
        assertThat(vedtak[0].behandlingstype).isEqualTo("Førstegangsbehandling")
        assertThat(vedtak[0].eksternBehandlingId).isEqualTo(eksternBehandlingId)
        assertThat(vedtak[0].vedtakstidspunkt).isEqualTo(vedtakstidspunkt)
        assertThat(vedtak[0].fagsystemType).isEqualTo(FagsystemType.ORDNIÆR)
    }

    @Test
    internal fun `skal ikke returnere henlagte behandlinger`() {
        val henlagtBehandling = behandling(
            fagsak = fagsak,
            type = BehandlingType.REVURDERING,
            status = BehandlingStatus.FERDIGSTILT,
            resultat = BehandlingResultat.HENLAGT,
        )
        every { behandlingService.hentBehandlinger(any<FagsakId>()) } returns listOf(henlagtBehandling)

        assertThat(service.hentVedtak(eksternFagsakId)).isEmpty()
    }

    @Test
    internal fun `skal ikke returnere behandlinger under behandling`() {
        val henlagtBehandling = behandling(
            fagsak = fagsak,
            type = BehandlingType.REVURDERING,
            status = BehandlingStatus.UTREDES,
            resultat = BehandlingResultat.IKKE_SATT,
        )
        every { behandlingService.hentBehandlinger(any<FagsakId>()) } returns listOf(henlagtBehandling)

        assertThat(service.hentVedtak(eksternFagsakId)).isEmpty()
    }

    private fun ferdigstiltBehandling(vedtakstidspunkt: LocalDateTime?): Behandling = behandling(
        fagsak,
        vedtakstidspunkt = vedtakstidspunkt,
        resultat = BehandlingResultat.AVSLÅTT,
        type = BehandlingType.FØRSTEGANGSBEHANDLING,
        status = BehandlingStatus.FERDIGSTILT,
    )
}
