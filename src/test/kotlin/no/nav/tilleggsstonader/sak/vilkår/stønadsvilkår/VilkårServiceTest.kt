package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.test.fnr.FnrGenerator
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.fakta.BehandlingFaktaService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.SøknadsskjemaBarnetilsynMapper
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil
import no.nav.tilleggsstonader.sak.util.JournalpostUtil.lagJournalpost
import no.nav.tilleggsstonader.sak.util.SøknadUtil
import no.nav.tilleggsstonader.sak.util.SøknadUtil.barnMedBarnepass
import no.nav.tilleggsstonader.sak.util.VilkårGrunnlagUtil.mockVilkårGrunnlagDto
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.søknadBarnTilBehandlingBarn
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Opphavsvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat.IKKE_TATT_STILLING_TIL
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat.OPPFYLT
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat.SKAL_IKKE_VURDERES
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OppdaterVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarPåVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil.ikkeOppfylteDelvilkårPassBarn
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.util.UUID

internal class VilkårServiceTest {

    private val behandlingService = mockk<BehandlingService>()
    private val vilkårRepository = mockk<VilkårRepository>()
    private val barnService = mockk<BarnService>()
    private val behandlingFaktaService = mockk<BehandlingFaktaService>()
    private val fagsakService = mockk<FagsakService>()

    private val vilkårService = VilkårService(
        behandlingService = behandlingService,
        vilkårRepository = vilkårRepository,
        behandlingFaktaService = behandlingFaktaService,
        barnService = barnService,
        fagsakService = fagsakService,
    )

    private val barnUnder9år = FnrGenerator.generer(Year.now().minusYears(1).value, 5, 19)
    private val barnOver10år = FnrGenerator.generer(Year.now().minusYears(11).value, 1, 13)

    private val søknad = SøknadsskjemaBarnetilsynMapper.map(
        SøknadUtil.søknadskjemaBarnetilsyn(
            barnMedBarnepass = listOf(
                barnMedBarnepass(ident = barnUnder9år),
                barnMedBarnepass(ident = barnOver10år),
            ),
        ),
        lagJournalpost(),
    )
    private val barn = søknadBarnTilBehandlingBarn(søknad.barn)
    private val fagsak = fagsak()
    private val behandling = behandling(
        fagsak = fagsak,
        status = BehandlingStatus.OPPRETTET,
        steg = StegType.VILKÅR,
        årsak = BehandlingÅrsak.PAPIRSØKNAD,
    )
    private val behandlingId = behandling.id

