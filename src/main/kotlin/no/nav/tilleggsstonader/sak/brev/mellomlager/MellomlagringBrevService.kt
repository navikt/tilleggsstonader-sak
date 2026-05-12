package no.nav.tilleggsstonader.sak.brev.mellomlager

import no.nav.tilleggsstonader.libs.log.logger
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MellomlagringBrevService(
    private val behandlingService: BehandlingService,
    private val mellomlagerBrevRepository: MellomlagerBrevRepository,
    private val mellomlagerFrittståendeBrevRepository: MellomlagerFrittståendeBrevRepository,
    private val transactionHandler: TransactionHandler,
) {
    fun mellomlagreBrev(
        behandlingId: BehandlingId,
        brevverdier: String,
        brevmal: String,
    ): BehandlingId {
        feilHvis(behandlingService.behandlingErLåstForVidereRedigering(behandlingId)) {
            "Kan ikke mellomlagre brev for behandling=$behandlingId når behandlingen er låst."
        }
        val eksisterende = mellomlagerBrevRepository.findByIdOrNull(behandlingId)
        return if (eksisterende == null) {
            opprettMellomlagretBrev(behandlingId, brevverdier, brevmal).behandlingId
        } else {
            oppdaterMellomlagretBrev(eksisterende, brevverdier, brevmal).behandlingId
        }
    }

    private fun opprettMellomlagretBrev(
        behandlingId: BehandlingId,
        brevverdier: String,
        brevmal: String,
    ): MellomlagretBrev =
        try {
            transactionHandler.runInNewTransaction {
                mellomlagerBrevRepository.insert(MellomlagretBrev(behandlingId, brevverdier, brevmal))
            }
        } catch (_: DuplicateKeyException) {
            håndterDuplikatVedOpprettelse(behandlingId, brevverdier, brevmal)
        }

    private fun håndterDuplikatVedOpprettelse(
        behandlingId: BehandlingId,
        brevverdier: String,
        brevmal: String,
    ): MellomlagretBrev {
        logger.info(
            "Mellomlagret brev finnes allerede behandling=$behandlingId (mulig race condition). Oppdaterer eksisterende.",
        )
        return transactionHandler.runInNewTransaction {
            val eksisterende =
                mellomlagerBrevRepository.findByIdOrNull(behandlingId)
                    ?: feil("Fant ikke mellomlagret brev etter DuplicateKeyException for behandling=$behandlingId")
            mellomlagerBrevRepository.update(eksisterende.copy(brevverdier = brevverdier, brevmal = brevmal))
        }
    }

    private fun oppdaterMellomlagretBrev(
        eksisterende: MellomlagretBrev,
        brevverdier: String,
        brevmal: String,
    ): MellomlagretBrev =
        transactionHandler.runInTransaction {
            mellomlagerBrevRepository.update(eksisterende.copy(brevverdier = brevverdier, brevmal = brevmal))
        }

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
