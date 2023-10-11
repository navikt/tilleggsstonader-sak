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
import no.nav.tilleggsstonader.sak.util.VilkårGrunnlagUtil.mockVilkårGrunnlagDto
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.søknadBarnTilBehandlingBarn
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.Opphavsvilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.OppdaterVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.SvarPåVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.dto.VurderingDto
import no.nav.tilleggsstonader.sak.vilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.regler.evalutation.OppdaterVilkår.opprettNyeVilkår
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkårsreglerForStønad
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDateTime
import java.util.UUID

internal class VilkårStegServiceTest {

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
    private val vilkårService = VilkårService(
        behandlingService,
        søknadService,
        vilkårRepository,
        barnService,
        vilkårGrunnlagService,
        // grunnlagsdataService,
        fagsakService,
        // featureToggleService,
    )
    private val vilkårStegService = VilkårStegService(
        behandlingService = behandlingService,
        vilkårService = vilkårService,
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
    private val behandlingId = behandling.id

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
        every { vilkårGrunnlagService.hentGrunnlag(any()) } returns mockVilkårGrunnlagDto()

        justRun { behandlingshistorikkService.opprettHistorikkInnslag(any(), any(), any(), any()) }

        BrukerContextUtil.mockBrukerContext("saksbehandlernavn")
    }

    @AfterEach
    internal fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `kan ikke oppdatere vilkår koblet til en behandling som ikke finnes`() {
        val vilkårId = UUID.randomUUID()
        every { vilkårRepository.findByIdOrNull(vilkårId) } returns null
        assertThat(
            catchThrowable {
                vilkårStegService.oppdaterVilkår(
                    SvarPåVilkårDto(
                        id = vilkårId,
                        behandlingId = behandlingId,
                        delvilkårsett = listOf(),
                    ),
                )
            },
        ).hasMessageContaining("Finner ikke Vilkår med id")
    }

