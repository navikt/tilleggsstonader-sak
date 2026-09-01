package no.nav.tilleggsstonader.sak.privatbil.manuellRegistrering

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.journalpost.Sak
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.journalpost
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class KjørelisteJournalpostValideringTest {
    private val fagsakService = mockk<FagsakService>()
    private val journalpostService = mockk<JournalpostService>()
    private val validering = KjørelisteJournalpostValideringService(fagsakService, journalpostService)

    @Test
    fun `skal godta journalpost som finnes på fagsaken`() {
        val behandlingId =
            no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
                .random()
        val fagsakId = FagsakId.random()
        val fagsak = fagsak(id = fagsakId, eksternId = EksternFagsakId(id = 1, fagsakId = fagsakId))
        val journalpostId = "123"

        every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak
        every { journalpostService.hentJournalpost(journalpostId) } returns
            journalpost(
                journalpostId = journalpostId,
                sak = Sak(fagsakId = fagsak.eksternId.id.toString()),
            )

        validering.validerJournalpost(behandlingId, journalpostId)

        verify(exactly = 1) { journalpostService.hentJournalpost(journalpostId) }
    }

    @Test
    fun `skal feile når journalpost ligger på annen fagsak`() {
        val behandlingId =
            no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
                .random()
        val fagsakId = FagsakId.random()
        val fagsak = fagsak(id = fagsakId, eksternId = EksternFagsakId(id = 1, fagsakId = fagsakId))
        val annenFagsakId = FagsakId.random()
        val annenFagsak = fagsak(id = annenFagsakId, eksternId = EksternFagsakId(id = 2, fagsakId = annenFagsakId))

        val journalpost =
            journalpost(
                journalpostId = "999",
                sak = Sak(fagsakId = annenFagsak.eksternId.id.toString()),
            )
        every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak
        every { journalpostService.hentJournalpost("999") } returns journalpost

        assertThatThrownBy {
            validering.validerJournalpost(behandlingId, "999")
        }.hasMessageContaining(
            "Journalpost med id=999 finnes ikke på saksnummer ${fagsak.eksternId.id}, men på ${journalpost.sak?.fagsakId}. Journalfør dokumentet på riktig saksnummer i gosys.",
        )
    }
}
