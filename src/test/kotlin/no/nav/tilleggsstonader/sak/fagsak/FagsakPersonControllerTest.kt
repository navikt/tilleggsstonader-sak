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

        val fagsakPersonDto = testWithBrukerContext { fagsakPersonController.hentFagsakPerson(person.id) }

        assertThat(fagsakPersonDto.tilsynBarn).isEqualTo(tilsynBarn.id)
        assertThat(fagsakPersonDto.læremidler).isEqualTo(læremidler.id)
    }

    @Test
    internal fun `skal finne utvidede fagsaker til person`() {
        val person = testoppsettService.opprettPerson("1")
        val tilsynBarn = testoppsettService.lagreFagsak(fagsak(person = person, stønadstype = Stønadstype.BARNETILSYN))
        val læremidler = testoppsettService.lagreFagsak(fagsak(person = person, stønadstype = Stønadstype.LÆREMIDLER))

        val fagsakPersonDto = testWithBrukerContext { fagsakPersonController.hentFagsakPersonUtvidet(person.id) }

        assertThat(fagsakPersonDto.tilsynBarn?.id).isEqualTo(tilsynBarn.id)
        assertThat(fagsakPersonDto.læremidler?.id).isEqualTo(læremidler.id)
    }
}
