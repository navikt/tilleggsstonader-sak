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
                status = "IKKE_SATT"
            )
        )
        val totrinnsstatusFraDb = totrinnskontrollRepository.findByIdOrThrow(totrinnsstatus.id)
        assertThat(totrinnsstatusFraDb).isEqualTo(totrinnsstatus)
    }

    @Nested
    inner class findLastBehandlingIdOrderBySporbarEndretEndretTid {

        @Test
        fun `skal finne xx`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val totrinnsstatus = totrinnskontrollRepository.insert(
                Totrinnsstatus(
                    behandlingId = behandling.id,
                    saksbehandler = "1",
                    status = "IKKE_SATT"
                )
            )
            //totrinnskontrollRepository.findLastBehandlingIdOrderBySporbarEndretEndretTid()
        }
    }
}