package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.test.fnr.FnrGenerator
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadLæremidler
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

class OpprettTestBehandlingControllerTest : IntegrationTest() {

    @Autowired
    lateinit var controller: OpprettTestBehandlingController

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var barnService: BarnService

    @Autowired
    lateinit var søknadService: SøknadService

    @BeforeEach
    fun setUp() {
        BrukerContextUtil.mockBrukerContext()
    }

    @AfterEach
    override fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    fun `skal kunne opprette en behandling for barnetilsyn`() {
        val ident = FnrGenerator.generer(2000, 1, 1)
        val behandlingId = controller.opprettBehandling(TestBehandlingRequest(ident, stønadstype = Stønadstype.BARNETILSYN))

        assertThat(behandlingRepository.findByIdOrNull(behandlingId)).isNotNull
        assertThat(barnService.finnBarnPåBehandling(behandlingId)).hasSize(2)
        val søknad = søknadService.hentSøknadBarnetilsyn(behandlingId)!!
        assertThat(søknad.barn).hasSize(2)
    }

    @Test
    fun `skal kunne opprette en behandling for læremidler`() {
        val ident = FnrGenerator.generer(2000, 1, 1)
        val behandlingId = controller.opprettBehandling(TestBehandlingRequest(ident, stønadstype = Stønadstype.LÆREMIDLER))

        assertThat(behandlingRepository.findByIdOrNull(behandlingId)).isNotNull
        val søknad = søknadService.hentSøknadLæremidler(behandlingId)!!
        assertThat(søknad).isNotNull
        assertThat(søknad).isInstanceOf(SøknadLæremidler::class.java)
    }
}
