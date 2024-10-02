package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.tilleggsstonader.kontrakter.brevmottaker.BrevmottakerOrganisasjonDto
import no.nav.tilleggsstonader.kontrakter.brevmottaker.BrevmottakerPersonDto
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerUtil.validerUnikeBrevmottakere
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BrevmottakereService(
    private val brevmottakereRepository: BrevmottakerRepository,
    private val behandlingService: BehandlingService,
) {

    @Transactional
    fun lagreBrevmottakere(behandlingId: BehandlingId, brevmottakereDto: BrevmottakereDto) {
        validerBehandlingKanRedigeres(behandlingId)
        validerAntallBrevmottakere(brevmottakereDto)
        validerUnikeBrevmottakere(brevmottakereDto)
        fjernMottakereIkkeIDto(brevmottakereDto, behandlingId)

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
    fun hentEllerOpprettBrevmottakere(behandlingId: BehandlingId): BrevmottakereDto {
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
        behandlingId: BehandlingId,
    ) {
        val nyeBrevmottakere = brevmottakereDto.personer.map { it.id } + brevmottakereDto.organisasjoner.map { it.id }
        brevmottakereRepository.findByBehandlingId(behandlingId)
            .filter { it.id !in nyeBrevmottakere }
            .forEach { brevmottakereRepository.deleteById(it.id) }
    }

    private fun validerBehandlingKanRedigeres(behandlingId: BehandlingId) {
        brukerfeilHvis(behandlingService.hentBehandling(behandlingId).status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke oppdatere brevmottakere fordi behandling er låst for redigering."
        }
    }

    private fun lagreNyBrevmottakerPerson(behandlingId: BehandlingId, it: BrevmottakerPersonDto) {
        brevmottakereRepository.insert(
            BrevmottakerVedtaksbrev(
                id = it.id,
                behandlingId = behandlingId,
                mottaker = it.tilMottaker(),
            ),
        )
    }

    private fun lagreNyOrganisasjonsmottaker(behandlingId: BehandlingId, it: BrevmottakerOrganisasjonDto) {
        brevmottakereRepository.insert(
            BrevmottakerVedtaksbrev(
                id = it.id,
                behandlingId = behandlingId,
                mottaker = it.tilMottaker(),
            ),
        )
    }

    private fun oppdaterBrevmottakerPerson(brevmottaker: BrevmottakerVedtaksbrev, it: BrevmottakerPersonDto) {
        brevmottakereRepository.update(brevmottaker.copy(mottaker = it.tilMottaker()))
    }

    private fun oppdaterOrganisasjonsmottaker(brevmottaker: BrevmottakerVedtaksbrev, it: BrevmottakerOrganisasjonDto) {
        brevmottakereRepository.update(brevmottaker.copy(mottaker = it.tilMottaker()))
    }

    private fun opprettBrevmottaker(behandlingId: BehandlingId): BrevmottakerVedtaksbrev {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)

        val brevmottaker = BrevmottakerVedtaksbrev(
            behandlingId = behandlingId,
            mottaker = Mottaker(
                ident = saksbehandling.ident,
                mottakerRolle = MottakerRolle.BRUKER,
                mottakerType = MottakerType.PERSON,
            ),
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
