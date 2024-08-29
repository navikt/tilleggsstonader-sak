package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.test.fnr.FnrGenerator
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.GjennbrukDataRevurderingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.PdlClientConfig
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Opphavsvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpprettVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.EksempelRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil.oppfylteDelvilkårPassBarnDto
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.util.UUID

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

    @Autowired
    lateinit var gjennbrukDataRevurderingService: GjennbrukDataRevurderingService

    @Test
    internal fun `kopierVilkårsettTilNyBehandling - skal kopiere vilkår til ny behandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        val revurdering = testoppsettService.lagre(behandling(fagsak))
        val barn1Ident = FnrGenerator.generer(LocalDate.now().minusYears(3))
        val barnFørsteBehandling = barnRepository.insertAll(listOf(barn1Ident).tilBehandlingBarn(behandling))
        val barnIdMap = barnService.gjenbrukBarn(behandling.id, revurdering.id)

        val vilkårForBehandling = opprettVilkårsvurderinger(behandling, barnFørsteBehandling).first()

        vilkårService.kopierVilkårsettTilNyBehandling(
            forrigeBehandlingId = behandling.id,
            nyBehandling = revurdering,
            barnIdMap = barnIdMap,
            stønadstype = Stønadstype.BARNETILSYN,
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
            .isEqualTo(Opphavsvilkår(behandling.id, vilkårForBehandling.sporbar.endret.endretTid))

        assertVilkårErGjenbrukt(vilkårForBehandling, vilkårForRevurdering)
    }

    @Disabled // TODO
    @Test
    internal fun `oppdaterGrunnlagsdataOgHentEllerOpprettVurderinger - skal kaste feil dersom behandlingen er låst for videre behandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        assertThat(catchThrowable { vilkårService.oppdaterGrunnlagsdataOgHentEllerOpprettVurderinger(behandling.id) })
            .hasMessage("Kan ikke laste inn nye grunnlagsdata for behandling med status ${behandling.status}")
    }

    @Test
    internal fun `kopierVilkårsettTilNyBehandling - skal kaste feil hvis det ikke finnes noen vurderinger`() {
        val tidligereBehandlingId = UUID.randomUUID()
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val revurdering = testoppsettService.lagre(behandling(fagsak))

        assertThat(
            catchThrowable {
                vilkårService.kopierVilkårsettTilNyBehandling(
                    tidligereBehandlingId,
                    revurdering,
                    emptyMap(),
                    Stønadstype.BARNETILSYN,
                )
            },
        )
            .hasMessage("Tidligere behandling=$tidligereBehandlingId har ikke noen vilkår")
    }

    @Nested
    inner class OpprettelseAvNyeBarnVidHentingAvVilkår {

        val barn1Ident = FnrGenerator.generer(LocalDate.now().minusYears(3))
        val barn2Ident = FnrGenerator.generer(LocalDate.now().minusYears(7))

        val fagsak = fagsak()

        val førstegangsbehandling = behandling(fagsak, status = BehandlingStatus.FERDIGSTILT)
        val revurdering = behandling(fagsak)

        @BeforeEach
        fun setUp() {
            testoppsettService.lagreFagsak(fagsak)
        }

        /**
         * Søknad 1: Barn1
         *
         * Søknad 2: Barn2
         */
        @Test
        internal fun `hentEllerOpprettVilkår skal opprette vilkår for nytt barn og kopiere vilkår for eksisterende barn på fagsak`() {
            val barnPåFørsteBehandling = listOf(barn1Ident).tilBehandlingBarn(førstegangsbehandling)

            opprettFørstegangsbehandling(barnPåFørsteBehandling)

            // barn 2 må opprettes manuelt fordi den ikke opprettes i gjenbruk
            opprettRevurdering(listOf(barn2Ident).tilBehandlingBarn(revurdering))

            opprettVilkårForNyeBarnVidHentingAvVilkår()

            assertHarBeholdtBarn1OgOpprettetVilkårForNyttBarn()
        }

        /**
         * Søknad 1: Barn1
         *
         * Søknad 2: Barn 1 og Barn2
         */
        @Test
        internal fun `hentEllerOpprettVilkår skal opprette vilkår for nytt barn og gjennbruke eksisterende vilkår for barn`() {
            val barnFørsteBehandling = listOf(barn1Ident).tilBehandlingBarn(førstegangsbehandling)

            opprettFørstegangsbehandling(barnFørsteBehandling)

            // barn 2 må opprettes manuelt fordi den ikke opprettes i gjenbruk
            opprettRevurdering(listOf(barn2Ident).tilBehandlingBarn(revurdering))

            opprettVilkårForNyeBarnVidHentingAvVilkår()

            assertHarBeholdtBarn1OgOpprettetVilkårForNyttBarn()
        }

        /**
         * Søknad 1: Barn1
         *
         * Søknad 2: Barn 1 og Barn2
         */
        @Test
        fun `skal ikke opprette barn andre gangen man kaller på hent vilkår`() {
            val barnFørsteBehandling = listOf(barn1Ident).tilBehandlingBarn(førstegangsbehandling)

            opprettFørstegangsbehandling(barnFørsteBehandling)

            // barn 2 må opprettes manuelt fordi den ikke opprettes i gjenbruk
            opprettRevurdering(listOf(barn2Ident).tilBehandlingBarn(revurdering))

            opprettVilkårForNyeBarnVidHentingAvVilkår()
            val vilkårRevurdering = vilkårRepository.findByBehandlingId(revurdering.id)

            opprettVilkårForNyeBarnVidHentingAvVilkår()

            assertThat(vilkårRepository.findByBehandlingId(revurdering.id))
                .`as`("Vilkåren er ikke opprettet på nytt")
                .containsAnyElementsOf(vilkårRevurdering)
        }

        private fun assertHarBeholdtBarn1OgOpprettetVilkårForNyttBarn() {
            val barnRevurdering = barnService.finnBarnPåBehandling(revurdering.id)
            val revurderingBarn1 = barnRevurdering.single { it.ident == barn1Ident }
            val revurderingBarn2 = barnRevurdering.single { it.ident == barn2Ident }

            val vilkårFørstegangsbehandling = vilkårRepository.findByBehandlingId(førstegangsbehandling.id).single()
            val vilkårRevurdering = vilkårRepository.findByBehandlingId(revurdering.id)
            val vilkårRevurderingBarn1 = vilkårRevurdering.single { it.barnId == revurderingBarn1.id }
            val vilkårRevurderingBarn2 = vilkårRevurdering.single { it.barnId == revurderingBarn2.id }

            assertThat(vilkårRevurdering).hasSize(2)
            assertVilkårErGjenbrukt(vilkårFørstegangsbehandling, vilkårRevurderingBarn1)

            with(vilkårRevurderingBarn2) {
                assertThat(opphavsvilkår).isNull()
                assertThat(barnId).isEqualTo(revurderingBarn2.id)
            }
        }

        private fun opprettFørstegangsbehandling(barn: List<BehandlingBarn>) {
            testoppsettService.lagre(førstegangsbehandling)
            barnRepository.insertAll(barn)
            opprettVilkårsvurderinger(førstegangsbehandling, barn)
        }

        private fun opprettRevurdering(barn: List<BehandlingBarn>) {
            testoppsettService.lagre(revurdering)
            gjennbrukDataRevurderingService.gjenbrukData(revurdering, førstegangsbehandling.id)
            // barn som ikke var med i første behandling må opprettes manuelt
            barnService.opprettBarn(barn)
        }

        private fun opprettVilkårForNyeBarnVidHentingAvVilkår() {
            val barnRevurdering = barnService.finnBarnPåBehandling(revurdering.id)
            // Oppretter vilkår for nye barn
            vilkårService.hentEllerOpprettVilkår(revurdering.id, HovedregelMetadata(barnRevurdering, revurdering))
        }
    }

    @Nested
    inner class OpprettVilkår {

        val behandling = behandling(status = BehandlingStatus.UTREDES, steg = StegType.VILKÅR)
        val barn = behandlingBarn(behandlingId = behandling.id, personIdent = PdlClientConfig.barnFnr)

        val opprettOppfyltDelvilkår = OpprettVilkårDto(
            vilkårType = VilkårType.PASS_BARN,
            barnId = barn.id,
            behandlingId = behandling.id,
            delvilkårsett = oppfylteDelvilkårPassBarnDto(),
            fom = LocalDate.now(),
            tom = LocalDate.now().plusDays(1),
            beløp = 1,
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
            assertThat(vilkårFraDb.fom).isEqualTo(LocalDate.now())
            assertThat(vilkårFraDb.tom).isEqualTo(LocalDate.now().plusDays(1))
            assertThat(vilkårFraDb.beløp).isEqualTo(1)
            assertThat(vilkårFraDb.barnId).isEqualTo(barn.id)
            assertThat(vilkårFraDb.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
            assertThat(vilkårFraDb.opphavsvilkår).isNull()
            assertThat(vilkårFraDb.delvilkårsett.map { it.hovedregel })
                .containsExactlyInAnyOrderElementsOf(opprettOppfyltDelvilkår.delvilkårsett.map { it.hovedregel() })
        }

        @Test
        fun `kan ikke opprette vilkår på behandling som er ferdigstilt`() {
            behandlingRepository.update(behandling.copy(status = BehandlingStatus.FERDIGSTILT))

            assertThatThrownBy {
                vilkårService.opprettNyttVilkår(opprettOppfyltDelvilkår)
            }.hasMessageContaining("Behandlingen er låst for videre redigering")
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
                vilkårService.opprettNyttVilkår(opprettOppfyltDelvilkår.copy(barnId = UUID.randomUUID()))
            }.hasMessageContaining("Finner ikke barn på behandling")
        }

        @Test
        fun `kan ikke opprette vilkårtype som ikke finnes på stønadstype`() {
            assertThatThrownBy {
                vilkårService.opprettNyttVilkår(opprettOppfyltDelvilkår.copy(vilkårType = VilkårType.EKSEMPEL))
            }.hasMessageContaining("Vilkårtype=EKSEMPEL eksisterer ikke for stønadstype=BARNETILSYN")
        }
    }

    private fun opprettVilkårsvurderinger(
        behandling: Behandling,
        barn: List<BehandlingBarn>,
    ): List<Vilkår> {
        val hovedregelMetadata =
            HovedregelMetadata(
                barn = barn,
                behandling = mockk(),
            )
        val delvilkårsett = EksempelRegel().initiereDelvilkår(hovedregelMetadata)
        val vilkårsett = listOf(
            vilkår(
                resultat = Vilkårsresultat.OPPFYLT,
                type = VilkårType.PASS_BARN,
                behandlingId = behandling.id,
                barnId = barn.first().id,
                delvilkår = delvilkårsett,
            ),
        )
        return vilkårRepository.insertAll(vilkårsett)
    }

    fun List<String>.tilBehandlingBarn(behandling: Behandling) =
        this.map { behandlingBarn(behandlingId = behandling.id, personIdent = it) }

    private fun assertVilkårErGjenbrukt(
        vilkårForBehandling: Vilkår,
        vilkårForRevurdering: Vilkår,
    ) {
        assertThat(vilkårForBehandling).usingRecursiveComparison()
            .ignoringFields("id", "sporbar", "behandlingId", "barnId", "opphavsvilkår")
            .isEqualTo(vilkårForRevurdering)

        assertThat(vilkårForRevurdering.opphavsvilkår?.behandlingId).isEqualTo(vilkårForBehandling.behandlingId)
    }
}