    @BeforeEach
    fun setUp() {
        mockHentBehandling(behandling)
        every { behandlingService.oppdaterStatusPåBehandling(any(), any()) } returns behandling

        every { vilkårRepository.insertAll(any()) } answers { firstArg() }
        every { barnService.finnBarnPåBehandling(behandlingId) } returns barn
        every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak()

        every { behandlingFaktaService.hentFakta(behandlingId) } returns mockVilkårGrunnlagDto()
        every { vilkårRepository.insertAll(any()) } answers { firstArg() }
        justRun { vilkårRepository.deleteById(any()) }
        BrukerContextUtil.mockBrukerContext("saksbehandlernavn")
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    @Disabled
    fun `skal ikke opprette nye Vilkår for behandlinger som allerede har vurderinger`() {
        every { vilkårRepository.findByBehandlingId(behandlingId) } returns listOf(
            vilkår(
                behandlingId = behandlingId,
                type = VilkårType.PASS_BARN,
                resultat = OPPFYLT,
            ),
        )

        vilkårService.hentEllerOpprettVilkårsvurdering(behandlingId)

        verify(exactly = 0) { vilkårRepository.updateAll(any()) }
        verify(exactly = 0) { vilkårRepository.insertAll(any()) }
    }

    /*
    @Test
    internal fun `skal ikke returnere delvilkår som er ikke aktuelle til frontend`() {
        val delvilkårsvurdering =
            SivilstandRegel().initiereDelvilkårsvurdering(
                HovedregelMetadata(
                    mockk(),
                    Sivilstandstype.ENKE_ELLER_ENKEMANN,
                    barn = emptyList(),
                    søktOmBarnetilsyn = emptyList(),
                    vilkårgrunnlagDto = mockk(),
                    behandling = mockk(),
                ),
            )
        every { vilkårsvurderingRepository.findByBehandlingId(behandlingId) } returns
            listOf(
                Vilkårsvurdering(
                    behandlingId = behandlingId,
                    type = VilkårType.SIVILSTAND,
                    delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvurdering),
                    opphavsvilkår = null,
                ),
            )

        val vilkår = vurderingService.hentEllerOpprettVurderinger(behandlingId)

        assertThat(delvilkårsvurdering).hasSize(5)
        assertThat(delvilkårsvurdering.filter { it.resultat == Vilkårsresultat.IKKE_AKTUELL }).hasSize(4)
        assertThat(delvilkårsvurdering.filter { it.resultat == Vilkårsresultat.IKKE_TATT_STILLING_TIL }).hasSize(1)

        assertThat(vilkår.vurderinger).hasSize(1)
        val delvilkårsvurderinger = vilkår.vurderinger.first().delvilkårsvurderinger
        assertThat(delvilkårsvurderinger).hasSize(1)
        assertThat(delvilkårsvurderinger.first().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(delvilkårsvurderinger.first().vurderinger).hasSize(1)
    }*/

    @Disabled
    @Test
    internal fun `skal ikke opprette vilkår hvis behandling er låst for videre vurdering`() {
        val eksisterendeVilkårsett = listOf(
            vilkår(
                behandlingId = behandlingId,
                type = VilkårType.PASS_BARN,
                resultat = OPPFYLT,
            ),
        )
        every { vilkårRepository.findByBehandlingId(behandlingId) } returns eksisterendeVilkårsett

        val vilkårsett = vilkårService.hentEllerOpprettVilkårsvurdering(behandlingId).vilkårsett

        assertThat(vilkårsett).hasSize(1)
        verify(exactly = 0) { vilkårRepository.insertAll(any()) }
        assertThat(vilkårsett.map { it.id }).isEqualTo(eksisterendeVilkårsett.map { it.id })
    }

    @Test
    internal fun `Skal returnere ikke oppfylt hvis vilkårsett ikke inneholder alle vilkår`() {
        val vilkårsett = listOf(
            vilkår(
                resultat = OPPFYLT,
                type = VilkårType.EKSEMPEL,
                behandlingId = behandlingId,
            ),
        )
        every { vilkårRepository.findByBehandlingId(behandlingId) } returns vilkårsett
        val erAlleVilkårOppfylt = vilkårService.erAlleVilkårOppfylt(behandlingId)
        assertThat(erAlleVilkårOppfylt).isFalse
    }

    @Test
    internal fun `Skal returnere oppfylt hvis alle vilår er oppfylt`() {
        val vilkårsett = lagVilkårsett(behandlingId, OPPFYLT)
        every { vilkårRepository.findByBehandlingId(behandlingId) } returns vilkårsett
        val erAlleVilkårOppfylt = vilkårService.erAlleVilkårOppfylt(behandlingId)
        assertThat(erAlleVilkårOppfylt).isTrue
    }

    @Test
    internal fun `Skal returnere ikke oppfylt hvis noen vilkår er SKAL_IKKE_VURDERES`() {
        val vilkårsett = lagVilkårsett(behandlingId, SKAL_IKKE_VURDERES)
        // Guard
        assertThat(
            vilkårsett.map { it.type }.containsAll(VilkårType.hentVilkårForStønad(Stønadstype.BARNETILSYN)),
        ).isTrue()
        every { vilkårRepository.findByBehandlingId(behandlingId) } returns vilkårsett

        val erAlleVilkårOppfylt = vilkårService.erAlleVilkårOppfylt(behandlingId)
        assertThat(erAlleVilkårOppfylt).isFalse
    }

    @Nested
    inner class OppdaterVilkår {
        @Test
        internal fun `kan ikke oppdatere vilkår koblet til en behandling som ikke finnes`() {
            val vilkårId = UUID.randomUUID()
            every { vilkårRepository.findByIdOrNull(vilkårId) } returns null
            assertThat(
                Assertions.catchThrowable {
                    vilkårService.oppdaterVilkår(
                        SvarPåVilkårDto(
                            id = vilkårId,
                            behandlingId = behandlingId,
                            delvilkårsett = listOf(),
                            fom = null,
                            tom = null,
                            utgift = null,
                        ),
                    )
                },
            ).hasMessageContaining("Finner ikke Vilkår med id")
        }

        @Test
        internal fun `skal oppdatere vilkår med resultat, begrunnelse og unntak`() {
            val lagretVilkår = slot<Vilkår>()
            val vilkår = initiererVilkår(lagretVilkår)

            vilkårService.oppdaterVilkår(
                SvarPåVilkårDto(
                    id = vilkår.id,
                    behandlingId = behandlingId,
                    delvilkårsett = PassBarnRegelTestUtil.oppfylteDelvilkårPassBarnDto(),
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 1, 31),
                    utgift = 1,
                ),
            )

            assertThat(lagretVilkår.captured.resultat).isEqualTo(OPPFYLT)
            assertThat(lagretVilkår.captured.type).isEqualTo(vilkår.type)
            assertThat(lagretVilkår.captured.fom).isEqualTo(LocalDate.of(2024, 1, 1))
            assertThat(lagretVilkår.captured.tom).isEqualTo(LocalDate.of(2024, 1, 31))
            assertThat(lagretVilkår.captured.utgift).isEqualTo(1)
            assertThat(lagretVilkår.captured.opphavsvilkår).isNull()

            assertThat(lagretVilkår.captured.delvilkårsett).hasSize(3)

            val delvilkår = lagretVilkår.captured.delvilkårsett.last()
            assertThat(delvilkår.hovedregel).isEqualTo(RegelId.HAR_FULLFØRT_FJERDEKLASSE)
            assertThat(delvilkår.resultat).isEqualTo(OPPFYLT)
            assertThat(delvilkår.vurderinger).hasSize(1)
            assertThat(delvilkår.vurderinger.first().svar).isEqualTo(SvarId.NEI)
            assertThat(delvilkår.vurderinger.first().begrunnelse).isEqualTo("en begrunnelse")
        }
    }

