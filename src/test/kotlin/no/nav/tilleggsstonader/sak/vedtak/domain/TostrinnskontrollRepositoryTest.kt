package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TostrinnskontrollRepositoryTest : IntegrationTest() {

    @Autowired
    lateinit var totrinnskontrollRepository: TotrinnskontrollRepository

    @Test
    fun `skal kunne lagre og hente totrinnsstatus`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val totrinnsstatus = totrinnskontrollRepository.insert(
            Totrinnsstatus(
                behandlingId = behandling.id,
                saksbehandler = "1",
                status = "IKKE_SATT",
            ),
        )
        val totrinnsstatusFraDb = totrinnskontrollRepository.findByIdOrThrow(totrinnsstatus.id)
        assertThat(totrinnsstatusFraDb).isEqualTo(totrinnsstatus)
    }

    @Nested
    inner class FindLastBehandlingIdOrderBySporbarEndretEndretTid {

        @Test
        fun `skal finne siste totrinnsstatus basert på endret tidspunkt`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val totrinnsstatusFirst = totrinnskontrollRepository.insert(
                Totrinnsstatus(
                    behandlingId = behandling.id,
                    saksbehandler = "1",
                    status = "IKKE_SATT",
                ),

            )
            val totrinnsstatusSecond = totrinnskontrollRepository.insert(
                Totrinnsstatus(
                    behandlingId = behandling.id,
                    saksbehandler = "2",
                    status = "IKKE_SATT",
                ),
            )
            val totrinnsstatusSecondFraDb = totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(totrinnsstatusFirst.behandlingId) // findByIdOrThrow(totrinnsstatus.id)

            assertThat(totrinnsstatusSecondFraDb).isNotEqualTo(totrinnsstatusFirst)
        }
    }

    @Nested
    inner class FindAllByBehandlingId {
        @Test
        fun `skal finne alle totrinnstatuser på gjeldene behandling`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val totrinnsstatusFirst = totrinnskontrollRepository.insert(
                Totrinnsstatus(
                    behandlingId = behandling.id,
                    saksbehandler = "1",
                    status = "IKKE_SATT",
                ),

            )
            val totrinnsstatusSecond = totrinnskontrollRepository.insert(
                Totrinnsstatus(
                    behandlingId = behandling.id,
                    saksbehandler = "2",
                    status = "IKKE_SATT",
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

            val totrinnsstatusFirst = totrinnskontrollRepository.insert(
                Totrinnsstatus(
                    behandlingId = behandlingOne.id,
                    saksbehandler = "1",
                    status = "IKKE_SATT",
                ),

            )
            val totrinnsstatusSecond = totrinnskontrollRepository.insert(
                Totrinnsstatus(
                    behandlingId = behandlingOne.id,
                    saksbehandler = "2",
                    status = "TOTRINNSKONTROLL",
                ),
            )
            val totrinnsstatusThird = totrinnskontrollRepository.insert(
                Totrinnsstatus(
                    behandlingId = behandlingOne.id,
                    saksbehandler = "2",
                    status = "KAN_FATTE_VEDTAK",
                ),
            )

            val totrinnsstatusFraDb = totrinnskontrollRepository.findTopByBehandlingIdAndStatusOrderBySporbarEndretEndretTidDesc(totrinnsstatusFirst.behandlingId, totrinnsstatusSecond.status)
            assertThat(totrinnsstatusFraDb).isEqualTo(totrinnsstatusSecond)
        }
    }
}
