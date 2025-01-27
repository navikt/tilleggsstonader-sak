package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

class TostrinnskontrollRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var totrinnskontrollRepository: TotrinnskontrollRepository

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `skal kunne lagre og hente totrinnskontroll`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val totrinnskontroll =
            lagreOgOppdaterEndretTidTilOpprettetTid(
                Totrinnskontroll(
                    behandlingId = behandling.id,
                    saksbehandler = "1",
                    status = TotrinnInternStatus.GODKJENT,
                ),
            )
        val totrinnskontrollFraDb = totrinnskontrollRepository.findByIdOrThrow(totrinnskontroll.id)
        assertThat(totrinnskontrollFraDb).isEqualTo(totrinnskontroll)
    }

    @Nested
    inner class FindLastBehandlingIdOrderBySporbarEndretEndretTid {
        @Test
        fun `skal finne siste totrinnskontroll basert på endret tidspunkt`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            val t =
                Totrinnskontroll(
                    behandlingId = behandling.id,
                    saksbehandler = "1",
                    status = TotrinnInternStatus.GODKJENT,
                    sporbar = Sporbar(opprettetTid = SporbarUtils.now().plusDays(1)),
                )
            val totrinnskontrollFirst = lagreOgOppdaterEndretTidTilOpprettetTid(t)
            assertThat(totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(behandling.id))
                .isEqualTo(totrinnskontrollFirst)

            val totrinnskontrollSecond =
                lagreOgOppdaterEndretTidTilOpprettetTid(
                    Totrinnskontroll(
                        behandlingId = behandling.id,
                        saksbehandler = "2",
                        status = TotrinnInternStatus.GODKJENT,
                        sporbar = Sporbar(opprettetTid = SporbarUtils.now().plusDays(2)),
                    ),
                )
            assertThat(totrinnskontrollRepository.findTopByBehandlingIdOrderBySporbarEndretEndretTidDesc(behandling.id))
                .isEqualTo(totrinnskontrollSecond)
        }
    }

    private fun lagreOgOppdaterEndretTidTilOpprettetTid(totrinnskontroll: Totrinnskontroll): Totrinnskontroll {
        totrinnskontrollRepository.insert(totrinnskontroll)
        jdbcTemplate.update(
            "UPDATE totrinnskontroll SET endret_tid = ? WHERE id = ?",
            totrinnskontroll.sporbar.opprettetTid,
            totrinnskontroll.id,
        )
        return totrinnskontrollRepository.findByIdOrThrow(totrinnskontroll.id)
    }
}
