package no.nav.tilleggsstonader.sak.brev.mellomlager

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service

@Service
class MellomlagringBrevService(
    private val mellomlagerBrevRepository: MellomlagerBrevRepository,
    private val mellomlagerFrittståendeBrevRepository: MellomlagerFrittståendeBrevRepository,
) {

    fun mellomLagreBrev(behandlingId: BehandlingId, brevverdier: String, brevmal: String): BehandlingId {
        slettMellomlagringHvisFinnes(behandlingId)
        val mellomlagretBrev = MellomlagretBrev(
            behandlingId,
            brevverdier,
            brevmal,
        )
        return mellomlagerBrevRepository.insert(mellomlagretBrev).behandlingId
    }

    fun mellomLagreFrittståendeSanitybrev(
        fagsakId: FagsakId,
        brevverdier: String,
        brevmal: String,
    ): FagsakId {
        slettMellomlagretFrittståendeBrev(fagsakId, SikkerhetContext.hentSaksbehandler())
        val mellomlagretBrev = MellomlagretFrittståendeBrev(
            fagsakId = fagsakId,
            brevverdier = brevverdier,
            brevmal = brevmal,
        )
        return mellomlagerFrittståendeBrevRepository.insert(mellomlagretBrev).fagsakId
    }

    fun hentMellomlagretFrittståendeSanitybrev(fagsakId: FagsakId): MellomlagreBrevDto? =
        mellomlagerFrittståendeBrevRepository.findByFagsakIdAndSporbarOpprettetAv(
            fagsakId,
            SikkerhetContext.hentSaksbehandler(),
        )?.let { MellomlagreBrevDto(it.brevverdier, it.brevmal) }

    fun hentMellomlagretBrev(behhandlingId: BehandlingId): MellomlagreBrevDto? =
        mellomlagerBrevRepository.findByIdOrNull(behhandlingId)?.let {
            MellomlagreBrevDto(it.brevverdier, it.brevmal)
        }

    fun slettMellomlagringHvisFinnes(behandlingId: BehandlingId) {
        mellomlagerBrevRepository.deleteById(behandlingId)
    }

    fun slettMellomlagretFrittståendeBrev(fagsakId: FagsakId, saksbehandlerIdent: String) {
        mellomlagerFrittståendeBrevRepository.findByFagsakIdAndSporbarOpprettetAv(fagsakId, saksbehandlerIdent)
            ?.let { mellomlagerFrittståendeBrevRepository.deleteById(it.id) }
    }
}