    @Test
    internal fun `skal oppdatere vilkår med resultat, begrunnelse og unntak`() {
        val lagretVilkår = slot<Vilkår>()
        val vilkår = initiererVilkår(lagretVilkår)

        val delvilkårDto = listOf(
            DelvilkårDto(
                Vilkårsresultat.IKKE_OPPFYLT,
                listOf(VurderingDto(RegelId.HAR_ET_NAVN, SvarId.JA, "a")),
            ),
        )
        vilkårStegService.oppdaterVilkår(
            SvarPåVilkårDto(
                id = vilkår.id,
                behandlingId = behandlingId,
                delvilkårsett = delvilkårDto,
            ),
        )

        assertThat(lagretVilkår.captured.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(lagretVilkår.captured.type).isEqualTo(vilkår.type)
        assertThat(lagretVilkår.captured.opphavsvilkår).isNull()

        val delvilkår = lagretVilkår.captured.delvilkårsett.first()
        assertThat(delvilkår.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(delvilkår.vurderinger).hasSize(1)
        assertThat(delvilkår.vurderinger.first().svar).isEqualTo(SvarId.JA)
        assertThat(delvilkår.vurderinger.first().begrunnelse).isEqualTo("a")
    }

    @Test
    internal fun `skal oppdatere vilkår med resultat SKAL_IKKE_VURDERES`() {
        every { barnService.finnBarnPåBehandling(behandlingId) } returns barn
        val oppdatertVilkår = slot<Vilkår>()
        val vilkår = initiererVilkår(oppdatertVilkår)

        vilkårStegService.settVilkårTilSkalIkkeVurderes(
            OppdaterVilkårDto(
                id = vilkår.id,
                behandlingId = behandlingId,
            ),
        )

        assertThat(oppdatertVilkår.captured.resultat).isEqualTo(Vilkårsresultat.SKAL_IKKE_VURDERES)
        assertThat(oppdatertVilkår.captured.type).isEqualTo(vilkår.type)
        assertThat(oppdatertVilkår.captured.opphavsvilkår).isNull()

        val delvilkårsett = oppdatertVilkår.captured.delvilkårsett.first()
        assertThat(delvilkårsett.resultat).isEqualTo(Vilkårsresultat.SKAL_IKKE_VURDERES)
        assertThat(delvilkårsett.vurderinger).hasSize(1)
        assertThat(delvilkårsett.vurderinger.first().svar).isNull()
        assertThat(delvilkårsett.vurderinger.first().begrunnelse).isNull()
    }

    @Test
    internal fun `nullstille skal fjerne opphavsvilkår fra vilkår`() {
        every { barnService.finnBarnPåBehandling(behandlingId) } returns barn
        val oppdatertVilkår = slot<Vilkår>()
        val vilkår = initiererVilkår(oppdatertVilkår)

        vilkårStegService.nullstillVilkår(OppdaterVilkårDto(vilkår.id, behandlingId))

        assertThat(oppdatertVilkår.captured.resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(oppdatertVilkår.captured.type).isEqualTo(vilkår.type)
        assertThat(oppdatertVilkår.captured.opphavsvilkår).isNull()
    }

    @Test
    internal fun `skal ikke oppdatere vilkår hvis behandlingen er låst for videre behandling`() {
        every { behandlingService.hentBehandling(behandlingId) } returns behandling(
            fagsak(),
            BehandlingStatus.FERDIGSTILT,
        )
        val vilkår = vilkår(
            behandlingId,
            resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
            VilkårType.EKSEMPEL,
        )
        every { vilkårRepository.findByIdOrNull(vilkår.id) } returns vilkår

        assertThat(
            catchThrowable {
                vilkårStegService.oppdaterVilkår(
                    SvarPåVilkårDto(
                        id = vilkår.id,
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
        val vilkår = initiererVilkår(lagretVilkår)
        val delvilkårDto = listOf(
            DelvilkårDto(
                Vilkårsresultat.IKKE_OPPFYLT,
                listOf(VurderingDto(RegelId.HAR_ET_NAVN, SvarId.JA, "a")),
            ),
        )
        vilkårStegService.oppdaterVilkår(
            SvarPåVilkårDto(
                id = vilkår.id,
                behandlingId = behandlingId,
                delvilkårsett = delvilkårDto,
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
        val vilkår = initiererVilkår(lagretVilkår)
        val delvilkårDto = listOf(
            DelvilkårDto(
                Vilkårsresultat.IKKE_OPPFYLT,
                listOf(VurderingDto(RegelId.HAR_ET_NAVN, SvarId.JA, "a")),
            ),
        )
        vilkårStegService.oppdaterVilkår(
            SvarPåVilkårDto(
                id = vilkår.id,
                behandlingId = behandlingId,
                delvilkårsett = delvilkårDto,
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
        val vilkårsett =
            opprettNyeVilkår(
                behandlingId,
                HovedregelMetadata(
                    barn = emptyList(),
                    behandling = behandling,
                ),
                Stønadstype.BARNETILSYN,
            )

        assertThat(vilkårsett).hasSize(vilkårsreglerForStønad(Stønadstype.BARNETILSYN).size)
        assertThat(vilkårsett.count { it.type == VilkårType.EKSEMPEL }).isEqualTo(1)
    }

    // KUN FOR Å TESTE OPPDATERSTEG
    private fun initiererVilkår(lagretVilkår: CapturingSlot<Vilkår>): Vilkår {
        val vilkår =
            vilkår(
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
        val vilkårsett =
            opprettNyeVilkår(
                behandlingId,
                HovedregelMetadata(
                    barn = barn,
                    behandling = behandling,
                ),
                Stønadstype.BARNETILSYN,
            )
                .map { if (it.type == vilkår.type) vilkår else it }

        every { vilkårRepository.findByIdOrNull(vilkår.id) } returns vilkår
        every { vilkårRepository.findByBehandlingId(behandlingId) } returns vilkårsett
        every { vilkårRepository.update(capture(lagretVilkår)) } answers
            { it.invocation.args.first() as Vilkår }
        return vilkår
    }
}
