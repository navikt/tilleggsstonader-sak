package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.tilleggsstonader.kontrakter.brevmottaker.BrevmottakerOrganisasjonDto
import no.nav.tilleggsstonader.kontrakter.brevmottaker.BrevmottakerPersonDto
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
        validerBehandlingKanRedigeres(behandlingId)
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
            fjernMottakereIkkeIDto(brevmottakereDto, behandlingId)
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
            validerBehandlingKanRedigeres(behandlingId)

            val brevmottaker = opprettBrevmottaker(behandlingId)

            listOf(brevmottaker).tilBrevmottakereDto()
        }
    }

    private fun fjernMottakereIkkeIDto(
        brevmottakereDto: BrevmottakereDto,
        behandlingId: UUID,
    ) {
        val nyeBrevmottakere = brevmottakereDto.personer.map { it.id } + brevmottakereDto.organisasjoner.map { it.id }
        brevmottakereRepository.findByBehandlingId(behandlingId)
            .filter { it.id !in nyeBrevmottakere }
            .forEach { brevmottakereRepository.deleteById(it.id) }
    }

    private fun validerBehandlingKanRedigeres(behandlingId: UUID) {
        brukerfeilHvis(behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke oppdatere brevmottakere fordi behandling er låst for redigering."
        }
    }

    private fun lagreNyBrevmottakerPerson(behandlingId: UUID, it: BrevmottakerPersonDto) {
        brevmottakereRepository.insert(
            Brevmottaker(
                id = it.id,
                behandlingId = behandlingId,
                mottakerRolle = MottakerRolle.valueOf(it.mottakerRolle.name),
                mottakerType = MottakerType.PERSON,
                ident = it.personIdent,
                mottakerNavn = it.navn,
            ),
        )
    }

    private fun lagreNyOrganisasjonsmottaker(behandlingId: UUID, it: BrevmottakerOrganisasjonDto) {
        brevmottakereRepository.insert(
            Brevmottaker(
                id = it.id,
                behandlingId = behandlingId,
                mottakerRolle = MottakerRolle.FULLMAKT,
                mottakerType = MottakerType.ORGANISASJON,
                ident = it.organisasjonsnummer,
                mottakerNavn = it.navnHosOrganisasjon,
                organisasjonsNavn = it.organisasjonsnavn,
            ),
        )
    }

    private fun oppdaterBrevmottakerPerson(brevmottaker: Brevmottaker, it: BrevmottakerPersonDto) {
        brevmottakereRepository.update(
            brevmottaker.copy(
                mottakerRolle = MottakerRolle.valueOf(it.mottakerRolle.name),
                mottakerType = MottakerType.PERSON,
                ident = it.personIdent,
                mottakerNavn = it.navn,
            ),
        )
    }

    private fun oppdaterOrganisasjonsmottaker(brevmottaker: Brevmottaker, it: BrevmottakerOrganisasjonDto) {
        brevmottakereRepository.update(
            brevmottaker.copy(
                ident = it.organisasjonsnummer,
                mottakerNavn = it.navnHosOrganisasjon,
                organisasjonsNavn = it.organisasjonsnavn,
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
