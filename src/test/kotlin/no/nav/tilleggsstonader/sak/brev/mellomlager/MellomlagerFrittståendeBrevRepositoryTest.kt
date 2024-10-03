package no.nav.tilleggsstonader.sak.brev.mellomlager

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.temporal.ChronoUnit

internal class MellomlagerFrittståendeBrevRepositoryTest : IntegrationTest() {

    @Autowired
    private lateinit var repository: MellomlagerFrittståendeBrevRepository

    private val fagsak = fagsak()

    @BeforeEach
    fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
    }

    @Test
    internal fun `skal lagre mellomlagret brev`() {
        val mellomlagretBrev = repository.insert(mellomlagretFrittståendeBrev())

        val mellomlagretBrevFraDb = repository.findById(mellomlagretBrev.id)
        assertThat(mellomlagretBrevFraDb).get()
            .usingRecursiveComparison()
            .ignoringFields("sporbar.endret.endretTid")
            .isEqualTo(mellomlagretBrev)
        assertThat(mellomlagretBrevFraDb.get().sporbar.endret.endretTid)
            .isCloseTo(mellomlagretBrev.sporbar.endret.endretTid, within(1, ChronoUnit.SECONDS))
    }

    @Test
    internal fun `skal finne igjen mellomlagret brev fra fagsakId og saksbehandlers ident`() {
        val saksbehandlerIdent = "12345678910"
        val fagsak = testoppsettService.lagreFagsak(fagsak())

        testWithBrukerContext(saksbehandlerIdent) {
            val mellomlagretBrev = repository.insert(mellomlagretFrittståendeBrev())
            val mellomlagretBrevFraDb =
                repository.findByFagsakIdAndSporbarOpprettetAv(fagsak.id, saksbehandlerIdent)
            assertThat(mellomlagretBrevFraDb)
                .usingRecursiveComparison()
                .ignoringFields("sporbar.endret.endretTid")
                .isEqualTo(mellomlagretBrev)

            assertThat(mellomlagretBrevFraDb!!.sporbar.endret.endretTid)
                .isCloseTo(mellomlagretBrev.sporbar.endret.endretTid, within(1, ChronoUnit.SECONDS))
        }
    }

    @Test
    fun `mellomlagring er unik per saksbehandler og fagsakId og saksbehandler A skal ikke finne brevet til saksbehandler B`() {
        val saksbehandlerA = "A"
        val saksbehandlerB = "B"

        testWithBrukerContext(saksbehandlerB) {
            repository.insert(mellomlagretFrittståendeBrev())
        }

        assertThat(repository.findByFagsakIdAndSporbarOpprettetAv(fagsak.id, saksbehandlerB)).isNotNull
        assertThat(repository.findByFagsakIdAndSporbarOpprettetAv(FagsakId.random(), saksbehandlerB)).isNull()

        assertThat(repository.findByFagsakIdAndSporbarOpprettetAv(fagsak.id, saksbehandlerA)).isNull()
    }

    private fun mellomlagretFrittståendeBrev() = MellomlagretFrittståendeBrev(
        fagsakId = fagsak.id,
        brevverdier = "{}",
        brevmal = "",
    )
}
