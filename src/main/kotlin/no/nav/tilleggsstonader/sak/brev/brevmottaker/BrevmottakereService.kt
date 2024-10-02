package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.tilleggsstonader.kontrakter.brevmottaker.BrevmottakerDto
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerUtil.validerUnikeBrevmottakere
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerVedtaksbrev
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.Mottaker
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.MottakerRolle
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.MottakerType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BrevmottakereService(
    private val brevmottakereRepository: BrevmottakerVedtaksbrevRepository,
    private val behandlingService: BehandlingService,
) {

    @Transactional
    fun lagreBrevmottakere(behandlingId: BehandlingId, brevmottakereDto: BrevmottakereDto) {
        validerBehandlingKanRedigeres(behandlingId)
        validerAntallBrevmottakere(brevmottakereDto)
        validerUnikeBrevmottakere(brevmottakereDto)
        fjernMottakereIkkeIDto(brevmottakereDto, behandlingId)

        brevmottakereDto.organisasjoner.forEach { lagreEllerOppdater(behandlingId, it) }
        brevmottakereDto.personer.forEach { lagreEllerOppdater(behandlingId, it) }
    }

    @Transactional
    fun hentEllerOpprettBrevmottakere(behandlingId: BehandlingId): BrevmottakereDto {
        return if (brevmottakereRepository.existsByBehandlingId(behandlingId)) {
            brevmottakereRepository.findByBehandlingId(behandlingId).tilBrevmottakereDto()
        } else {
            validerBehandlingKanRedigeres(behandlingId)

            val brevmottaker = opprettInitiellBrevmottakerForBehandling(behandlingId)

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

    private fun lagreEllerOppdater(
        behandlingId: BehandlingId,
        dto: BrevmottakerDto,
    ) {
        val brevmottaker = brevmottakereRepository.findByIdOrNull(dto.id)

        if (brevmottaker != null) {
            oppdaterBrevmottaker(brevmottaker, dto)
        } else {
            opprettNyBrevmottaker(behandlingId, dto)
        }
    }

    private fun opprettNyBrevmottaker(behandlingId: BehandlingId, it: BrevmottakerDto) {
        brevmottakereRepository.insert(
            BrevmottakerVedtaksbrev(
                id = it.id,
                behandlingId = behandlingId,
                mottaker = it.tilMottaker(),
            ),
        )
    }

    private fun oppdaterBrevmottaker(brevmottaker: BrevmottakerVedtaksbrev, it: BrevmottakerDto) {
        brevmottakereRepository.update(brevmottaker.copy(mottaker = it.tilMottaker()))
    }

    private fun opprettInitiellBrevmottakerForBehandling(behandlingId: BehandlingId): BrevmottakerVedtaksbrev {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)

        val brevmottaker = BrevmottakerVedtaksbrev(
            behandlingId = behandlingId,
            mottaker = Mottaker(
                ident = saksbehandling.ident,
                mottakerRolle = MottakerRolle.BRUKER,
                mottakerType = MottakerType.PERSON,
            ),
        )
        return brevmottakereRepository.insert(brevmottaker)
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
