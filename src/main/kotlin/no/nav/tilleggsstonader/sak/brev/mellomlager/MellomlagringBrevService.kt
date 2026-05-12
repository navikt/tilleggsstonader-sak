package no.nav.tilleggsstonader.sak.brev.mellomlager

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.database.AdvisoryLockService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MellomlagringBrevService(
    private val behandlingService: BehandlingService,
    private val mellomlagerBrevRepository: MellomlagerBrevRepository,
    private val mellomlagerFrittståendeBrevRepository: MellomlagerFrittståendeBrevRepository,
    private val advisoryLockService: AdvisoryLockService,
) {
    @Transactional
    fun mellomlagreBrev(
        behandlingId: BehandlingId,
        brevverdier: String,
        brevmal: String,
    ): BehandlingId {
        feilHvis(behandlingService.behandlingErLåstForVidereRedigering(behandlingId)) {
            "Kan ikke mellomlagre brev for behandling=$behandlingId når behandlingen er låst."
        }
        return advisoryLockService.lockForTransaction(behandlingId) {
            lagreMellomlagretBrev(behandlingId, brevverdier, brevmal)
        }.behandlingId
    }

    private fun lagreMellomlagretBrev(
        behandlingId: BehandlingId,
        brevverdier: String,
        brevmal: String,
    ): MellomlagretBrev =
        mellomlagerBrevRepository.findByIdOrNull(behandlingId)?.let {
            oppdaterMellomlagretBrev(it, brevverdier, brevmal)
        } ?: opprettMellomlagretBrev(behandlingId, brevverdier, brevmal)

    private fun opprettMellomlagretBrev(
        behandlingId: BehandlingId,
        brevverdier: String,
        brevmal: String,
    ): MellomlagretBrev = mellomlagerBrevRepository.insert(MellomlagretBrev(behandlingId, brevverdier, brevmal))

    private fun oppdaterMellomlagretBrev(
        eksisterende: MellomlagretBrev,
        brevverdier: String,
        brevmal: String,
    ): MellomlagretBrev =
        mellomlagerBrevRepository.update(eksisterende.copy(brevverdier = brevverdier, brevmal = brevmal))

    @Transactional
    fun mellomLagreFrittståendeSanitybrev(
        fagsakId: FagsakId,
        brevverdier: String,
        brevmal: String,
    ): FagsakId {
        slettMellomlagretFrittståendeBrev(fagsakId, SikkerhetContext.hentSaksbehandler())
        val mellomlagretBrev =
            MellomlagretFrittståendeBrev(
                fagsakId = fagsakId,
                brevverdier = brevverdier,
                brevmal = brevmal,
            )
        return mellomlagerFrittståendeBrevRepository.insert(mellomlagretBrev).fagsakId
    }

    fun hentMellomlagretFrittståendeSanitybrev(fagsakId: FagsakId): MellomlagreBrevDto? =
        mellomlagerFrittståendeBrevRepository
            .findByFagsakIdAndSporbarOpprettetAv(
                fagsakId,
                SikkerhetContext.hentSaksbehandler(),
            )?.let { MellomlagreBrevDto(it.brevverdier, it.brevmal) }

    fun hentMellomlagretBrev(behandlingId: BehandlingId): MellomlagreBrevDto? =
        mellomlagerBrevRepository.findByIdOrNull(behandlingId)?.let {
            MellomlagreBrevDto(it.brevverdier, it.brevmal)
        }

    fun slettMellomlagretFrittståendeBrev(
        fagsakId: FagsakId,
        saksbehandlerIdent: String,
    ) {
        mellomlagerFrittståendeBrevRepository
            .findByFagsakIdAndSporbarOpprettetAv(fagsakId, saksbehandlerIdent)
            ?.let { mellomlagerFrittståendeBrevRepository.deleteById(it.id) }
    }
}
