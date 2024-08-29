package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import io.mockk.CapturingSlot
import io.mockk.every
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
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockUnleashService
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
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat.AUTOMATISK_OPPFYLT
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat.IKKE_TATT_STILLING_TIL
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat.OPPFYLT
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat.SKAL_IKKE_VURDERES
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OppdaterVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarPåVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation.OppdaterVilkår.opprettNyeVilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelTestUtil
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
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
        unleashService = mockUnleashService(),
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
        BrukerContextUtil.mockBrukerContext("saksbehandlernavn")
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Nested
    inner class OppretteVilkårBarnetilsyn {
        val nyttVilkårsett = slot<List<Vilkår>>()

        @BeforeEach
        fun setUp() {
            every { vilkårRepository.insertAll(capture(nyttVilkårsett)) } answers { firstArg() }
            every { vilkårRepository.findByBehandlingId(behandlingId) } returns emptyList()
            every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak()
        }

        @Test
        fun `skal opprette nye Vilkår for barnetilsyn med alle vilkår dersom ingen vurderinger finnes`() {
            val aktuelleVilkårTyper = VilkårType.hentVilkårForStønad(Stønadstype.BARNETILSYN)

            vilkårService.hentEllerOpprettVilkårsvurdering(behandlingId)

            val vilkårsett = nyttVilkårsett.captured

            assertThat(vilkårsett).hasSize(aktuelleVilkårTyper.size + 1) // 2 barn, ekstra vilkår av type PASS_BARN
            assertThat(
                vilkårsett.map { it.type }.distinct(),
            ).containsExactlyInAnyOrderElementsOf(aktuelleVilkårTyper)

            vilkårsett.finnVilkårAvType(VilkårType.PASS_BARN).inneholderKunResultat(IKKE_TATT_STILLING_TIL)
        }

        @Test
        fun `skal opprette et vilkår av type PASS_BARN per barn`() {
            vilkårService.hentEllerOpprettVilkårsvurdering(behandlingId)

            val vilkårPassBarn = nyttVilkårsett.captured.finnVilkårAvType(VilkårType.PASS_BARN)
            assertThat(vilkårPassBarn).hasSize(2)
        }

        @Test
        fun `skal initiere automatisk verdi for ALDER_LAVERE_ENN_GRENSEVERDI`() {
            vilkårService.hentEllerOpprettVilkårsvurdering(behandlingId)

            val vilkårPassBarn = nyttVilkårsett.captured.finnVilkårAvType(VilkårType.PASS_BARN)

            val resultaterBarnUnder9år =
                vilkårPassBarn.finnVurderingResultaterForBarn(barn.single { it.ident == barnUnder9år }.id)
            assertThat(resultaterBarnUnder9år).containsOnlyOnce(AUTOMATISK_OPPFYLT)

            val resultaterBarnOver10år =
                vilkårPassBarn.finnVurderingResultaterForBarn(barn.single { it.ident == barnOver10år }.id)
            assertThat(resultaterBarnOver10år).containsOnly(IKKE_TATT_STILLING_TIL)
        }
    }

    @Test
    fun `skal ikke opprette nye Vilkår for behandlinger som allerede har vurderinger`() {
        every { vilkårRepository.findByBehandlingId(behandlingId) } returns listOf(
            vilkår(
                resultat = OPPFYLT,
                type = VilkårType.EKSEMPEL,
                behandlingId = behandlingId,
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

    @Test
    internal fun `skal ikke opprette vilkår hvis behandling er låst for videre vurdering`() {
        val eksisterendeVilkårsett = listOf(
            vilkår(
                resultat = OPPFYLT,
                type = VilkårType.EKSEMPEL,
                behandlingId = behandlingId,
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

    @Test
    fun `Skal opprette vilkår for nye barn`() {
        val vilkårsett = lagVilkårsett(behandlingId, OPPFYLT)
        every { vilkårRepository.findByBehandlingId(behandlingId) } returns vilkårsett
        every { barnService.finnBarnPåBehandling(behandlingId) } returns barn
        every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak()

        val vilkårSlot = slot<List<Vilkår>>()

        every { vilkårRepository.insertAll(capture(vilkårSlot)) } answers { vilkårSlot.captured }

        vilkårService.hentEllerOpprettVilkårsvurdering(behandlingId)

        assertThat(vilkårSlot.captured).hasSize(2)
        assertThat(vilkårSlot.captured.map { it.barnId }).containsExactlyInAnyOrderElementsOf(barn.map { it.id })
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
                            beløp = null,
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
                    fom = LocalDate.now(),
                    tom = LocalDate.now(),
                    beløp = 1,
                ),
            )

            assertThat(lagretVilkår.captured.resultat).isEqualTo(OPPFYLT)
            assertThat(lagretVilkår.captured.type).isEqualTo(vilkår.type)
            assertThat(lagretVilkår.captured.fom).isEqualTo(LocalDate.now())
            assertThat(lagretVilkår.captured.tom).isEqualTo(LocalDate.now())
            assertThat(lagretVilkår.captured.beløp).isEqualTo(1)
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
    inner class NullstillVilkår {

        @Test
        internal fun `nullstille skal fjerne opphavsvilkår fra vilkår`() {
            val oppdatertVilkår = slot<Vilkår>()
            val vilkår = initiererVilkår(oppdatertVilkår)

            vilkårService.nullstillVilkår(OppdaterVilkårDto(vilkår.id, behandlingId))

            assertThat(oppdatertVilkår.captured.resultat).isEqualTo(IKKE_TATT_STILLING_TIL)
            assertThat(oppdatertVilkår.captured.type).isEqualTo(vilkår.type)
            assertThat(oppdatertVilkår.captured.opphavsvilkår).isNull()
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
            resultat = IKKE_TATT_STILLING_TIL,
            VilkårType.PASS_BARN,
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
                        beløp = null,
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
            resultat = IKKE_TATT_STILLING_TIL,
            VilkårType.PASS_BARN,
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
                        beløp = null,
                    ),
                )
            },
        ).isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("Kan ikke oppdatere vilkår når behandling er på steg")
        verify(exactly = 0) { vilkårRepository.insertAll(any()) }
    }

    @Test
    internal fun `behandlingen uten barn skal kaste feilmelding`() {
        Assertions.assertThatThrownBy {
            opprettNyeVilkår(
                behandlingId,
                HovedregelMetadata(
                    barn = emptyList(),
                    behandling = behandling,
                ),
                Stønadstype.BARNETILSYN,
            )
        }.hasMessageContaining("Kan ikke opprette vilkår når ingen barn er knyttet til behandling")
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
        val vilkår =
            opprettNyeVilkår(
                behandlingId,
                HovedregelMetadata(
                    barn = barn,
                    behandling = behandling,
                ),
                Stønadstype.BARNETILSYN,
            ).first()
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
