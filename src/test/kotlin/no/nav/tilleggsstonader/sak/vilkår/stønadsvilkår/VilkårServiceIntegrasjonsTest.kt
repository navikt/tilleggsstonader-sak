package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.tilleggsstonader.libs.test.fnr.FnrGenerator
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.barn.NyttBarnId
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.PdlClientConfig
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vilkår.VilkårTestUtils
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Opphavsvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpprettVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil.oppfylteDelvilkårPassBarnDto
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.YearMonth

internal class VilkårServiceIntegrasjonsTest : IntegrationTest() {
    @Autowired
    lateinit var vilkårRepository: VilkårRepository

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var vilkårService: VilkårService

    @Autowired
    lateinit var barnRepository: BarnRepository

    @Autowired
    lateinit var barnService: BarnService

    @Nested
    inner class KopierVilkår {
        val fagsak = fagsak()
        val førstegangsbehandling = behandling(fagsak, status = BehandlingStatus.FERDIGSTILT)
        val revurdering = behandling(fagsak)
        val barn1Ident = FnrGenerator.generer(LocalDate.now().minusYears(3))
        val barnFørsteBehandling =
            listOf(behandlingBarn(behandlingId = førstegangsbehandling.id, personIdent = barn1Ident))
        var barnIdMap: Map<BarnId, NyttBarnId> = emptyMap()

        @BeforeEach
        fun setUp() {
            testoppsettService.lagreFagsak(fagsak)
            testoppsettService.lagre(førstegangsbehandling)
            testoppsettService.lagre(revurdering)
            barnRepository.insertAll(barnFørsteBehandling)

            barnIdMap = barnService.gjenbrukBarn(førstegangsbehandling.id, revurdering.id)
        }

        @Test
        internal fun `kopierVilkårsettTilNyBehandling - skal kopiere vilkår til ny behandling`() {
            val vilkårForBehandling = lagreVilkårsvurderinger(førstegangsbehandling, barnFørsteBehandling).first()

            vilkårService.kopierVilkårsettTilNyBehandling(
                forrigeBehandlingId = førstegangsbehandling.id,
                nyBehandling = revurdering,
                barnIdMap = barnIdMap,
            )

            val vilkårForRevurdering = vilkårRepository.findByBehandlingId(revurdering.id).first()

            assertThat(vilkårForBehandling.id).isNotEqualTo(vilkårForRevurdering.id)
            assertThat(vilkårForBehandling.behandlingId).isNotEqualTo(vilkårForRevurdering.behandlingId)
            assertThat(vilkårForBehandling.sporbar.opprettetTid).isNotEqualTo(vilkårForRevurdering.sporbar.opprettetTid)
            assertThat(vilkårForBehandling.sporbar.endret.endretTid).isNotEqualTo(vilkårForRevurdering.sporbar.endret.endretTid)
            assertThat(vilkårForBehandling.barnId).isNotEqualTo(vilkårForRevurdering.barnId)
            assertThat(vilkårForBehandling.barnId).isEqualTo(barnFørsteBehandling.first().id)
            assertThat(vilkårForBehandling.opphavsvilkår).isNull()
            assertThat(vilkårForRevurdering.barnId).isEqualTo(barnIdMap[barnFørsteBehandling.first().id])
            assertThat(vilkårForRevurdering.opphavsvilkår)
                .isEqualTo(Opphavsvilkår(førstegangsbehandling.id, vilkårForBehandling.sporbar.endret.endretTid))

            assertVilkårErGjenbrukt(vilkårForBehandling, vilkårForRevurdering)
        }

        @Test
        internal fun `kopierVilkårsettTilNyBehandling - skal ikke kopiere vilkår som mangler periode`() {
            lagreVilkårsvurderinger(førstegangsbehandling, barnFørsteBehandling, fom = null, tom = null).first()

            vilkårService.kopierVilkårsettTilNyBehandling(
                forrigeBehandlingId = førstegangsbehandling.id,
                nyBehandling = revurdering,
                barnIdMap = barnIdMap,
            )

            val vilkårForRevurdering = vilkårRepository.findByBehandlingId(revurdering.id)

            assertThat(vilkårForRevurdering).isEmpty()
        }

        @Test
        internal fun `kopierVilkårsettTilNyBehandling - skal ikke kopiere vilkår som er slettet`() {
            lagreVilkårsvurderinger(førstegangsbehandling, barnFørsteBehandling, status = VilkårStatus.SLETTET).first()

            vilkårService.kopierVilkårsettTilNyBehandling(
                forrigeBehandlingId = førstegangsbehandling.id,
                nyBehandling = revurdering,
                barnIdMap = barnIdMap,
            )

            val vilkårForRevurdering = vilkårRepository.findByBehandlingId(revurdering.id)

            assertThat(vilkårForRevurdering).isEmpty()
        }
    }

