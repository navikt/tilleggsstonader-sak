package no.nav.tilleggsstonader.sak.brev.frittstående

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerFrittståendeBrevRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.MottakerTestUtil.mottakerPerson
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerFrittståendeBrev
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.Mottaker
import no.nav.tilleggsstonader.sak.brev.mellomlager.MellomlagerFrittståendeBrevRepository
import no.nav.tilleggsstonader.sak.brev.mellomlager.MellomlagretFrittståendeBrev
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.mockBrukerContext
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class FrittståendeBrevServiceTest : IntegrationTest() {

    @Autowired
    lateinit var frittståendeBrevService: FrittståendeBrevService

    @Autowired
    lateinit var frittståendeBrevRepository: FrittståendeBrevRepository

    @Autowired
    lateinit var brevmottakerFrittståendeBrevRepository: BrevmottakerFrittståendeBrevRepository

    @Autowired
    lateinit var mellomlagerFrittståendeBrevRepository: MellomlagerFrittståendeBrevRepository

    @Autowired
    lateinit var taskService: TaskService

    private val fagsak = fagsak(setOf(PersonIdent("123")))

    @BeforeEach
    fun setUp() {
        mockBrukerContext("456")
        testoppsettService.lagreFagsak(fagsak)
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        clearBrukerContext()
    }

    @Test
    fun `skal opprette task for journalføring av frittstående brev`() {
        frittståendeBrevService.sendFrittståendeBrev(
            fagsakId = fagsak.id,
            request = FrittståendeBrevDto(pdf = "brev".toByteArray(), tittel = "Tittel"),
        )

        val task = taskService.findAll().single()

        val brevmottakere = brevmottakerFrittståendeBrevRepository.findAll()
        assertThat(brevmottakere).hasSize(1)
        assertThat(task.type).isEqualTo(JournalførFrittståendeBrevTask.TYPE)
        assertThat(task.payload).contains(brevmottakere.single().id.toString())
    }

    @Test
    fun `skal opprette task for hver brevmottaker`() {
        val mottaker1 =
            brevmottakerFrittståendeBrevRepository.insert(brevmottakerFrittståendeBrev(mottakerPerson(ident = "ident1")))
        val mottaker2 =
            brevmottakerFrittståendeBrevRepository.insert(brevmottakerFrittståendeBrev(mottakerPerson(ident = "ident2")))

        frittståendeBrevService.sendFrittståendeBrev(
            fagsakId = fagsak.id,
            request = FrittståendeBrevDto(pdf = "brev".toByteArray(), tittel = "Tittel"),
        )

        val tasks = taskService.findAll()

        assertThat(tasks).hasSize(2)
        assertThat(tasks.map { it.payload }.firstOrNull { it.contains(mottaker1.id.toString()) }).isNotNull
        assertThat(tasks.map { it.payload }.firstOrNull { it.contains(mottaker2.id.toString()) }).isNotNull
    }

    @Test
    fun `skal oppdatere brevmottaker med brevId når man sender frittstående brev sånn at brevmottakeren senere ikke lengre vises for saksbehandler for nytt brev`() {
        val mottaker1 =
            brevmottakerFrittståendeBrevRepository.insert(brevmottakerFrittståendeBrev(mottakerPerson(ident = "ident1")))

        frittståendeBrevService.sendFrittståendeBrev(
            fagsakId = fagsak.id,
            request = FrittståendeBrevDto(pdf = "brev".toByteArray(), tittel = "Tittel"),
        )
        val frittståendeBrev = frittståendeBrevRepository.findAll().single()
        val oppdatertBrevmottaker = brevmottakerFrittståendeBrevRepository.findByIdOrThrow(mottaker1.id)
        assertThat(oppdatertBrevmottaker.brevId).isEqualTo(frittståendeBrev.id)
    }

    @Test
    fun `skal fjerne mellomlagret brev for saksbehandler og fagsak når man sender brev`() {
        val mellomlagretBrev = MellomlagretFrittståendeBrev(fagsakId = fagsak.id, brevmal = "", brevverdier = "")
        mellomlagerFrittståendeBrevRepository.insert(mellomlagretBrev)

        val annetMellomlagretBrev = opprettMellomlagretBrevForAnnenSaksbehandler()

        frittståendeBrevService.sendFrittståendeBrev(
            fagsakId = fagsak.id,
            request = FrittståendeBrevDto(pdf = "brev".toByteArray(), tittel = "Tittel"),
        )

        val mellomlagredeBrev = mellomlagerFrittståendeBrevRepository.findAll().map { it.id }
        assertThat(mellomlagredeBrev).containsExactly(annetMellomlagretBrev.id)
    }

    private fun opprettMellomlagretBrevForAnnenSaksbehandler(): MellomlagretFrittståendeBrev {
        val mellomlagretBrev = MellomlagretFrittståendeBrev(fagsakId = fagsak.id, brevmal = "", brevverdier = "", sporbar = Sporbar(opprettetAv = "annen"))
        return mellomlagerFrittståendeBrevRepository.insert(mellomlagretBrev)
    }

    private fun brevmottakerFrittståendeBrev(mottaker: Mottaker) =
        BrevmottakerFrittståendeBrev(fagsakId = fagsak.id, mottaker = mottaker)
}
