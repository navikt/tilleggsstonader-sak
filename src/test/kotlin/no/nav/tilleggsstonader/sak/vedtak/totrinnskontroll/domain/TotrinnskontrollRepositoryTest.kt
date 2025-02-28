package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.behandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class TotrinnskontrollRepositoryTest : IntegrationTest() {
    @Autowired
    lateinit var totrinnskontrollRepository: TotrinnskontrollRepository

    val behandling = behandling()

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettBehandlingMedFagsak(behandling)
    }

    @Test
    fun `skal kunne lagre og hente totrinnskontroll`() {
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
    inner class FindTopByBehandlingIdOrderBySporbarEndretEndretTidDesc {
        @Test
        fun `skal finne siste totrinnskontroll basert på endret tidspunkt`() {
            val totrinnskontrollFirst =
                lagreOgOppdaterEndretTidTilOpprettetTid(
                    Totrinnskontroll(
                        behandlingId = behandling.id,
                        saksbehandler = "1",
                        status = TotrinnInternStatus.GODKJENT,
                        sporbar = Sporbar(opprettetTid = SporbarUtils.now().plusDays(1)),
                    ),
                )
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

    @Nested
    inner class FindTopByBehandlingIdAndStatusNotOrderBySporbarEndretEndretTidDesc {
        @Test
        fun `skal ikke finne siste som er angret`() {
            lagreOgOppdaterEndretTidTilOpprettetTid(
                Totrinnskontroll(
                    behandlingId = behandling.id,
                    saksbehandler = "1",
                    status = TotrinnInternStatus.ANGRET,
                    sporbar = Sporbar(opprettetTid = SporbarUtils.now().plusDays(1)),
                ),
            )
            assertThat(finnSisteSomIkkeErAngret()).isNull()

            val totrinnskontrollSecond =
                lagreOgOppdaterEndretTidTilOpprettetTid(
                    Totrinnskontroll(
                        behandlingId = behandling.id,
                        saksbehandler = "2",
                        status = TotrinnInternStatus.GODKJENT,
                        sporbar = Sporbar(opprettetTid = SporbarUtils.now().plusDays(2)),
                    ),
                )
            assertThat(finnSisteSomIkkeErAngret()).isEqualTo(totrinnskontrollSecond)
        }

        private fun finnSisteSomIkkeErAngret() =
            totrinnskontrollRepository.findTopByBehandlingIdAndStatusNotOrderBySporbarEndretEndretTidDesc(
                behandling.id,
                TotrinnInternStatus.ANGRET,
            )
    }

    private fun lagreOgOppdaterEndretTidTilOpprettetTid(totrinnskontroll: Totrinnskontroll): Totrinnskontroll {
        totrinnskontrollRepository.insert(totrinnskontroll)
        jdbcTemplate.update(
            "UPDATE totrinnskontroll SET endret_tid = :endretTid WHERE id = :id",
            mapOf(
                "endretTid" to totrinnskontroll.sporbar.opprettetTid,
                "id" to totrinnskontroll.id,
            ),
        )
        return totrinnskontrollRepository.findByIdOrThrow(totrinnskontroll.id)
    }
}
