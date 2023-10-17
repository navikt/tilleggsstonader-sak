package no.nav.tilleggsstonader.sak.brev

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

internal class BrevRepositoryTest : IntegrationTest() {

    @Autowired
    private lateinit var vedtaksbrevRepository: VedtaksbrevRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Test
    internal fun `lagre og hent vedtaksbrev`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val vedtaksbrev = Vedtaksbrev(
            behandlingId = behandling.id,
            saksbehandlerHtml = "noe html",
            saksbehandlersignatur = "Sakliga Behandlersen",
            besluttersignatur = "Beslutter",
            beslutterPdf = null,
            saksbehandlerIdent = "123",
            beslutterIdent = "321",
        )

        vedtaksbrevRepository.insert(vedtaksbrev)

        val brevFraDb = vedtaksbrevRepository.findByIdOrNull(behandling.id)

        assertThat(brevFraDb).isNotNull
        assertThat(brevFraDb).isEqualTo(vedtaksbrev)
    }
}
