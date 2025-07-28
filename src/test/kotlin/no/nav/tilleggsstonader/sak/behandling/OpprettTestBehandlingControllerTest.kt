package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.log.IdUtils
import no.nav.tilleggsstonader.libs.log.mdc.MDCConstants
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
import org.slf4j.MDC
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
        MDC.put(MDCConstants.MDC_CALL_ID, IdUtils.generateId())
    }

    @AfterEach
    override fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
        MDC.remove(MDCConstants.MDC_CALL_ID)
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

    @Test
    fun `skal kunne opprette en behandling for daglig reise TSO`() {
        val ident = FnrGenerator.generer(2000, 1, 1)
        val behandlingId = controller.opprettBehandling(TestBehandlingRequest(ident, stønadstype = Stønadstype.DAGLIG_REISE_TSO))

        assertThat(behandlingRepository.findByIdOrNull(behandlingId)).isNotNull
        val søknad = søknadService.hentSøknadDagligReiseTSO(behandlingId)!!
        assertThat(søknad).isNotNull
        assertThat(søknad).isInstanceOf(SøknadLæremidler::class.java)
    }

    @Test
    fun `skal kunne opprette en behandling for daglig reise TSR`() {
        val ident = FnrGenerator.generer(2000, 1, 1)
        val behandlingId = controller.opprettBehandling(TestBehandlingRequest(ident, stønadstype = Stønadstype.DAGLIG_REISE_TSR))

        assertThat(behandlingRepository.findByIdOrNull(behandlingId)).isNotNull
        val søknad = søknadService.hentSøknadDagligReiseTSR(behandlingId)!!
        assertThat(søknad).isNotNull
        assertThat(søknad).isInstanceOf(SøknadLæremidler::class.java)
    }
}
