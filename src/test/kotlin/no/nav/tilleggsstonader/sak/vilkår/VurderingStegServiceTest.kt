package no.nav.tilleggsstonader.sak.vilkår

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.historikk.BehandlingshistorikkService
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.StegUtfall
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.SøknadsskjemaMapper
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil
import no.nav.tilleggsstonader.sak.util.SøknadUtil.søknadskjemaBarnetilsyn
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.søknadBarnTilBehandlingBarn
import no.nav.tilleggsstonader.sak.util.vilkårsvurdering
import no.nav.tilleggsstonader.sak.vilkår.VilkårTestUtil.mockVilkårGrunnlagDto
import no.nav.tilleggsstonader.sak.vilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.Opphavsvilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.OppdaterVilkårsvurderingDto
import no.nav.tilleggsstonader.sak.vilkår.dto.SvarPåVurderingerDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VurderingDto
import no.nav.tilleggsstonader.sak.vilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.regler.evalutation.OppdaterVilkår.opprettNyeVilkårsvurderinger
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkårsreglerForStønad
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime
import java.util.UUID

internal class VurderingStegServiceTest {

    private val behandlingService = mockk<BehandlingService>()
    private val søknadService = mockk<SøknadService>()
    private val vilkårRepository = mockk<VilkårRepository>()
    private val barnService = mockk<BarnService>()

    // private val personopplysningerIntegrasjonerClient = mockk<PersonopplysningerIntegrasjonerClient>()
    // private val blankettRepository = mockk<BlankettRepository>()
    private val vilkårGrunnlagService = mockk<VilkårGrunnlagService>()

    // private val stegService = mockk<StegService>()
    // private val taskService = mockk<TaskService>()
    // private val grunnlagsdataService = mockk<GrunnlagsdataService>()
    private val fagsakService = mockk<FagsakService>()

    // private val featureToggleService = mockk<FeatureToggleService>()
    private val behandlingshistorikkService = mockk<BehandlingshistorikkService>()
    private val vurderingService = VurderingService(
        behandlingService,
        søknadService,
        vilkårRepository,
        barnService,
        vilkårGrunnlagService,
        // grunnlagsdataService,
        fagsakService,
        // featureToggleService,
    )
    private val vurderingStegService = VurderingStegService(
        behandlingService = behandlingService,
        vurderingService = vurderingService,
        vilkårRepository = vilkårRepository,
        // blankettRepository = blankettRepository,
        // stegService = stegService,
        // taskService = taskService,
        behandlingshistorikkService = behandlingshistorikkService,
    )
    private val søknad = SøknadsskjemaMapper.map(
        søknadskjemaBarnetilsyn(),
        "id",
    )
    private val barn = søknadBarnTilBehandlingBarn(søknad.barn)
    val fagsak = fagsak()
    private val behandling = behandling(fagsak, BehandlingStatus.OPPRETTET)
    private val behandlingId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        every { behandlingService.hentSaksbehandling(behandlingId) } returns saksbehandling(fagsak, behandling)
        // every { behandlingService.hentAktivIdent(behandlingId) } returns søknad.fødselsnummer
        every { behandlingService.oppdaterStatusPåBehandling(any(), any()) } returns behandling
        every { behandlingService.oppdaterKategoriPåBehandling(any(), any()) } returns behandling
        // every { søknadService.hentSøknadsgrunnlag(any()) }.returns(søknad)
        every { fagsakService.hentFagsakForBehandling(any()) } returns fagsak(stønadstype = Stønadstype.BARNETILSYN)
        every { vilkårRepository.insertAll(any()) } answers { firstArg() }
        every { vilkårGrunnlagService.hentGrunnlag(any(), any(), any()) } returns
            mockVilkårGrunnlagDto()

        justRun { behandlingshistorikkService.opprettHistorikkInnslag(any(), any(), any(), any()) }

        BrukerContextUtil.mockBrukerContext("saksbehandlernavn")
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `kan ikke oppdatere vilkårsvurdering koblet til en behandling som ikke finnes`() {
        val vurderingId = UUID.randomUUID()
        every { vilkårRepository.findByIdOrNull(vurderingId) } returns null
        assertThat(
            catchThrowable {
                vurderingStegService.oppdaterVilkår(
                    SvarPåVurderingerDto(
                        id = vurderingId,
                        behandlingId = behandlingId,
                        delvilkårsvurderinger = listOf(),
                    ),
                )
            },
        ).hasMessageContaining("Finner ikke Vilkårsvurdering med id")
    }

