package no.nav.tilleggsstonader.sak.oppfølging

import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import org.springframework.stereotype.Service

@Service
class OppfølgingService(
    private val oppfølgingRepository: OppfølgingRepository,
) {
    fun kontroller(request: KontrollerOppfølgingRequest): OppfølgingMedDetaljer {
        val oppfølging = oppfølgingRepository.findByIdOrThrow(request.id)
        brukerfeilHvis(oppfølging.version != request.version) {
            "Det har allerede skjedd en endring på oppfølging. Last siden på nytt"
        }
        feilHvis(!oppfølging.aktiv) {
            "Kan ikke redigere en oppfølging som ikke lengre er aktiv"
        }
        val kontrollert =
            Kontrollert(
                utfall = request.utfall,
                kommentar = request.kommentar,
            )
        oppfølgingRepository.update(oppfølging.copy(kontrollert = kontrollert))
        return oppfølgingRepository.finnAktivMedDetaljer(oppfølging.behandlingId)
    }

    fun hentAktiveOppfølginger(): List<OppfølgingMedDetaljer> =
        oppfølgingRepository
            .finnAktiveMedDetaljer()
            .sortedBy { it.behandlingsdetaljer.vedtakstidspunkt }
}
