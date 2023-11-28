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
    private val brevmottakereRepository: BrevmottakerRepository,
    private val behandlingService: BehandlingService,
) {

    @Transactional
    fun lagreBrevmottakere(behandlingId: UUID, brevmottakereDto: BrevmottakereDto) {
        validerAntallBrevmottakere(brevmottakereDto)
        validerUnikeBrevmottakere(brevmottakereDto)

        brevmottakereDto.organisasjoner.forEach {
            val brevmottaker = brevmottakereRepository.findByIdOrNull(it.id)

            if (brevmottaker != null) {
                oppdaterOrganisasjonsmottaker(brevmottaker, it)
            } else {
                lagreNyOrganisasjonsmottaker(behandlingId, it)
            }
        }
        brevmottakereDto.personer.forEach {
            val brevmottaker = brevmottakereRepository.findByIdOrNull(it.id)

            if (brevmottaker != null) {
                oppdaterBrevmottakerPerson(brevmottaker, it)
            } else {
                lagreNyBrevmottakerPerson(behandlingId, it)
            }
        }
    }

    @Transactional
    fun hentEllerOpprettBrevmottakere(behandlingId: UUID): BrevmottakereDto {
        return if (brevmottakereRepository.existsByBehandlingId(behandlingId)) {
            brevmottakereRepository.findByBehandlingId(behandlingId).tilBrevmottakereDto()
        } else {
            val brevmottaker = opprettBrevmottaker(behandlingId)

            listOf(brevmottaker).tilBrevmottakereDto()
        }
    }

    private fun lagreNyBrevmottakerPerson(behandlingId: UUID, it: BrevmottakerPersonDto) {
        brevmottakereRepository.insert(
            Brevmottaker(
                behandlingId = behandlingId,
                ident = it.personIdent,
                mottakerRolle = it.mottakerRolle,
                mottakerType = MottakerType.PERSON,
            ),
        )
    }

    private fun lagreNyOrganisasjonsmottaker(behandlingId: UUID, it: BrevmottakerOrganisasjonDto) {
        brevmottakereRepository.insert(
            Brevmottaker(
                behandlingId = behandlingId,
                ident = it.organisasjonsnummer,
                mottakerRolle = it.mottakerRolle,
                mottakerType = MottakerType.ORGANISASJON,
                navnHosOrganisasjon = it.navnHosOrganisasjon,
            ),
        )
    }

    private fun oppdaterBrevmottakerPerson(brevmottaker: Brevmottaker, it: BrevmottakerPersonDto) {
        brevmottakereRepository.update(
            brevmottaker.copy(
                ident = it.personIdent,
                mottakerRolle = it.mottakerRolle,
            ),
        )
    }

    private fun oppdaterOrganisasjonsmottaker(brevmottaker: Brevmottaker, it: BrevmottakerOrganisasjonDto) {
        brevmottakereRepository.update(
            brevmottaker.copy(
                mottakerRolle = it.mottakerRolle,
                ident = it.organisasjonsnummer,
                navnHosOrganisasjon = it.navnHosOrganisasjon,
            ),
        )
    }

    private fun opprettBrevmottaker(behandlingId: UUID): Brevmottaker {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)

        val brevmottaker = Brevmottaker(
            behandlingId = behandlingId,
            ident = saksbehandling.ident,
            mottakerRolle = MottakerRolle.BRUKER,
            mottakerType = MottakerType.PERSON,
        )
        brevmottakereRepository.insert(brevmottaker)
        return brevmottaker
    }

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
