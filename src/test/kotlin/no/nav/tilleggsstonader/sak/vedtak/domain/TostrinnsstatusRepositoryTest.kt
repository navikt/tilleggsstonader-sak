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
    lateinit var totrinnskontrollRepository: TotrinnskontrollRepository

    @Test
    fun `skal kunne lagre og hente totrinnsstatus`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val totrinnskontroll = totrinnskontrollRepository.insert(
            Totrinnskontroll(
                behandlingId = behandling.id,
                saksbehandler = "1",
                status = TotrinnsKontrollStatus.KLAR,
            ),
        )
        val totrinnsstatusFraDb = totrinnskontrollRepository.findByIdOrThrow(totrinnskontroll.id)
        assertThat(totrinnsstatusFraDb).isEqualTo(totrinnskontroll)
    }

    @Nested
    inner class FindLastBehandlingIdOrderBySporbarEndretEndretTid {

        @Test
        fun `skal finne siste totrinnsstatus basert på endret tidspunkt`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val totrinnskontrollFirst = totrinnskontrollRepository.insert(
                Totrinnskontroll(
                    behandlingId = behandling.id,
                    saksbehandler = "1",
                    status = TotrinnsKontrollStatus.KLAR,
                    sporbar = Sporbar(opprettetTid = SporbarUtils.now().plusDays(1)),
                ),

            )
            val totrinnskontrollSecond = totrinnskontrollRepository.insert(
                Totrinnskontroll(
                    behandlingId = behandling.id,
                    saksbehandler = "2",
                    status = TotrinnsKontrollStatus.KLAR,
                    sporbar = Sporbar(opprettetTid = SporbarUtils.now().plusDays(2)),
                ),
            )
            val totrinnsstatusSecondFraDb = totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(totrinnskontrollFirst.behandlingId)
            assertThat(totrinnsstatusSecondFraDb).isNotEqualTo(totrinnskontrollFirst)
        }
    }

    @Nested
    inner class FindAllByBehandlingId {
        @Test
        fun `skal finne alle totrinnstatuser på gjeldene behandling`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val totrinnskontrollFirst = totrinnskontrollRepository.insert(
                Totrinnskontroll(
                    behandlingId = behandling.id,
                    saksbehandler = "1",
                    status = TotrinnsKontrollStatus.KLAR,
                    sporbar = Sporbar(opprettetTid = SporbarUtils.now().plusDays(1)),
                ),

            )
            val totrinnskontrollSecond = totrinnskontrollRepository.insert(
                Totrinnskontroll(
                    behandlingId = behandling.id,
                    saksbehandler = "2",
                    status = TotrinnsKontrollStatus.KLAR,
                    sporbar = Sporbar(opprettetTid = SporbarUtils.now().plusDays(2)),
                ),
            )
            val listemedtotrinnsstatusfradb = totrinnskontrollRepository.findAllByBehandlingId(behandling.id)

            assertThat(listemedtotrinnsstatusfradb.count()).isEqualTo(2)
        }
    }

    @Nested
    inner class FindTopByBehandlingIdAndStatusOrderBySporbarEndretEndretTidDesc {
        @Test
        fun `skal finne siste totrinnsstatus basert på status og sist endret`() {
            val behandlingOne = testoppsettService.opprettBehandlingMedFagsak(behandling())

            val totrinnskontrollFirst = totrinnskontrollRepository.insert(
                Totrinnskontroll(
                    behandlingId = behandlingOne.id,
                    saksbehandler = "1",
                    status = TotrinnsKontrollStatus.UNDERKJENT,
                    sporbar = Sporbar(opprettetTid = SporbarUtils.now().plusDays(1)),
                ),

            )
            val totrinnskontrollSecond = totrinnskontrollRepository.insert(
                Totrinnskontroll(
                    behandlingId = behandlingOne.id,
                    saksbehandler = "2",
                    status = TotrinnsKontrollStatus.KLAR,
                    sporbar = Sporbar(opprettetTid = SporbarUtils.now().plusDays(2)),
                ),
            )
            val totrinnskontrollThird = totrinnskontrollRepository.insert(
                Totrinnskontroll(
                    behandlingId = behandlingOne.id,
                    saksbehandler = "2",
                    status = TotrinnsKontrollStatus.UNDERKJENT,
                    sporbar = Sporbar(opprettetTid = SporbarUtils.now().plusDays(3)),
                ),
            )

            val totrinnsstatusFraDb = totrinnskontrollRepository.findTopByBehandlingIdAndStatusOrderBySporbarEndretEndretTidDesc(totrinnskontrollFirst.behandlingId, totrinnskontrollSecond.status)
            assertThat(totrinnsstatusFraDb).isEqualTo(totrinnskontrollSecond)
        }
    }
}
