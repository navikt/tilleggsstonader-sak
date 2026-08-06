package no.nav.tilleggsstonader.sak.privatbil

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import org.springframework.stereotype.Service

@Service
class KjørelisteService(
    private val repository: KjørelisteRepository,
) {
    fun lagre(
        innsendtKjøreliste: InnsendtKjøreliste,
        fagsakId: FagsakId,
        journalpostId: String,
        begrunnelse: String? = null,
        manueltRegistrert: Boolean,
        behandlingId: BehandlingId? = null,
    ): Kjøreliste {
        val kjørseliste =
            Kjøreliste(
                journalpostId = journalpostId,
                fagsakId = fagsakId,
                datoMottatt = SporbarUtils.now(),
                begrunnelse = begrunnelse,
                manueltLagretIBehandling = behandlingId,
                data = innsendtKjøreliste,
            )
        return repository.insert(kjørseliste)
    }

    fun hentManueltLagredeIBehandling(behandlingId: BehandlingId): List<Kjøreliste> =
        repository.findByManueltLagretIBehandling(behandlingId)

    fun slettKjørelister(kjørelister: List<Kjøreliste>) {
        repository.deleteAll(kjørelister)
    }

    fun hentForFagsakId(fagsakId: FagsakId): List<Kjøreliste> = repository.findByFagsakId(fagsakId)

    fun hentKjøreliste(kjørelisteId: KjørelisteId): Kjøreliste = repository.findByIdOrThrow(kjørelisteId)
}
