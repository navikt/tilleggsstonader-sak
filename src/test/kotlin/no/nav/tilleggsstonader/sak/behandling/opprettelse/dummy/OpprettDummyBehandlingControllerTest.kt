package no.nav.tilleggsstonader.sak.behandling.opprettelse.dummy

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.log.IdUtils
import no.nav.tilleggsstonader.libs.log.mdc.MDCConstants
import no.nav.tilleggsstonader.libs.test.fnr.FnrGenerator
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadDagligReise
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadLæremidler
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull

class OpprettDummyBehandlingControllerTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var controller: OpprettDummyBehandlingController

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
        val ident = FnrGenerator.generer(1 januar 2000)
        val behandlingId = controller.opprettBehandling(TestBehandlingRequest(ident, stønadstype = Stønadstype.BARNETILSYN))

        Assertions.assertThat(behandlingRepository.findByIdOrNull(behandlingId)).isNotNull
        Assertions.assertThat(barnService.finnBarnPåBehandling(behandlingId)).hasSize(2)
        val søknad = søknadService.hentSøknadBarnetilsyn(behandlingId)!!
        Assertions.assertThat(søknad.barn).hasSize(2)
    }

    @Test
    fun `skal kunne opprette en behandling for læremidler`() {
        val ident = FnrGenerator.generer(1 januar 2000)
        val behandlingId = controller.opprettBehandling(TestBehandlingRequest(ident, stønadstype = Stønadstype.LÆREMIDLER))

        Assertions.assertThat(behandlingRepository.findByIdOrNull(behandlingId)).isNotNull
        val søknad = søknadService.hentSøknadLæremidler(behandlingId)!!
        Assertions.assertThat(søknad).isNotNull
        Assertions.assertThat(søknad).isInstanceOf(SøknadLæremidler::class.java)
    }

    @Test
    fun `skal kunne opprette en behandling for daglig reise TSO`() {
        val ident = FnrGenerator.generer(1 januar 2000)
        val behandlingId = controller.opprettBehandling(TestBehandlingRequest(ident, stønadstype = Stønadstype.DAGLIG_REISE_TSO))

        Assertions.assertThat(behandlingRepository.findByIdOrNull(behandlingId)).isNotNull
        val søknad = søknadService.hentSøknadDagligReise(behandlingId)!!
        Assertions.assertThat(søknad).isNotNull
        Assertions.assertThat(søknad).isInstanceOf(SøknadDagligReise::class.java)
    }

    @Test
    fun `skal kunne opprette en behandling for daglig reise TSR`() {
        val ident = FnrGenerator.generer(1 januar 2000)
        val behandlingId = controller.opprettBehandling(TestBehandlingRequest(ident, stønadstype = Stønadstype.DAGLIG_REISE_TSR))
        Assertions.assertThat(behandlingRepository.findByIdOrNull(behandlingId)).isNotNull
        val søknad = søknadService.hentSøknadDagligReise(behandlingId)!!
        Assertions.assertThat(søknad).isNotNull
        Assertions.assertThat(søknad).isInstanceOf(SøknadDagligReise::class.java)
    }
}
