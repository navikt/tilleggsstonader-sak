package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TostrinnsstatusRepositoryTest : IntegrationTest() {

    @Autowired
    lateinit var totrinnsstatusRepository: TotrinnsstatusRepository

    @Test
    fun `skal kunne lagre og hente totrinnsstatus`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val totrinnsstatus = totrinnsstatusRepository.insert(
            Totrinnsstatus(
                behandlingId = behandling.id,
                saksbehandler = "1",
                status = TotrinnsKontrollStatus.KLAR,
            ),
        )
        val totrinnsstatusFraDb = totrinnsstatusRepository.findByIdOrThrow(totrinnsstatus.id)
        assertThat(totrinnsstatusFraDb).isEqualTo(totrinnsstatus)
    }

    @Nested
    inner class FindLastBehandlingIdOrderBySporbarEndretEndretTid {

        @Test
        fun `skal finne siste totrinnsstatus basert på endret tidspunkt`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val totrinnsstatusFirst = totrinnsstatusRepository.insert(
                Totrinnsstatus(
                    behandlingId = behandling.id,
                    saksbehandler = "1",
                    status = TotrinnsKontrollStatus.KLAR,
                    sporbar = Sporbar(opprettetTid = SporbarUtils.now().plusDays(1)),
                ),

            )
            val totrinnsstatusSecond = totrinnsstatusRepository.insert(
                Totrinnsstatus(
                    behandlingId = behandling.id,
                    saksbehandler = "2",
                    status = TotrinnsKontrollStatus.KLAR,
                    sporbar = Sporbar(opprettetTid = SporbarUtils.now().plusDays(2)),
                ),
            )
            val totrinnsstatusSecondFraDb = totrinnsstatusRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(totrinnsstatusFirst.behandlingId)
            assertThat(totrinnsstatusSecondFraDb).isNotEqualTo(totrinnsstatusFirst)
        }
    }

    @Nested
    inner class FindAllByBehandlingId {
        @Test
        fun `skal finne alle totrinnstatuser på gjeldene behandling`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val totrinnsstatusFirst = totrinnsstatusRepository.insert(
                Totrinnsstatus(
                    behandlingId = behandling.id,
                    saksbehandler = "1",
                    status = TotrinnsKontrollStatus.KLAR,
                    sporbar = Sporbar(opprettetTid = SporbarUtils.now().plusDays(1)),
                ),

            )
            val totrinnsstatusSecond = totrinnsstatusRepository.insert(
                Totrinnsstatus(
                    behandlingId = behandling.id,
                    saksbehandler = "2",
                    status = TotrinnsKontrollStatus.KLAR,
                    sporbar = Sporbar(opprettetTid = SporbarUtils.now().plusDays(2)),
                ),
            )
            val listemedtotrinnsstatusfradb = totrinnsstatusRepository.findAllByBehandlingId(behandling.id)

            assertThat(listemedtotrinnsstatusfradb.count()).isEqualTo(2)
        }
    }

    @Nested
    inner class FindTopByBehandlingIdAndStatusOrderBySporbarEndretEndretTidDesc {
        @Test
        fun `skal finne siste totrinnsstatus basert på status og sist endret`() {
            val behandlingOne = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val totrinnsstatusFirst = totrinnsstatusRepository.insert(
                Totrinnsstatus(
                    behandlingId = behandlingOne.id,
                    saksbehandler = "1",
                    status = TotrinnsKontrollStatus.UNDERKJENT,
                    sporbar = Sporbar(opprettetTid = SporbarUtils.now().plusDays(1)),
                ),

            )
            val totrinnsstatusSecond = totrinnsstatusRepository.insert(
                Totrinnsstatus(
                    behandlingId = behandlingOne.id,
                    saksbehandler = "2",
                    status = TotrinnsKontrollStatus.KLAR,
                    sporbar = Sporbar(opprettetTid = SporbarUtils.now().plusDays(2)),
                ),
            )
            val totrinnsstatusThird = totrinnsstatusRepository.insert(
                Totrinnsstatus(
                    behandlingId = behandlingOne.id,
                    saksbehandler = "2",
                    status = TotrinnsKontrollStatus.UNDERKJENT,
                    sporbar = Sporbar(opprettetTid = SporbarUtils.now().plusDays(3)),
                ),
            )

            val totrinnsstatusFraDb = totrinnsstatusRepository.findTopByBehandlingIdAndStatusOrderBySporbarEndretEndretTidDesc(totrinnsstatusFirst.behandlingId, totrinnsstatusSecond.status)
            assertThat(totrinnsstatusFraDb).isEqualTo(totrinnsstatusSecond)
        }
    }
}
