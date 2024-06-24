package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

class OpprettRevurderingBehandlingServiceTest : IntegrationTest() {

    @Autowired
    lateinit var service: OpprettRevurderingBehandlingService

    @Autowired
    lateinit var barnService: BarnService

    @Autowired
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    @Autowired
    lateinit var stønadsperiodeRepository: StønadsperiodeRepository

    @BeforeEach
    fun setUp() {
        BrukerContextUtil.mockBrukerContext()
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        BrukerContextUtil.clearBrukerContext()
    }

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

    @Test
    fun `skal gjenbruke informasjon fra forrige behandling`() {
        val fom = LocalDate.of(2024, 1, 1)
        val tom = LocalDate.of(2024, 1, 31)

        val behandling1 = testoppsettService.opprettBehandlingMedFagsak(
            behandling(
                status = BehandlingStatus.FERDIGSTILT,
                resultat = BehandlingResultat.INNVILGET,
            ),
            opprettGrunnlagsdata = false,
        )

        vilkårperiodeRepository.insertAll(
            listOf(
                målgruppe(behandlingId = behandling1.id, fom = fom, tom = tom),
                aktivitet(behandlingId = behandling1.id, fom = fom, tom = tom),
            ),
        )
        stønadsperiodeRepository.insert(stønadsperiode(behandlingId = behandling1.id, fom = fom, tom = tom))

        val revurderingId = service.opprettBehandling(opprettBehandlingDto(fagsakId = behandling1.fagsakId))

        assertThat(vilkårperiodeRepository.findByBehandlingId(revurderingId)).hasSize(2)
        assertThat(stønadsperiodeRepository.findAllByBehandlingId(revurderingId)).hasSize(1)
    }

    private fun opprettBehandlingDto(
        fagsakId: UUID,
    ) = OpprettBehandlingDto(
        fagsakId = fagsakId,
    )
}
