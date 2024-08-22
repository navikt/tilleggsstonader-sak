package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.OpprettBehandlingDto
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.PdlClientConfig
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
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

    @Autowired
    lateinit var vilkårRepository: VilkårRepository

    @BeforeEach
    fun setUp() {
        BrukerContextUtil.mockBrukerContext()
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        BrukerContextUtil.clearBrukerContext()
        PdlClientConfig.opprettPdlSøker()
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

            vilkårRepository.insert(vilkår(behandlingId = behandling.id))

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

            vilkårRepository.insert(vilkår(behandlingId = behandling.id))

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

            vilkårRepository.insert(vilkår(behandlingId = behandling.id))

            val nyBehandlingId =
                service.opprettBehandling(opprettBehandlingDto(fagsakId = behandling.fagsakId))

            val nyBehandling = testoppsettService.hentBehandling(nyBehandlingId)
            assertThat(nyBehandling.forrigeBehandlingId).isNull()
        }
    }

    @Nested
    inner class GjenbrukDataFraForrigeBehandling {
        var tidligereBehandling: Behandling? = null
        val barnIdent = "barn1"
        val fom = LocalDate.of(2024, 1, 1)
        val tom = LocalDate.of(2024, 1, 31)

        @BeforeEach
        fun setUp() {
            tidligereBehandling = testoppsettService.opprettBehandlingMedFagsak(
                behandling(
                    status = BehandlingStatus.FERDIGSTILT,
                    resultat = BehandlingResultat.INNVILGET,
                ),
                opprettGrunnlagsdata = false,
            )

            val barn = barnService.opprettBarn(
                listOf(
                    behandlingBarn(
                        behandlingId = tidligereBehandling!!.id,
                        personIdent = barnIdent,
                    ),
                ),
            )

            vilkårperiodeRepository.insertAll(
                listOf(
                    målgruppe(behandlingId = tidligereBehandling!!.id, fom = fom, tom = tom),
                    aktivitet(behandlingId = tidligereBehandling!!.id, fom = fom, tom = tom),
                ),
            )

            stønadsperiodeRepository.insert(
                stønadsperiode(
                    behandlingId = tidligereBehandling!!.id,
                    fom = fom,
                    tom = tom,
                ),
            )

            vilkårRepository.insertAll(
                barn.map {
                    vilkår(
                        behandlingId = tidligereBehandling!!.id,
                        barnId = it.id,
                        type = VilkårType.PASS_BARN,
                    )
                },
            )
        }

        @Test
        fun `skal gjenbruke barn fra forrige behandlingen`() {
            val nyBehandlingId =
                service.opprettBehandling(opprettBehandlingDto(fagsakId = tidligereBehandling!!.fagsakId))

            with(barnService.finnBarnPåBehandling(tidligereBehandling!!.id)) {
                assertThat(this).hasSize(1)
            }

            with(barnService.finnBarnPåBehandling(nyBehandlingId)) {
                assertThat(this).hasSize(1)
                assertThat(this.single().ident).isEqualTo(barnIdent)
            }
        }

        // TODO gjenbruke barn fra henlagt/avslått behandling?

        @Test
        fun `skal gjenbruke informasjon fra forrige behandling`() {
            val revurderingId =
                service.opprettBehandling(opprettBehandlingDto(fagsakId = tidligereBehandling!!.fagsakId))

            assertThat(vilkårperiodeRepository.findByBehandlingId(revurderingId)).hasSize(2)
            assertThat(stønadsperiodeRepository.findAllByBehandlingId(revurderingId)).hasSize(1)
            assertThat(vilkårRepository.findByBehandlingId(revurderingId)).hasSize(1)
        }
    }

    @Nested
    inner class HåndteringAvBarn {
        val behandling = behandling(
            status = BehandlingStatus.FERDIGSTILT,
            resultat = BehandlingResultat.INNVILGET,
        )

        @Test
        fun `hentBarnTilRevurdering - skal markere barn som finnes med på forrige behandling`() {
            testoppsettService.opprettBehandlingMedFagsak(behandling, opprettGrunnlagsdata = false)
            val eksisterendeBarn = behandlingBarn(behandlingId = behandling.id, personIdent = PdlClientConfig.barnFnr)
            barnService.opprettBarn(listOf(eksisterendeBarn))

            val barnTilRevurdering = service.hentBarnTilRevurdering(behandling.fagsakId)

            assertThat(barnTilRevurdering.barn).hasSize(2)
            assertThat(barnTilRevurdering.barn.single { it.ident == eksisterendeBarn.ident }.finnesPåForrigeBehandling)
                .isTrue()

            assertThat(barnTilRevurdering.barn.single { it.ident == PdlClientConfig.barn2Fnr }.finnesPåForrigeBehandling)
                .isFalse()
        }
    }

    private fun opprettBehandlingDto(
        fagsakId: UUID,
        årsak: BehandlingÅrsak = BehandlingÅrsak.NYE_OPPLYSNINGER,
    ) = OpprettBehandlingDto(
        fagsakId = fagsakId,
        årsak = årsak,
    )
}
