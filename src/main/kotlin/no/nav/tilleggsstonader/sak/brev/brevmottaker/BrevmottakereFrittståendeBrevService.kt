package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.tilleggsstonader.kontrakter.brevmottaker.BrevmottakerDto
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerUtil.validerAntallBrevmottakere
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerUtil.validerUnikeBrevmottakere
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerFrittståendeBrev
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.Mottaker
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.MottakerRolle
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.MottakerType
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Brevmottakere for frittstående brev
 * Disse fjernes i det at man sender et frittstående brev, men det opprettes en task for hver mottaker som håndterer utsendelsen
 */
@Service
class BrevmottakereFrittståendeBrevService(
    private val brevmottakereRepository: BrevmottakerFrittståendeBrevRepository,
    private val fagsakService: FagsakService,
) {

    @Transactional
    fun lagreBrevmottakere(fagsakId: FagsakId, brevmottakereDto: BrevmottakereDto) {
        validerAntallBrevmottakere(brevmottakereDto)
        validerUnikeBrevmottakere(brevmottakereDto)
        fjernMottakereIkkeIDto(brevmottakereDto, fagsakId)

        brevmottakereDto.organisasjoner.forEach { lagreEllerOppdater(fagsakId, it) }
        brevmottakereDto.personer.forEach { lagreEllerOppdater(fagsakId, it) }
    }

    @Transactional
    fun hentEllerOpprettBrevmottakere(fagsakId: FagsakId): List<BrevmottakerFrittståendeBrev> {
        val saksbehandlerIdent = SikkerhetContext.hentSaksbehandler()

        if (!brevmottakereRepository.existsByFagsakIdAndSporbarOpprettetAvAndBrevIdIsNull(fagsakId, saksbehandlerIdent)) {
            opprettInitiellBrevmottakerForFagsak(fagsakId)
        }

        return brevmottakereRepository.findByFagsakIdAndSporbarOpprettetAvAndBrevIdIsNull(fagsakId, saksbehandlerIdent)
    }

    fun hentBrevmottakere(id: UUID) = brevmottakereRepository.findByIdOrThrow(id)

    @Transactional
    fun oppdaterBrevmottaker(brevmottaker: BrevmottakerFrittståendeBrev): BrevmottakerFrittståendeBrev {
        return brevmottakereRepository.update(brevmottaker)
    }

    private fun fjernMottakereIkkeIDto(
        brevmottakereDto: BrevmottakereDto,
        fagsakId: FagsakId,
    ) {
        val nyeBrevmottakere = brevmottakereDto.personer.map { it.id } + brevmottakereDto.organisasjoner.map { it.id }
        brevmottakereRepository.findByFagsakIdAndSporbarOpprettetAvAndBrevIdIsNull(
            fagsakId,
            SikkerhetContext.hentSaksbehandler(),
        )
            .filter { it.id !in nyeBrevmottakere }
            .forEach { brevmottakereRepository.deleteById(it.id) }
    }

    private fun lagreEllerOppdater(
        fagsakId: FagsakId,
        dto: BrevmottakerDto,
    ) {
        val brevmottaker = brevmottakereRepository.findByIdOrNull(dto.id)

        if (brevmottaker != null) {
            oppdaterBrevmottaker(brevmottaker, dto)
        } else {
            opprettNyBrevmottaker(fagsakId, dto)
        }
    }

    private fun opprettNyBrevmottaker(fagsakId: FagsakId, it: BrevmottakerDto) {
        brevmottakereRepository.insert(
            BrevmottakerFrittståendeBrev(
                id = it.id,
                fagsakId = fagsakId,
                mottaker = it.tilMottaker(),
            ),
        )
    }

    private fun oppdaterBrevmottaker(brevmottaker: BrevmottakerFrittståendeBrev, it: BrevmottakerDto) {
        brevmottakereRepository.update(brevmottaker.copy(mottaker = it.tilMottaker()))
    }

    private fun opprettInitiellBrevmottakerForFagsak(fagsakId: FagsakId): BrevmottakerFrittståendeBrev {
        val ident = fagsakService.hentAktivIdent(fagsakId)

        val brevmottaker = BrevmottakerFrittståendeBrev(
            fagsakId = fagsakId,
            mottaker = Mottaker(
                ident = ident,
                mottakerRolle = MottakerRolle.BRUKER,
                mottakerType = MottakerType.PERSON,
            ),
        )
        return brevmottakereRepository.insert(brevmottaker)
    }
}
