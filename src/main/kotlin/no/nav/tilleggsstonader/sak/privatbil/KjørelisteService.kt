package no.nav.tilleggsstonader.sak.privatbil

import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class KjørelisteService(
    private val repository: KjørelisteRepository,
) {
    fun lagre(
        innsendtKjøreliste: InnsendtKjøreliste,
        fagsakId: FagsakId,
        journalpostId: String,
    ): Kjøreliste {
        val kjørseliste =
            Kjøreliste(
                journalpostId = journalpostId,
                fagsakId = fagsakId,
                datoMottatt = SporbarUtils.now(),
                data = innsendtKjøreliste,
            )
        return repository.insert(kjørseliste)
    }

    fun hentForFagsakId(fagsakId: FagsakId): List<Kjøreliste> = repository.findByFagsakId(fagsakId)

    fun hentKjøreliste(kjørelisteId: UUID): Kjøreliste = repository.findByIdOrThrow(kjørelisteId)
}
