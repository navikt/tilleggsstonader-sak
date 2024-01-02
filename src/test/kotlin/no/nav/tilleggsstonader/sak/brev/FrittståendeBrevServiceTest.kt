package no.nav.tilleggsstonader.sak.brev

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.mockBrukerContext
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class FrittståendeBrevServiceTest : IntegrationTest() {
    @Autowired
    lateinit var frittståendeBrevService: FrittståendeBrevService

    @Autowired
    lateinit var taskService: TaskService

    private val fagsakTilknyttetPesonIdent123 = fagsak(setOf(PersonIdent("123")))

    @BeforeEach
    fun setUp() {
        mockBrukerContext("456")
        testoppsettService.lagreFagsak(fagsakTilknyttetPesonIdent123)
    }

    @Test
    fun `skal opprette task for distribuering etter journalføring`() {
        frittståendeBrevService.sendFrittståendeBrev(
            fagsakId = fagsakTilknyttetPesonIdent123.id,
            request = FrittståendeBrevDto(pdf = "brev".toByteArray(), tittel = "Tittel"),
        )

        val res = taskService.findAll().single()

        Assertions.assertThat(res.type).isEqualTo(DistribuerFrittståendeBrevTask.TYPE)
    }
}
