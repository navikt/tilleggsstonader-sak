package no.nav.tilleggsstonader.sak.behandling.barn

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime

class BarnServiceTest : IntegrationTest() {
    @Autowired
    lateinit var barnService: BarnService

    val dummySaksbehandler = "saksbehandler1"
    val fagsak = fagsak()
    val behandling = behandling(fagsak = fagsak)

    @BeforeEach
    fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagre(behandling)
    }

    @Nested
    inner class KopierMangelendeBarnFraForrgieBehandling {
        @Test
        fun `kan kopiere maglende barn fra forrgie behandling`() {
            testWithBrukerContext(dummySaksbehandler) {
                val barn = dummyBarn(behandling.id, "barn1")
                barnService.opprettBarn(listOf(barn))
                testoppsettService.ferdigstillBehandling(behandling)

                val nyBehandling =
                    behandling(
                        fagsak = fagsak,
                        status = BehandlingStatus.UTREDES,
                        resultat = BehandlingResultat.INNVILGET,
                        vedtakstidspunkt = LocalDateTime.now().plusDays(1),
                    )
                testoppsettService.lagre(nyBehandling)
                val barnNyBehandling = dummyBarn(nyBehandling.id, "barnNyBehandling")
                barnService.opprettBarn(listOf(barnNyBehandling))

                barnService.kopierManglendeBarnFraForrigeBehandling(behandling.id, nyBehandling)

                val barnEtterTaAvVent = barnService.finnBarnPåBehandling(nyBehandling.id)
                assertThat(barnEtterTaAvVent).hasSize(2)
                assertThat(barnEtterTaAvVent.map { it.ident }).containsExactlyInAnyOrder(
                    "barn1",
                    "barnNyBehandling",
                )
            }
        }

        @Test
        fun `to barn på forrgie behandling`() {
            testWithBrukerContext(dummySaksbehandler) {
                val barn1 = dummyBarn(behandling.id, "barn1")
                val barn2 = dummyBarn(behandling.id, "barn2")
                barnService.opprettBarn(listOf(barn1, barn2))
                testoppsettService.ferdigstillBehandling(behandling)

                val nyBehandling =
                    behandling(
                        fagsak = fagsak,
                        status = BehandlingStatus.UTREDES,
                        resultat = BehandlingResultat.INNVILGET,
                        vedtakstidspunkt = LocalDateTime.now().plusDays(1),
                    )
                testoppsettService.lagre(nyBehandling)
                val barnNyBehandling = dummyBarn(nyBehandling.id, "barnNyBehandling")
                barnService.opprettBarn(listOf(barnNyBehandling))

                barnService.kopierManglendeBarnFraForrigeBehandling(behandling.id, nyBehandling)

                val barnEtterTaAvVent = barnService.finnBarnPåBehandling(nyBehandling.id)
                assertThat(barnEtterTaAvVent).hasSize(3)
                assertThat(barnEtterTaAvVent.map { it.ident }).containsExactlyInAnyOrder(
                    "barn1",
                    "barn2",
                    "barnNyBehandling",
                )
            }
        }

        @Test
        fun `tre barn på forrgie behandling`() {
            testWithBrukerContext(dummySaksbehandler) {
                val barn1 = dummyBarn(behandling.id, "barn1")
                val barn2 = dummyBarn(behandling.id, "barn2")
                val barn3 = dummyBarn(behandling.id, "barn3")
                barnService.opprettBarn(listOf(barn1, barn2, barn3))
                testoppsettService.ferdigstillBehandling(behandling)

                val nyBehandling =
                    behandling(
                        fagsak = fagsak,
                        status = BehandlingStatus.UTREDES,
                        resultat = BehandlingResultat.INNVILGET,
                        vedtakstidspunkt = LocalDateTime.now().plusDays(1),
                    )
                testoppsettService.lagre(nyBehandling)
                val barnNyBehandling = dummyBarn(nyBehandling.id, "barnNyBehandling")
                barnService.opprettBarn(listOf(barnNyBehandling))

                barnService.kopierManglendeBarnFraForrigeBehandling(behandling.id, nyBehandling)

                val barnEtterTaAvVent = barnService.finnBarnPåBehandling(nyBehandling.id)
                assertThat(barnEtterTaAvVent).hasSize(4)
                assertThat(barnEtterTaAvVent.map { it.ident }).containsExactlyInAnyOrder(
                    "barn1",
                    "barn2",
                    "barn3",
                    "barnNyBehandling",
                )
            }
        }

        @Test
        fun `ingen barn på forrgie behandling`() {
            testWithBrukerContext(dummySaksbehandler) {
                testoppsettService.ferdigstillBehandling(behandling)

                val nyBehandling =
                    behandling(
                        fagsak = fagsak,
                        status = BehandlingStatus.UTREDES,
                        resultat = BehandlingResultat.INNVILGET,
                        vedtakstidspunkt = LocalDateTime.now().plusDays(1),
                    )
                testoppsettService.lagre(nyBehandling)
                val barnNyBehandling = dummyBarn(nyBehandling.id, "barnNyBehandling")
                barnService.opprettBarn(listOf(barnNyBehandling))

                barnService.kopierManglendeBarnFraForrigeBehandling(behandling.id, nyBehandling)

                val barnEtterTaAvVent = barnService.finnBarnPåBehandling(nyBehandling.id)
                assertThat(barnEtterTaAvVent).hasSize(1)
                assertThat(barnEtterTaAvVent.map { it.ident }).containsExactlyInAnyOrder(
                    "barnNyBehandling",
                )
            }
        }

        @Test
        fun `ingen barn på ny behandling`() {
            testWithBrukerContext(dummySaksbehandler) {
                val barn = dummyBarn(behandling.id, "barn1")
                barnService.opprettBarn(listOf(barn))
                testoppsettService.ferdigstillBehandling(behandling)

                val nyBehandling =
                    behandling(
                        fagsak = fagsak,
                        status = BehandlingStatus.UTREDES,
                        resultat = BehandlingResultat.INNVILGET,
                        vedtakstidspunkt = LocalDateTime.now().plusDays(1),
                    )
                testoppsettService.lagre(nyBehandling)

                barnService.kopierManglendeBarnFraForrigeBehandling(behandling.id, nyBehandling)

                val barnEtterTaAvVent = barnService.finnBarnPåBehandling(nyBehandling.id)
                assertThat(barnEtterTaAvVent).hasSize(1)
                assertThat(barnEtterTaAvVent.map { it.ident }).containsExactlyInAnyOrder(
                    "barn1",
                )
            }
        }

        @Test
        fun `to barn på ny behandling`() {
            testWithBrukerContext(dummySaksbehandler) {
                val barn = dummyBarn(behandling.id, "barn1")
                barnService.opprettBarn(listOf(barn))
                testoppsettService.ferdigstillBehandling(behandling)

                val nyBehandling =
                    behandling(
                        fagsak = fagsak,
                        status = BehandlingStatus.UTREDES,
                        resultat = BehandlingResultat.INNVILGET,
                        vedtakstidspunkt = LocalDateTime.now().plusDays(1),
                    )
                testoppsettService.lagre(nyBehandling)
                val barnNyBehandling1 = dummyBarn(nyBehandling.id, "barnNyBehandling1")
                val barnNyBehandling2 = dummyBarn(nyBehandling.id, "barnNyBehandling2")
                barnService.opprettBarn(listOf(barnNyBehandling1, barnNyBehandling2))

                barnService.kopierManglendeBarnFraForrigeBehandling(behandling.id, nyBehandling)

                val barnEtterTaAvVent = barnService.finnBarnPåBehandling(nyBehandling.id)
                assertThat(barnEtterTaAvVent).hasSize(3)
                assertThat(barnEtterTaAvVent.map { it.ident }).containsExactlyInAnyOrder(
                    "barn1",
                    "barnNyBehandling1",
                    "barnNyBehandling2",
                )
            }
        }

        @Test
        fun `duplikerer ikke barn`() {
            testWithBrukerContext(dummySaksbehandler) {
                val barn = dummyBarn(behandling.id, "barn1")
                barnService.opprettBarn(listOf(barn))
                testoppsettService.ferdigstillBehandling(behandling)

                val nyBehandling =
                    behandling(
                        fagsak = fagsak,
                        status = BehandlingStatus.UTREDES,
                        resultat = BehandlingResultat.INNVILGET,
                        vedtakstidspunkt = LocalDateTime.now().plusDays(1),
                    )
                testoppsettService.lagre(nyBehandling)
                val barn1Kopi = dummyBarn(nyBehandling.id, "barn1")
                barnService.opprettBarn(listOf(barn1Kopi))

                barnService.kopierManglendeBarnFraForrigeBehandling(behandling.id, nyBehandling)

                val barnEtterTaAvVent = barnService.finnBarnPåBehandling(nyBehandling.id)
                assertThat(barnEtterTaAvVent).hasSize(1)
                assertThat(barnEtterTaAvVent.map { it.ident }).containsExactlyInAnyOrder(
                    "barn1",
                )
            }
        }
    }

    /**
     * Utility method for creating a dummy test barn (child)
     */
    private fun dummyBarn(
        behandlingId: BehandlingId,
        ident: String,
    ) = BehandlingBarn(
        behandlingId = behandlingId,
        ident = ident,
    )
}
