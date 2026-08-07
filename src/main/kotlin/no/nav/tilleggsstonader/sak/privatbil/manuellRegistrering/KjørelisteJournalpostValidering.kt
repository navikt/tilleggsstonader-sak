package no.nav.tilleggsstonader.sak.privatbil.manuellRegistrering

import no.nav.tilleggsstonader.libs.feil.brukerfeilHvis
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.journalføring.JournalpostService
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

interface KjørelisteJournalpostValidering {
    fun validerJournalpost(
        behandlingId: BehandlingId,
        journalpostId: String,
    )
}

@Service
@Profile("!mock-journalpost")
class KjørelisteJournalpostValideringService(
    private val fagsakService: FagsakService,
    private val journalpostService: JournalpostService,
) : KjørelisteJournalpostValidering {
    override fun validerJournalpost(
        behandlingId: BehandlingId,
        journalpostId: String,
    ) {
        val fagsak = fagsakService.hentFagsakForBehandling(behandlingId)
        val journalpost = journalpostService.hentJournalpost(journalpostId)
        val journalpostFagsakId = journalpost.sak?.fagsakId

        brukerfeilHvis(journalpostFagsakId != fagsak.eksternId.id.toString()) {
            "Journalpost med id=$journalpostId finnes ikke på saksnummer ${fagsak.eksternId.id}. Journalfør dokumentet på riktig saksnummer i gosys."
        }
    }
}

@Service
@Profile("mock-journalpost")
class KjørelisteJournalpostValideringNoop : KjørelisteJournalpostValidering {
    override fun validerJournalpost(
        behandlingId: BehandlingId,
        journalpostId: String,
    ) = Unit
}