    @Nested
    inner class OpprettVilkår {
        val behandling = behandling(status = BehandlingStatus.UTREDES, steg = StegType.VILKÅR)
        val barn = behandlingBarn(behandlingId = behandling.id, personIdent = PdlClientConfig.BARN_FNR)

        val opprettOppfyltDelvilkår =
            OpprettVilkårDto(
                vilkårType = VilkårType.PASS_BARN,
                barnId = barn.id,
                behandlingId = behandling.id,
                delvilkårsett = oppfylteDelvilkårPassBarnDto(),
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 31),
                utgift = 1,
            )

        @BeforeEach
        fun setUp() {
            testoppsettService.opprettBehandlingMedFagsak(behandling, opprettGrunnlagsdata = false)
            barnRepository.insert(barn)
            testoppsettService.opprettGrunnlagsdata(behandling.id)
        }

        @Test
        fun `skal opprette vilkår på behandling`() {
            vilkårService.opprettNyttVilkår(opprettOppfyltDelvilkår)

            val vilkårFraDb = vilkårRepository.findByBehandlingId(behandling.id).single()
            assertThat(vilkårFraDb.behandlingId).isEqualTo(behandling.id)
            assertThat(vilkårFraDb.type).isEqualTo(VilkårType.PASS_BARN)
            assertThat(vilkårFraDb.fom).isEqualTo(LocalDate.of(2024, 1, 1))
            assertThat(vilkårFraDb.tom).isEqualTo(LocalDate.of(2024, 1, 31))
            assertThat(vilkårFraDb.utgift).isEqualTo(1)
            assertThat(vilkårFraDb.barnId).isEqualTo(barn.id)
            assertThat(vilkårFraDb.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
            assertThat(vilkårFraDb.status).isEqualTo(VilkårStatus.NY)
            assertThat(vilkårFraDb.opphavsvilkår).isNull()
            assertThat(vilkårFraDb.delvilkårsett.map { it.hovedregel })
                .containsExactlyInAnyOrderElementsOf(opprettOppfyltDelvilkår.delvilkårsett.map { it.hovedregel() })
        }

        @Test
        fun `kan ikke opprette vilkår på behandling som er ferdigstilt`() {
            behandlingRepository.update(behandling.copy(status = BehandlingStatus.FERDIGSTILT))

            assertThatThrownBy {
                vilkårService.opprettNyttVilkår(opprettOppfyltDelvilkår)
            }.hasMessageContaining("Kan ikke gjøre endringer på denne behandlingen fordi den er ferdigstilt.")
        }

        @Test
        fun `kan ikke opprette vilkår på behandling som er i feil steg`() {
            behandlingRepository.update(behandling.copy(steg = StegType.INNGANGSVILKÅR))

            assertThatThrownBy {
                vilkårService.opprettNyttVilkår(opprettOppfyltDelvilkår)
            }.hasMessageContaining("Kan ikke oppdatere vilkår når behandling er på steg=INNGANGSVILKÅR")
        }

        @Test
        fun `kan ikke opprette vilkår på barn som ikke finnes på behandling`() {
            assertThatThrownBy {
                vilkårService.opprettNyttVilkår(opprettOppfyltDelvilkår.copy(barnId = BarnId.random()))
            }.hasMessageContaining("Finner ikke barn på behandling")
        }

        @Test
        fun `kan ikke opprette vilkårtype som ikke finnes på stønadstype`() {
            assertThatThrownBy {
                vilkårService.opprettNyttVilkår(opprettOppfyltDelvilkår.copy(vilkårType = VilkårType.EKSEMPEL))
            }.hasMessageContaining("Vilkårtype=EKSEMPEL eksisterer ikke for stønadstype=BARNETILSYN")
        }
    }

    private fun lagreVilkårsvurderinger(
        behandling: Behandling,
        barn: List<BehandlingBarn>,
        fom: LocalDate? = YearMonth.now().atDay(1),
        tom: LocalDate? = YearMonth.now().atEndOfMonth(),
        status: VilkårStatus = VilkårStatus.NY,
    ): List<Vilkår> = vilkårRepository.insertAll(VilkårTestUtils.opprettVilkårsvurderinger(behandling, barn, fom, tom, status))

    private fun assertVilkårErGjenbrukt(
        vilkårForBehandling: Vilkår,
        vilkårForRevurdering: Vilkår,
    ) {
        assertThat(vilkårForBehandling)
            .usingRecursiveComparison()
            .ignoringFields("id", "sporbar", "behandlingId", "barnId", "opphavsvilkår", "status")
            .isEqualTo(vilkårForRevurdering)

        assertThat(vilkårForRevurdering.opphavsvilkår?.behandlingId).isEqualTo(vilkårForBehandling.behandlingId)
        assertThat(vilkårForRevurdering.status).isEqualTo(VilkårStatus.UENDRET)
    }
}
