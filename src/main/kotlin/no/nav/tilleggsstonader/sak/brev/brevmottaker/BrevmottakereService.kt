package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerUtil.validerUnikeBrevmottakere
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BrevmottakereService(
    val brevmottakereRepository: BrevmottakerRepository,
    private val behandlingService: BehandlingService,
) {

    @Transactional
    fun lagreBrevmottakere(behandlingId: UUID, brevmottakereDto: BrevmottakereDto) {
        validerAntallBrevmottakere(brevmottakereDto)
        validerUnikeBrevmottakere(brevmottakereDto)

        brevmottakereDto.organisasjoner.forEach {
            val brevmottaker = brevmottakereRepository.findByIdOrNull(it.id)

            if (brevmottaker != null) {
                brevmottakereRepository.update(brevmottaker.copy(organisasjonMottaker = it.toDomain()))
            } else {
                brevmottakereRepository.insert(Brevmottaker(behandlingId = behandlingId, organisasjonMottaker = it.toDomain()))
            }
        }
        brevmottakereDto.personer.forEach {
            val brevmottaker = brevmottakereRepository.findByIdOrNull(it.id)

            if (brevmottaker != null) {
                brevmottakereRepository.update(brevmottaker.copy(personMottaker = it.toDomain()))
            } else {
                brevmottakereRepository.insert(Brevmottaker(behandlingId = behandlingId, personMottaker = it.toDomain()))
            }
        }
    }

    fun hentEllerOpprettBrevmottakere(behandlingId: UUID): BrevmottakereDto {
        return if (brevmottakereRepository.existsByBehandlingId(behandlingId)) {
            brevmottakereRepository.findByBehandlingId(behandlingId).tilBrevmottakereDto()
        } else {
            val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)

            val brukerMottaker = BrevmottakerPerson(personIdent = saksbehandling.ident, mottakerRolle = MottakerRolle.BRUKER)

            val brevmottaker = Brevmottaker(
                behandlingId = behandlingId,
                personMottaker = brukerMottaker,
            )
            brevmottakereRepository.insert(brevmottaker)

            BrevmottakereDto(personer = listOf(brukerMottaker.tilDto(brevmottaker.id)), organisasjoner = emptyList())
        }
    }

    private fun List<Brevmottaker>.tilBrevmottakereDto(): BrevmottakereDto =
        BrevmottakereDto(
            personer = this.mapNotNull { it.personMottaker?.tilDto(it.id) },
            organisasjoner = this.mapNotNull { it.organisasjonMottaker?.tilDto(it.id) },
        )

    private fun BrevmottakerOrganisasjonDto.toDomain(): BrevmottakerOrganisasjon =
        BrevmottakerOrganisasjon(organisasjonsnummer, navnHosOrganisasjon, mottakerRolle)

    private fun BrevmottakerPersonDto.toDomain(): BrevmottakerPerson =
        BrevmottakerPerson(personIdent, navn, mottakerRolle)

    private fun BrevmottakerPerson.tilDto(id: UUID): BrevmottakerPersonDto =
        BrevmottakerPersonDto(id, personIdent, navn, mottakerRolle)

    private fun BrevmottakerOrganisasjon.tilDto(id: UUID): BrevmottakerOrganisasjonDto =
        BrevmottakerOrganisasjonDto(id, organisasjonsnummer, navnHosOrganisasjon, mottakerRolle)

    private fun validerAntallBrevmottakere(brevmottakere: BrevmottakereDto) {
        val antallPersonmottakere = brevmottakere.personer.size
        val antallOrganisasjonMottakere = brevmottakere.organisasjoner.size
        val antallMottakere = antallPersonmottakere + antallOrganisasjonMottakere
        brukerfeilHvis(antallMottakere == 0) {
            "Vedtaksbrevet må ha minst 1 mottaker"
        }
        brukerfeilHvis(antallMottakere > 2) {
            "Vedtaksbrevet kan ikke ha mer enn 2 mottakere"
        }
    }
}
