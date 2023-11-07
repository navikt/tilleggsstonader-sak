package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.PdlClientConfig.Companion.barn2Fnr
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.PdlClientConfig.Companion.barnFnr
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.util.SøknadUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.søknadBarnTilBehandlingBarn
import no.nav.tilleggsstonader.sak.vilkår.VilkårService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class TestSaksbehandlingServiceTest() : IntegrationTest() {

    @Autowired
    lateinit var testSaksbehandlingService: TestSaksbehandlingService

    @Autowired
    lateinit var søknadService: SøknadService

    @Autowired
    lateinit var vilkårService: VilkårService

    @Autowired
    lateinit var barnRepository: BarnRepository

    @Test
    internal fun `skal oppfylle alle vilkår`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(
            behandling(
                status = BehandlingStatus.UTREDES,
                steg = StegType.SEND_TIL_BESLUTTER,
            ),
        )
        val skjema = SøknadUtil.søknadskjemaBarnetilsyn(
            barnMedBarnepass = listOf(
                SøknadUtil.barnMedBarnepass(ident = barnFnr, navn = "navn1"),
                SøknadUtil.barnMedBarnepass(ident = barn2Fnr, navn = "navn1"),
            ),
        )
        val søknad = søknadService.lagreSøknad(behandling.id, "journalpostId", skjema)
        barnRepository.insertAll(søknadBarnTilBehandlingBarn(søknad.barn, behandling.id))

        vilkårService.hentEllerOpprettVilkårsvurdering(behandling.id)

        testSaksbehandlingService.utfyllVilkår(behandlingId = behandling.id)
        assertThat(vilkårService.erAlleVilkårOppfylt(behandling.id)).isTrue
    }
}