    @Test
    internal fun `skal oppdatere vilkår med resultat SKAL_IKKE_VURDERES`() {
        val oppdatertVilkår = slot<Vilkår>()
        val vilkår = initiererVilkår(oppdatertVilkår)

        vilkårService.settVilkårTilSkalIkkeVurderes(
            OppdaterVilkårDto(
                id = vilkår.id,
                behandlingId = behandlingId,
            ),
        )

        assertThat(oppdatertVilkår.captured.resultat).isEqualTo(SKAL_IKKE_VURDERES)
        assertThat(oppdatertVilkår.captured.type).isEqualTo(vilkår.type)
        assertThat(oppdatertVilkår.captured.opphavsvilkår).isNull()

        val delvilkårsett = oppdatertVilkår.captured.delvilkårsett.first()
        assertThat(delvilkårsett.resultat).isEqualTo(SKAL_IKKE_VURDERES)
        assertThat(delvilkårsett.vurderinger).hasSize(1)
        assertThat(delvilkårsett.vurderinger.first().svar).isNull()
        assertThat(delvilkårsett.vurderinger.first().begrunnelse).isNull()
    }

    @Nested
    inner class SlettVilkår {

        @Test
        internal fun `skal kunne slette vilkår opprettet i denne behandlingen`() {
            val vilkår = vilkår(behandlingId = behandlingId, type = VilkårType.PASS_BARN)
            every { vilkårRepository.findByIdOrNull(vilkår.id) } returns vilkår

            vilkårService.slettVilkår(OppdaterVilkårDto(vilkår.id, behandlingId))

            verify { vilkårRepository.deleteById(vilkår.id) }
        }

        @Test
        internal fun `skal ikke kunne slette vilkår opprettet i tidligere behandling`() {
            val vilkår = vilkår(
                behandlingId = behandlingId,
                type = VilkårType.PASS_BARN,
                opphavsvilkår = Opphavsvilkår(UUID.randomUUID(), LocalDateTime.now()),
            )
            every { vilkårRepository.findByIdOrNull(vilkår.id) } returns vilkår

            assertThatThrownBy {
                vilkårService.slettVilkår(OppdaterVilkårDto(vilkår.id, behandlingId))
            }.hasMessageContaining("Kan ikke slette vilkår opprettet i tidligere behandling")
        }
    }

