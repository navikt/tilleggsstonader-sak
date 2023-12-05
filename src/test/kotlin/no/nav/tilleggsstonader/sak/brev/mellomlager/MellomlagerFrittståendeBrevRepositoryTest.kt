package no.nav.tilleggsstonader.sak.brev.mellomlager

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.temporal.ChronoUnit

internal class MellomlagerFrittståendeBrevRepositoryTest : IntegrationTest() {

    @Autowired
    private lateinit var mellomlagerFrittståendeBrevRepository: MellomlagerFrittståendeBrevRepository

    @Test
    internal fun `skal lagre mellomlagret brev`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val mellomlagretBrev = MellomlagretFrittståendeBrev(
            fagsakId = fagsak.id,
            brevverdier = "{}",
            brevmal = "",
        )

        mellomlagerFrittståendeBrevRepository.insert(mellomlagretBrev)

        val mellomlagretBrevFraDb = mellomlagerFrittståendeBrevRepository.findById(mellomlagretBrev.id)
        Assertions.assertThat(mellomlagretBrevFraDb).get()
            .usingRecursiveComparison()
            .ignoringFields("sporbar.endret.endretTid")
            .isEqualTo(mellomlagretBrev)
        Assertions.assertThat(mellomlagretBrevFraDb.get().sporbar.endret.endretTid)
            .isCloseTo(mellomlagretBrev.sporbar.endret.endretTid, within(1, ChronoUnit.SECONDS))
    }

    @Test
    internal fun `skal finne igjen mellomlagret brev fra fagsakId og saksbehandlers ident`() {
        val saksbehandlerIdent = "12345678910"
        val fagsak = testoppsettService.lagreFagsak(fagsak())

        testWithBrukerContext(saksbehandlerIdent) {
            val mellomlagretBrev = MellomlagretFrittståendeBrev(
                fagsakId = fagsak.id,
                brevverdier = "{}",
                brevmal = "",
            )

            mellomlagerFrittståendeBrevRepository.insert(mellomlagretBrev)
            val mellomlagretBrevFraDb =
                mellomlagerFrittståendeBrevRepository.findByFagsakIdAndSporbarOpprettetAv(fagsak.id, saksbehandlerIdent)
            assertThat(mellomlagretBrevFraDb)
                .usingRecursiveComparison()
                .ignoringFields("sporbar.endret.endretTid")
                .isEqualTo(mellomlagretBrev)

            Assertions.assertThat(mellomlagretBrevFraDb!!.sporbar.endret.endretTid)
                .isCloseTo(mellomlagretBrev.sporbar.endret.endretTid, within(1, ChronoUnit.SECONDS))
        }
    }
}
