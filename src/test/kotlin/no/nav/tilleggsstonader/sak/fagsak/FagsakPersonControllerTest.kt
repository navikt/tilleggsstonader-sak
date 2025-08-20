package no.nav.tilleggsstonader.sak.fagsak

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.log.mdc.MDCConstants
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired

internal class FagsakPersonControllerTest : IntegrationTest() {
    @Autowired
    private lateinit var fagsakPersonController: FagsakPersonController

    @BeforeEach
    internal fun setUp() {
        MDC.put(MDCConstants.MDC_CALL_ID, "")
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        MDC.remove(MDCConstants.MDC_CALL_ID)
    }

    @Test
    internal fun `skal finne fagsaker til person`() {
        val person = testoppsettService.opprettPerson("1")
        val tilsynBarn = testoppsettService.lagreFagsak(fagsak(person = person, stønadstype = Stønadstype.BARNETILSYN))
        val læremidler = testoppsettService.lagreFagsak(fagsak(person = person, stønadstype = Stønadstype.LÆREMIDLER))
        val boutgifter = testoppsettService.lagreFagsak(fagsak(person = person, stønadstype = Stønadstype.BOUTGIFTER))
        val dagligReiseTsr = testoppsettService.lagreFagsak(fagsak(person = person, stønadstype = Stønadstype.DAGLIG_REISE_TSR))
        val dagligReiseTso = testoppsettService.lagreFagsak(fagsak(person = person, stønadstype = Stønadstype.DAGLIG_REISE_TSO))

        val fagsakPersonDto = testWithBrukerContext { fagsakPersonController.hentFagsakPerson(person.id) }

        assertThat(fagsakPersonDto.tilsynBarn).isEqualTo(tilsynBarn.id)
        assertThat(fagsakPersonDto.læremidler).isEqualTo(læremidler.id)
        assertThat(fagsakPersonDto.boutgifter).isEqualTo(boutgifter.id)
        assertThat(fagsakPersonDto.dagligReiseTsr).isEqualTo(dagligReiseTsr.id)
        assertThat(fagsakPersonDto.dagligReiseTso).isEqualTo(dagligReiseTso.id)
    }
}