    @Test
    internal fun `skal oppdatere vilkårsvurdering med resultat, begrunnelse og unntak`() {
        val lagretVilkår = slot<Vilkår>()
        val vilkårsvurdering = initiererVurderinger(lagretVilkår)

        val delvilkårDto = listOf(
            DelvilkårDto(
                Vilkårsresultat.IKKE_OPPFYLT,
                listOf(VurderingDto(RegelId.HAR_ET_NAVN, SvarId.JA, "a")),
            ),
        )
        vurderingStegService.oppdaterVilkår(
            SvarPåVurderingerDto(
                id = vilkårsvurdering.id,
                behandlingId = behandlingId,
                delvilkårsvurderinger = delvilkårDto,
            ),
        )

        assertThat(lagretVilkår.captured.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(lagretVilkår.captured.type).isEqualTo(vilkårsvurdering.type)
        assertThat(lagretVilkår.captured.opphavsvilkår).isNull()

        val delvilkårsvurdering = lagretVilkår.captured.delvilkårwrapper.delvilkårsett.first()
        assertThat(delvilkårsvurdering.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(delvilkårsvurdering.vurderinger).hasSize(1)
        assertThat(delvilkårsvurdering.vurderinger.first().svar).isEqualTo(SvarId.JA)
        assertThat(delvilkårsvurdering.vurderinger.first().begrunnelse).isEqualTo("a")
    }

    @Test
    internal fun `skal oppdatere vilkårsvurdering med resultat SKAL_IKKE_VURDERES`() {
        every { barnService.finnBarnPåBehandling(behandlingId) } returns barn
        val oppdatertVurdering = slot<Vilkår>()
        val vilkårsvurdering = initiererVurderinger(oppdatertVurdering)

        vurderingStegService.settVilkårTilSkalIkkeVurderes(
            OppdaterVilkårsvurderingDto(
                id = vilkårsvurdering.id,
                behandlingId = behandlingId,
            ),
        )

        assertThat(oppdatertVurdering.captured.resultat).isEqualTo(Vilkårsresultat.SKAL_IKKE_VURDERES)
        assertThat(oppdatertVurdering.captured.type).isEqualTo(vilkårsvurdering.type)
        assertThat(oppdatertVurdering.captured.opphavsvilkår).isNull()

        val delvilkårsvurdering = oppdatertVurdering.captured.delvilkårwrapper.delvilkårsett.first()
        assertThat(delvilkårsvurdering.resultat).isEqualTo(Vilkårsresultat.SKAL_IKKE_VURDERES)
        assertThat(delvilkårsvurdering.vurderinger).hasSize(1)
        assertThat(delvilkårsvurdering.vurderinger.first().svar).isNull()
        assertThat(delvilkårsvurdering.vurderinger.first().begrunnelse).isNull()
    }

    @Test
    internal fun `nullstille skal fjerne opphavsvilkår fra vilkårsvurdering`() {
        every { barnService.finnBarnPåBehandling(behandlingId) } returns barn
        val oppdatertVurdering = slot<Vilkår>()
        val vilkårsvurdering = initiererVurderinger(oppdatertVurdering)

        vurderingStegService.nullstillVilkår(OppdaterVilkårsvurderingDto(vilkårsvurdering.id, behandlingId))

        assertThat(oppdatertVurdering.captured.resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(oppdatertVurdering.captured.type).isEqualTo(vilkårsvurdering.type)
        assertThat(oppdatertVurdering.captured.opphavsvilkår).isNull()
    }

    @Test
    internal fun `skal ikke oppdatere vilkårsvurdering hvis behandlingen er låst for videre behandling`() {
        every { behandlingService.hentBehandling(behandlingId) } returns behandling(
            fagsak(),
            BehandlingStatus.FERDIGSTILT,
        )
        val vilkårsvurdering = vilkårsvurdering(
            behandlingId,
            resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
            VilkårType.EKSEMPEL,
        )
        every { vilkårRepository.findByIdOrNull(vilkårsvurdering.id) } returns vilkårsvurdering

        assertThat(
            catchThrowable {
                vurderingStegService.oppdaterVilkår(
                    SvarPåVurderingerDto(
                        id = vilkårsvurdering.id,
                        behandlingId = behandlingId,
                        listOf(),
                    ),
                )
            },
        ).isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("er låst for videre redigering")
        verify(exactly = 0) { vilkårRepository.insertAll(any()) }
    }

    @Test
    internal fun `skal oppdatere status fra OPPRETTET til UTREDES og lage historikkinnslag for første vilkår`() {
        every { behandlingService.hentSaksbehandling(behandlingId) } returns saksbehandling(
            fagsak(),
            status = BehandlingStatus.OPPRETTET,
        )
        val lagretVilkår = slot<Vilkår>()
        val vilkårsvurdering = initiererVurderinger(lagretVilkår)
        val delvilkårDto = listOf(
            DelvilkårDto(
                Vilkårsresultat.IKKE_OPPFYLT,
                listOf(VurderingDto(RegelId.HAR_ET_NAVN, SvarId.JA, "a")),
            ),
        )
        vurderingStegService.oppdaterVilkår(
            SvarPåVurderingerDto(
                id = vilkårsvurdering.id,
                behandlingId = behandlingId,
                delvilkårsvurderinger = delvilkårDto,
            ),
        )

        verify(exactly = 1) { behandlingService.oppdaterStatusPåBehandling(any(), BehandlingStatus.UTREDES) }
        verify(exactly = 1) {
            behandlingshistorikkService.opprettHistorikkInnslag(
                any(),
                StegType.VILKÅR,
                StegUtfall.UTREDNING_PÅBEGYNT,
                metadata = null,
            )
        }
    }

    @Test
    internal fun `skal ikke oppdatere status til UTREDES eller opprette historikkinnslag hvis den allerede er dette `() {
        val fagsak = fagsak()
        every { behandlingService.hentSaksbehandling(behandlingId) } returns saksbehandling(
            fagsak,
            status = BehandlingStatus.UTREDES,
        )
        val lagretVilkår = slot<Vilkår>()
        val vilkårsvurdering = initiererVurderinger(lagretVilkår)
        val delvilkårDto = listOf(
            DelvilkårDto(
                Vilkårsresultat.IKKE_OPPFYLT,
                listOf(VurderingDto(RegelId.HAR_ET_NAVN, SvarId.JA, "a")),
            ),
        )
        vurderingStegService.oppdaterVilkår(
            SvarPåVurderingerDto(
                id = vilkårsvurdering.id,
                behandlingId = behandlingId,
                delvilkårsvurderinger = delvilkårDto,
            ),
        )

        verify(exactly = 0) { behandlingService.oppdaterStatusPåBehandling(any(), BehandlingStatus.UTREDES) }
        verify(exactly = 0) {
            behandlingshistorikkService.opprettHistorikkInnslag(
                any(),
                StegType.VILKÅR,
                StegUtfall.UTREDNING_PÅBEGYNT,
                metadata = null,
            )
        }
    }

    @Test
    internal fun `behandlingen uten barn skal likevel opprette et vilkår for aleneomsorg`() {
        val vilkårsvurderinger =
            opprettNyeVilkårsvurderinger(
                behandlingId,
                HovedregelMetadata(
                    barn = emptyList(),
                    behandling = behandling,
                ),
                Stønadstype.BARNETILSYN,
            )

        assertThat(vilkårsvurderinger).hasSize(vilkårsreglerForStønad(Stønadstype.BARNETILSYN).size)
        assertThat(vilkårsvurderinger.count { it.type == VilkårType.EKSEMPEL }).isEqualTo(1)
    }

    // KUN FOR Å TESTE OPPDATERSTEG
    private fun initiererVurderinger(lagretVilkår: CapturingSlot<Vilkår>): Vilkår {
        val vilkårsvurdering =
            vilkårsvurdering(
                behandlingId,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                VilkårType.EKSEMPEL,
                listOf(
                    Delvilkår(
                        Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                        listOf(Vurdering(RegelId.HAR_ET_NAVN)),
                    ),
                ),
                opphavsvilkår = Opphavsvilkår(UUID.randomUUID(), LocalDateTime.now()),
            )
        val vilkårsvurderinger =
            opprettNyeVilkårsvurderinger(
                behandlingId,
                HovedregelMetadata(
                    barn = barn,
                    behandling = behandling,
                ),
                Stønadstype.BARNETILSYN,
            )
                .map { if (it.type == vilkårsvurdering.type) vilkårsvurdering else it }

        every { vilkårRepository.findByIdOrNull(vilkårsvurdering.id) } returns vilkårsvurdering
        every { vilkårRepository.findByBehandlingId(behandlingId) } returns vilkårsvurderinger
        every { vilkårRepository.update(capture(lagretVilkår)) } answers
            { it.invocation.args.first() as Vilkår }
        return vilkårsvurdering
    }
}