    @Test
    internal fun `skal ikke oppdatere vilkår hvis behandlingen er låst for videre behandling`() {
        val behandling = behandling(
            fagsak(),
            BehandlingStatus.FERDIGSTILT,
            StegType.VILKÅR,
        )
        mockHentBehandling(behandling)

        val vilkår = vilkår(
            behandlingId,
            type = VilkårType.PASS_BARN,
            resultat = IKKE_TATT_STILLING_TIL,
        )
        every { vilkårRepository.findByIdOrNull(vilkår.id) } returns vilkår

        assertThat(
            Assertions.catchThrowable {
                vilkårService.oppdaterVilkår(
                    SvarPåVilkårDto(
                        id = vilkår.id,
                        behandlingId = behandlingId,
                        listOf(),
                        fom = null,
                        tom = null,
                        utgift = null,
                    ),
                )
            },
        ).isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("er låst for videre redigering")
        verify(exactly = 0) { vilkårRepository.insertAll(any()) }
    }

    @Test
    internal fun `skal ikke oppdatere vilkår hvis behandlingen ikke er i steg VILKÅR`() {
        val behandling = behandling(
            fagsak(),
            BehandlingStatus.UTREDES,
            StegType.INNGANGSVILKÅR,
        )
        mockHentBehandling(behandling)
        val vilkår = vilkår(
            behandlingId,
            type = VilkårType.PASS_BARN,
            resultat = IKKE_TATT_STILLING_TIL,
        )
        every { vilkårRepository.findByIdOrNull(vilkår.id) } returns vilkår

        assertThat(
            Assertions.catchThrowable {
                vilkårService.oppdaterVilkår(
                    SvarPåVilkårDto(
                        id = vilkår.id,
                        behandlingId = behandlingId,
                        listOf(),
                        fom = null,
                        tom = null,
                        utgift = null,
                    ),
                )
            },
        ).isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("Kan ikke oppdatere vilkår når behandling er på steg")
        verify(exactly = 0) { vilkårRepository.insertAll(any()) }
    }

    private fun lagVilkårsett(
        behandlingId: UUID,
        resultat: Vilkårsresultat = OPPFYLT,
    ): List<Vilkår> {
        return VilkårType.hentVilkårForStønad(Stønadstype.BARNETILSYN).map {
            vilkår(
                behandlingId = behandlingId,
                resultat = resultat,
                type = it,
                delvilkår = listOf(),
            )
        }
    }

    private fun List<Vilkår>.finnVilkårAvType(
        vilkårType: VilkårType,
    ): List<Vilkår> {
        return this.filter { it.type == vilkårType }
    }

    private fun List<Vilkår>.inneholderKunResultat(
        resultat: Vilkårsresultat = IKKE_TATT_STILLING_TIL,
    ) {
        assertThat(
            this.map { it.resultat }.toSet(),
        ).containsOnly(resultat)
    }

    private fun List<Vilkår>.finnVurderingResultaterForBarn(ident: UUID): List<Vilkårsresultat>? {
        return this.find { vilkår -> vilkår.barnId == ident }?.delvilkårsett?.map { delvilkår -> delvilkår.resultat }
    }

    private fun initiererVilkår(lagretVilkår: CapturingSlot<Vilkår>): Vilkår {
        val vilkår = vilkår(
            behandlingId = behandlingId,
            resultat = Vilkårsresultat.IKKE_OPPFYLT,
            delvilkår = ikkeOppfylteDelvilkårPassBarn(),
            type = VilkårType.PASS_BARN,
        )
        every { vilkårRepository.findByIdOrNull(vilkår.id) } returns vilkår
        every { vilkårRepository.findByBehandlingId(behandlingId) } returns listOf(vilkår)
        every { vilkårRepository.update(capture(lagretVilkår)) } answers { it.invocation.args.first() as Vilkår }
        return vilkår
    }

    private fun mockHentBehandling(behandling: Behandling) {
        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { behandlingService.hentSaksbehandling(behandlingId) } returns saksbehandling(fagsak, behandling)
    }
}
