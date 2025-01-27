package no.nav.tilleggsstonader.sak.brev.brevmottaker

import no.nav.tilleggsstonader.kontrakter.brevmottaker.BrevmottakerPersonDto
import no.nav.tilleggsstonader.kontrakter.brevmottaker.MottakerRolle
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.brev.brevmottaker.MottakerTestUtil.mottakerPerson
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerFrittståendeBrev
import no.nav.tilleggsstonader.sak.brev.frittstående.FrittståendeBrev
import no.nav.tilleggsstonader.sak.brev.frittstående.FrittståendeBrevRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.Fil
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class BrevmottakereFrittståendeBrevServiceTest : IntegrationTest() {
    @Autowired
    lateinit var service: BrevmottakereFrittståendeBrevService

    @Autowired
    lateinit var repository: BrevmottakerFrittståendeBrevRepository

    @Autowired
    lateinit var frittståendeBrevRepository: FrittståendeBrevRepository

    val fagsak = fagsak()

    @BeforeEach
    fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        BrukerContextUtil.mockBrukerContext("saksbehandler1")
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        BrukerContextUtil.clearBrukerContext()
    }

    @Nested
    inner class LagreBrevmottakere {
        @Test
        fun `skal fjerne tidligere brevmottakere og opprette nytt hvis id er ukjent`() {
            service.hentEllerOpprettBrevmottakere(fagsak.id)
            val nyBrevmottaker = BrevmottakerPersonDto(UUID.randomUUID(), "annenIdent", "navn", MottakerRolle.VERGE)

            service.lagreBrevmottakere(
                fagsak.id,
                BrevmottakereDto(personer = listOf(nyBrevmottaker), organisasjoner = emptyList()),
            )

            assertThat(repository.findAll().map { it.id }).containsExactly(nyBrevmottaker.id)
        }

        @Test
        fun `skal ikke fjerne brevmottakere på allerede sendte brev`() {
            val frittståendeBrev = opprettFrittståendeBrev()
            val brevmottakerForSendtBrev =
                repository.insert(
                    BrevmottakerFrittståendeBrev(
                        fagsakId = fagsak.id,
                        brevId = frittståendeBrev.id,
                        mottaker = mottakerPerson("ident2"),
                    ),
                )

            service.hentEllerOpprettBrevmottakere(fagsak.id)
            val nyBrevmottaker = BrevmottakerPersonDto(UUID.randomUUID(), "annenIdent", "navn", MottakerRolle.VERGE)

            service.lagreBrevmottakere(
                fagsak.id,
                BrevmottakereDto(personer = listOf(nyBrevmottaker), organisasjoner = emptyList()),
            )

            assertThat(repository.findAll().map { it.id })
                .containsExactlyInAnyOrder(nyBrevmottaker.id, brevmottakerForSendtBrev.id)
        }

        @Test
        fun `oppdaterer brevmottakere hvis id er beholdt`() {
            val brevmottakere = service.hentEllerOpprettBrevmottakere(fagsak.id).single()

            val brevmottakereDto =
                BrevmottakereDto(
                    personer = listOf(brevmottakere.mottaker.tilPersonDto(brevmottakere.id).copy(navn = "nytt navn")),
                    organisasjoner = emptyList(),
                )
            service.lagreBrevmottakere(fagsak.id, brevmottakereDto)

            val alleBrevmottakere = repository.findAll()
            assertThat(alleBrevmottakere).hasSize(1)

            val oppdatertBrevmottakere = alleBrevmottakere.single()
            assertThat(oppdatertBrevmottakere.id).isEqualTo(brevmottakere.id)
            assertThat(oppdatertBrevmottakere.mottaker.mottakerNavn).isEqualTo("nytt navn")
        }
    }

    @Nested
    inner class HentEllerOpprettBrevmottakere {
        @Test
        fun `skal opprette og returnere brevmottakere hvis det ikke finnes noen`() {
            val brevmottakere = service.hentEllerOpprettBrevmottakere(fagsak.id)

            assertThat(brevmottakere).hasSize(1)
            assertThat(repository.findAll()).hasSize(1)
        }

        @Test
        fun `oppretter ny brevmottakere dersom brev allerede er sendt`() {
            val frittståendeBrev = opprettFrittståendeBrev()
            val brevmottakerForSendtBrev =
                repository.insert(
                    BrevmottakerFrittståendeBrev(
                        fagsakId = fagsak.id,
                        brevId = frittståendeBrev.id,
                        mottaker = mottakerPerson("ident2"),
                    ),
                )

            val brevmottakere = service.hentEllerOpprettBrevmottakere(fagsak.id)

            assertThat(brevmottakere.map { it.id }).doesNotContain(brevmottakerForSendtBrev.id)
            assertThat(repository.findAll()).hasSize(2)
        }

        @Test
        fun `skal ikke få opp brevmottakere til annen saksbehandler eller annen stønadstype`() {
            val annenFagsak = testoppsettService.lagreFagsak(fagsak(stønadstype = Stønadstype.LÆREMIDLER))

            repository.insert(
                BrevmottakerFrittståendeBrev(
                    fagsakId = annenFagsak.id,
                    mottaker = mottakerPerson("ident"),
                ),
            )

            repository.insert(
                BrevmottakerFrittståendeBrev(
                    fagsakId = annenFagsak.id,
                    mottaker = mottakerPerson("ident"),
                    sporbar = Sporbar(opprettetAv = "annenSaksbehandler"),
                ),
            )

            val brevmottakerForSaksbehandler =
                repository.insert(
                    BrevmottakerFrittståendeBrev(
                        fagsakId = fagsak.id,
                        mottaker = mottakerPerson("ident"),
                    ),
                )

            assertThat(service.hentEllerOpprettBrevmottakere(fagsak.id).map { it.id })
                .containsExactly(brevmottakerForSaksbehandler.id)
        }
    }

    private fun opprettFrittståendeBrev() =
        frittståendeBrevRepository.insert(
            FrittståendeBrev(
                fagsakId = fagsak.id,
                pdf = Fil("".toByteArray()),
                tittel = "tittel",
            ),
        )
}
