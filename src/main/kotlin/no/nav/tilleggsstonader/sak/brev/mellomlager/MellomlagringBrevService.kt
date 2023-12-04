package no.nav.tilleggsstonader.sak.brev.mellomlager

import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class MellomlagringBrevService(
    private val mellomlagerBrevRepository: MellomlagerBrevRepository,
    private val mellomlagerFrittståendeBrevRepository: MellomlagerFrittståendeBrevRepository,
) {

    fun mellomLagreBrev(behandlingId: UUID, brevverdier: String, brevmal: String): UUID {
        slettMellomlagringHvisFinnes(behandlingId)
        val mellomlagretBrev = MellomlagretBrev(
            behandlingId,
            brevverdier,
            brevmal,
        )
        return mellomlagerBrevRepository.insert(mellomlagretBrev).behandlingId
    }

    fun mellomLagreFrittståendeSanitybrev(
        fagsakId: UUID,
        brevverdier: String,
        brevmal: String,
    ): UUID {
        slettMellomlagretFrittståendeBrev(fagsakId, SikkerhetContext.hentSaksbehandler())
        val mellomlagretBrev = MellomlagretFrittståendeBrev(
            fagsakId = fagsakId,
            brevverdier = brevverdier,
            brevmal = brevmal,
        )
        return mellomlagerFrittståendeBrevRepository.insert(mellomlagretBrev).fagsakId
    }

    fun hentMellomlagretFrittståendeSanitybrev(fagsakId: UUID): MellomlagreBrevDto? =
        mellomlagerFrittståendeBrevRepository.findByFagsakIdAndSporbarEndretEndretAv(
            fagsakId,
            SikkerhetContext.hentSaksbehandler(),
        )?.let { MellomlagreBrevDto(it.brevverdier, it.brevmal) }

    fun hentMellomlagretBrev(behhandlingId: UUID, sanityVersjon: String): MellomlagreBrevDto? =
        mellomlagerBrevRepository.findByIdOrNull(behhandlingId)?.let {
            MellomlagreBrevDto(it.brevverdier, it.brevmal)
        }

    fun slettMellomlagringHvisFinnes(behandlingId: UUID) {
        mellomlagerBrevRepository.deleteById(behandlingId)
    }

    fun slettMellomlagretFrittståendeBrev(fagsakId: UUID, saksbehandlerIdent: String) {
        mellomlagerFrittståendeBrevRepository.findByFagsakIdAndSporbarEndretEndretAv(fagsakId, saksbehandlerIdent)
            ?.let { mellomlagerFrittståendeBrevRepository.deleteById(it.id) }
    }
}
