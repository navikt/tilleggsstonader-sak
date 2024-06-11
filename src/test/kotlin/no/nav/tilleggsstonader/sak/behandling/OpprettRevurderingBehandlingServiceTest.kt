package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

class OpprettRevurderingBehandlingServiceTest : IntegrationTest() {

    @Autowired
    lateinit var service: OpprettRevurderingBehandlingService

    @Autowired
    lateinit var barnService: BarnService

    @Nested
    inner class OpprettBehandling {

        @Test
        fun `skal feile hvis forrige behandlingen ikke er ferdigstilt`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(), opprettGrunnlagsdata = false)

            assertThatThrownBy {
                service.opprettBehandling(opprettBehandlingDto(fagsakId = behandling.fagsakId))
            }.hasMessage("Det finnes en behandling på fagsaken som ikke er ferdigstilt")
        }

        @Test
        fun `ny behandling skal peke til forrige innvilgede behandlingen fordi iverksetting skal peke til forrige iverksatte behandlingen`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(
                behandling(
                    status = BehandlingStatus.FERDIGSTILT,
                    resultat = BehandlingResultat.INNVILGET,
                ),
                opprettGrunnlagsdata = false,
            )

            val nyBehandlingId =
                service.opprettBehandling(opprettBehandlingDto(fagsakId = behandling.fagsakId))

            val nyBehandling = testoppsettService.hentBehandling(nyBehandlingId)
            assertThat(nyBehandling.forrigeBehandlingId).isEqualTo(behandling.id)
        }

        @Test
        fun `ny behandling skal ikke peke til forrige henlagde behandlingen fordi iverksetting skal peke til forrige iverksatte behandlingen`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(
                behandling(
                    status = BehandlingStatus.FERDIGSTILT,
                    resultat = BehandlingResultat.HENLAGT,
                ),
                opprettGrunnlagsdata = false,
            )

            val nyBehandlingId =
                service.opprettBehandling(opprettBehandlingDto(fagsakId = behandling.fagsakId))

            val nyBehandling = testoppsettService.hentBehandling(nyBehandlingId)
            assertThat(nyBehandling.forrigeBehandlingId).isNull()
        }

        @Test
        fun `ny behandling skal ikke peke til forrige avslåtte behandlingen fordi iverksetting skal peke til forrige iverksatte behandlingen`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(
                behandling(
                    status = BehandlingStatus.FERDIGSTILT,
                    resultat = BehandlingResultat.AVSLÅTT,
                ),
                opprettGrunnlagsdata = false,
            )

            val nyBehandlingId =
                service.opprettBehandling(opprettBehandlingDto(fagsakId = behandling.fagsakId))

            val nyBehandling = testoppsettService.hentBehandling(nyBehandlingId)
            assertThat(nyBehandling.forrigeBehandlingId).isNull()
        }
    }

    @Nested
    inner class GjenbrukBarn {

        @Test
        fun `skal gjenbruke barn fra forrige behandlingen`() {
            val barnIdent = "barn1"
            val behandling1 = testoppsettService.opprettBehandlingMedFagsak(
                behandling(
                    status = BehandlingStatus.FERDIGSTILT,
                    resultat = BehandlingResultat.INNVILGET,
                ),
                opprettGrunnlagsdata = false,
            )
            barnService.opprettBarn(listOf(behandlingBarn(behandlingId = behandling1.id, personIdent = barnIdent)))

            val nyBehandlingId =
                service.opprettBehandling(opprettBehandlingDto(fagsakId = behandling1.fagsakId))

            with(barnService.finnBarnPåBehandling(behandling1.id)) {
                assertThat(this).hasSize(1)
            }

            with(barnService.finnBarnPåBehandling(nyBehandlingId)) {
                assertThat(this).hasSize(1)
                assertThat(this.single().ident).isEqualTo(barnIdent)
            }
        }

        // TODO gjenbruke barn fra henlagt/avslått behandling?
    }

    private fun opprettBehandlingDto(
        fagsakId: UUID,
    ) = OpprettBehandlingDto(
        fagsakId = fagsakId,
    )
}
