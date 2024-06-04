package no.nav.tilleggsstonader.sak.opplysninger.grunnlag

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.grunnlagsdataDomain
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagGrunnlagsdata
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class GrunnlagPatchControllerTest : IntegrationTest() {

    @Autowired
    lateinit var grunnlagPatchController: GrunnlagPatchController

    @Autowired
    lateinit var grunnlagRepository: GrunnlagsdataRepository

    @Test
    fun `skal legge inn arena-grunnlag på behandlinger som mangler`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(), opprettGrunnlagsdata = false)

        grunnlagRepository.insert(
            grunnlagsdataDomain(
                behandlingId = behandling.id,
                grunnlag = lagGrunnlagsdata(arena = null),
            ),
        )
        grunnlagPatchController.patchGrunnlag()

        val oppdatertGrunnlag = grunnlagRepository.findByIdOrThrow(behandling.id)
        assertThat(oppdatertGrunnlag.grunnlag.arena?.vedtakTom).isNotNull()
    }

    @Test
    fun `skal ikke legge inn arena-grunnlag på bnehandling med feil status`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(
            behandling(status = BehandlingStatus.FERDIGSTILT),
            opprettGrunnlagsdata = false,
        )

        grunnlagRepository.insert(
            grunnlagsdataDomain(
                behandlingId = behandling.id,
                grunnlag = lagGrunnlagsdata(arena = null),
            ),
        )
        grunnlagPatchController.patchGrunnlag()

        val oppdatertGrunnlag = grunnlagRepository.findByIdOrThrow(behandling.id)
        assertThat(oppdatertGrunnlag.grunnlag.arena?.vedtakTom).isNull()
    }

    @Test
    fun `skal ikke bruke navnet til ny saksbehandler ved patchning`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(
            behandling(),
            opprettGrunnlagsdata = false,
        )
        testWithBrukerContext("saksbehandler1") {
            grunnlagRepository.insert(
                grunnlagsdataDomain(
                    behandlingId = behandling.id,
                    grunnlag = lagGrunnlagsdata(arena = null),
                ),
            )
        }

        testWithBrukerContext("saksbehandler2") {
            grunnlagPatchController.patchGrunnlag()
        }

        val oppdatertGrunnlag = grunnlagRepository.findByIdOrThrow(behandling.id)
        assertThat(oppdatertGrunnlag.sporbar.endret.endretAv).isEqualTo("VL")
    }
}
