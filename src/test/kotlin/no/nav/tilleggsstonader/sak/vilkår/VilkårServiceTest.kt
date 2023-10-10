package no.nav.tilleggsstonader.sak.vilkår

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.Stønadstype
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.SøknadsskjemaMapper
import no.nav.tilleggsstonader.sak.util.SøknadUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.søknadBarnTilBehandlingBarn
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.VilkårTestUtil.mockVilkårGrunnlagDto
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat.OPPFYLT
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat.SKAL_IKKE_VURDERES
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

internal class VilkårServiceTest {

    private val behandlingService = mockk<BehandlingService>()
    private val søknadService = mockk<SøknadService>()
    private val vilkårRepository = mockk<VilkårRepository>()

    // private val personopplysningerIntegrasjonerClient = mockk<PersonopplysningerIntegrasjonerClient>()
    // private val blankettRepository = mockk<BlankettRepository>()
    private val barnService = mockk<BarnService>()
    private val vilkårGrunnlagService = mockk<VilkårGrunnlagService>()

    // private val grunnlagsdataService = mockk<GrunnlagsdataService>()
    private val fagsakService = mockk<FagsakService>()
    private val vilkårService = VilkårService(
        behandlingService = behandlingService,
        søknadService = søknadService,
        vilkårRepository = vilkårRepository,
        vilkårGrunnlagService = vilkårGrunnlagService,
        // grunnlagsdataService = grunnlagsdataService,
        barnService = barnService,
        fagsakService = fagsakService,
    )
    private val søknad = SøknadsskjemaMapper.map(
        SøknadUtil.søknadskjemaBarnetilsyn(),
        "id",
    )
    private val barn = søknadBarnTilBehandlingBarn(søknad.barn)
    private val behandling = behandling(fagsak(), BehandlingStatus.OPPRETTET, årsak = BehandlingÅrsak.PAPIRSØKNAD)
    private val behandlingId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        // every { behandlingService.hentAktivIdent(behandlingId) } returns søknad.fødselsnummer
        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        // every { søknadService.hentSøknadsgrunnlag(any()) }.returns(søknad)

        every { vilkårRepository.insertAll(any()) } answers { firstArg() }
        // every { featureToggleService.isEnabled(any()) } returns true
        every { barnService.finnBarnPåBehandling(behandlingId) } returns barn
        every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak()

        every { vilkårGrunnlagService.hentGrunnlag(any(), any(), any()) } returns
            mockVilkårGrunnlagDto()
    }

    /*
    @Test
    fun `skal opprette nye Vilkår for barnetilsyn med alle vilkår dersom ingen vurderinger finnes`() {
        every { vilkårsvurderingRepository.findByBehandlingId(behandlingId) } returns emptyList()
        every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak()

        val nyeVilkårsvurderinger = slot<List<Vilkårsvurdering>>()
        every { vilkårsvurderingRepository.insertAll(capture(nyeVilkårsvurderinger)) } answers { firstArg() }
        val vilkår = VilkårType.hentVilkårForStønad(Stønadstype.BARNETILSYN)

        vurderingService.hentEllerOpprettVurderinger(behandlingId)

        assertThat(nyeVilkårsvurderinger.captured).hasSize(vilkår.size + 2) // 2 barn, Ekstra aleneomsorgsvilkår og aldersvilkår
        assertThat(
            nyeVilkårsvurderinger.captured.map { it.type }
                .distinct(),
        ).containsExactlyInAnyOrderElementsOf(vilkår)
        // assertThat(nyeVilkårsvurderinger.captured.filter { it.type == VilkårType.ALENEOMSORG }).hasSize(2)
        // assertThat(nyeVilkårsvurderinger.captured.filter { it.type == VilkårType.ALDER_PÅ_BARN }).hasSize(2)
        assertThat(nyeVilkårsvurderinger.captured.filter { it.barnId != null }).hasSize(4)
        assertThat(
            nyeVilkårsvurderinger.captured.filter { it.type == VilkårType.EKSEMPEL }
                .map { it.resultat }
                .toSet(),
        ).containsOnly(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(
            nyeVilkårsvurderinger.captured.filter { it.type == VilkårType.EKSEMPEL }
                .map { it.resultat }
                .toSet(),
        ).containsOnly(OPPFYLT)
        assertThat(nyeVilkårsvurderinger.captured.map { it.behandlingId }.toSet()).containsOnly(behandlingId)
    }
     */

    @Test
    fun `skal ikke opprette nye Vilkår for behandlinger som allerede har vurderinger`() {
        every { vilkårRepository.findByBehandlingId(behandlingId) } returns
            listOf(
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
            vilkårsett.map { it.type }
                .containsAll(VilkårType.hentVilkårForStønad(Stønadstype.BARNETILSYN)),
        ).isTrue()
        every { vilkårRepository.findByBehandlingId(behandlingId) } returns vilkårsett

        val erAlleVilkårOppfylt = vilkårService.erAlleVilkårOppfylt(behandlingId)
        assertThat(erAlleVilkårOppfylt).isFalse
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
}
