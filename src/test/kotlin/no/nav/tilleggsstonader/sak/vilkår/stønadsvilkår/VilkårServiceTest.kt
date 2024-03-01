package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.test.fnr.FnrGenerator
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.behandling.fakta.BehandlingFaktaService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper.SøknadsskjemaBarnetilsynMapper
import no.nav.tilleggsstonader.sak.util.JournalpostUtil.lagJournalpost
import no.nav.tilleggsstonader.sak.util.SøknadUtil
import no.nav.tilleggsstonader.sak.util.SøknadUtil.barnMedBarnepass
import no.nav.tilleggsstonader.sak.util.VilkårGrunnlagUtil.mockVilkårGrunnlagDto
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Year
import java.util.UUID

internal class VilkårServiceTest {

    private val behandlingService = mockk<BehandlingService>()
    private val søknadService = mockk<SøknadService>()
    private val vilkårRepository = mockk<VilkårRepository>()

    // private val personopplysningerIntegrasjonerClient = mockk<PersonopplysningerIntegrasjonerClient>()
    // private val blankettRepository = mockk<BlankettRepository>()
    private val barnService = mockk<BarnService>()
    private val behandlingFaktaService = mockk<BehandlingFaktaService>()

    // private val grunnlagsdataService = mockk<GrunnlagsdataService>()
    private val fagsakService = mockk<FagsakService>()

    private val vilkårService = VilkårService(
        behandlingService = behandlingService,
        søknadService = søknadService,
        vilkårRepository = vilkårRepository,
        behandlingFaktaService = behandlingFaktaService,
        // grunnlagsdataService = grunnlagsdataService,
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
    private val behandling = behandling(fagsak(), BehandlingStatus.OPPRETTET, årsak = BehandlingÅrsak.PAPIRSØKNAD)
    private val behandlingId = behandling.id

    @BeforeEach
    fun setUp() {
        // every { behandlingService.hentAktivIdent(behandlingId) } returns søknad.fødselsnummer
        every { behandlingService.hentBehandling(behandlingId) } returns behandling
        // every { søknadService.hentSøknadsgrunnlag(any()) }.returns(søknad)

        every { vilkårRepository.insertAll(any()) } answers { firstArg() }
        // every { featureToggleService.isEnabled(any()) } returns true
        every { barnService.finnBarnPåBehandling(behandlingId) } returns barn
        every { fagsakService.hentFagsakForBehandling(behandlingId) } returns fagsak()

        every { behandlingFaktaService.hentFakta(behandlingId) } returns mockVilkårGrunnlagDto()
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
        resultat: Vilkårsresultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
    ) {
        assertThat(
            this.map { it.resultat }.toSet(),
        ).containsOnly(resultat)
    }

    private fun List<Vilkår>.finnVurderingResultaterForBarn(ident: UUID): List<Vilkårsresultat>? {
        return this.find { vilkår -> vilkår.barnId == ident }?.delvilkårsett?.map { delvilkår -> delvilkår.resultat }
    }
}
