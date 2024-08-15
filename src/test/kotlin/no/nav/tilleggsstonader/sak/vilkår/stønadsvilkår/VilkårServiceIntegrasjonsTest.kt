package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.søknad.barnetilsyn.BarnMedBarnepass
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.GjennbrukDataRevurderingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import no.nav.tilleggsstonader.sak.util.JournalpostUtil.lagJournalpost
import no.nav.tilleggsstonader.sak.util.SøknadUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.søknadBarnTilBehandlingBarn
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Opphavsvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.EksempelRegel
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

internal class VilkårServiceIntegrasjonsTest : IntegrationTest() {

    @Autowired
    lateinit var vilkårRepository: VilkårRepository

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var vilkårService: VilkårService

    @Autowired
    lateinit var søknadService: SøknadService

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
        val søknadskjema = lagreSøknad(behandling)
        val barnPåFørsteSøknad = barnRepository.insertAll(søknadBarnTilBehandlingBarn(søknadskjema.barn, behandling.id))
        val barnIdMap = barnService.gjenbrukBarn(behandling.id, revurdering.id)

        val vilkårForBehandling = opprettVilkårsvurderinger(søknadskjema, behandling, barnPåFørsteSøknad).first()

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
        assertThat(vilkårForBehandling.barnId).isEqualTo(barnPåFørsteSøknad.first().id)
        assertThat(vilkårForBehandling.opphavsvilkår).isNull()
        assertThat(vilkårForRevurdering.barnId).isEqualTo(barnIdMap[barnPåFørsteSøknad.first().id])
        assertThat(vilkårForRevurdering.opphavsvilkår)
            .isEqualTo(Opphavsvilkår(behandling.id, vilkårForBehandling.sporbar.endret.endretTid))

        assertThat(vilkårForBehandling).usingRecursiveComparison()
            .ignoringFields("id", "sporbar", "behandlingId", "barnId", "opphavsvilkår")
            .isEqualTo(vilkårForRevurdering)
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

    /**
     * Søknad 1: Barn1
     *
     * Søknad 2: Barn2
     */
    @Test
    internal fun `hentEllerOpprettVilkår skal opprette vilkår for nytt barn og kopiere vilkår for eksisterende barn på fagsak`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())

        val barn1 = SøknadUtil.barnMedBarnepass()
        val førstegangsbehandling = testoppsettService.lagre(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        val førsteSøknad = lagreSøknad(førstegangsbehandling, listOf(barn1))
        val barnPåFørsteSøknad = barnService.opprettBarn(søknadBarnTilBehandlingBarn(førsteSøknad.barn, førstegangsbehandling.id))
        opprettVilkårsvurderinger(førsteSøknad, førstegangsbehandling, barnPåFørsteSøknad)

        val barn2 = SøknadUtil.barnMedBarnepass()
        val revurdering = testoppsettService.lagre(behandling(fagsak))
        val revurderingSøknad = lagreSøknad(revurdering, listOf(barn2))
        gjennbrukDataRevurderingService.gjenbrukData(revurdering, førstegangsbehandling.id)
        barnService.opprettBarn(søknadBarnTilBehandlingBarn(revurderingSøknad.barn, revurdering.id))

        val barnRevurdering = barnService.finnBarnPåBehandling(revurdering.id)
        vilkårService.hentEllerOpprettVilkår(revurdering.id, HovedregelMetadata(barnRevurdering, revurdering))

        val vilkårFørstegangsbehandling = vilkårRepository.findByBehandlingId(førstegangsbehandling.id).single()
        assertThat(vilkårFørstegangsbehandling.barnId).isEqualTo(barnPåFørsteSøknad.single().id)

        val vilkårRevurdering = vilkårRepository.findByBehandlingId(revurdering.id)
        assertThat(vilkårRevurdering.size).isEqualTo(2)

        val barn1Id = barnRevurdering.single { it.ident == barn1.ident.verdi }.id
        val vilkårBarn1 = vilkårRevurdering.single { it.barnId == barn1Id }

        assertThat(vilkårFørstegangsbehandling.id).isNotEqualTo(vilkårBarn1.id)
        assertThat(vilkårFørstegangsbehandling.behandlingId).isNotEqualTo(vilkårBarn1.behandlingId)
        assertThat(vilkårFørstegangsbehandling.sporbar.opprettetTid).isNotEqualTo(vilkårBarn1.sporbar.opprettetTid)
        assertThat(vilkårFørstegangsbehandling.sporbar.endret.endretTid).isNotEqualTo(vilkårBarn1.sporbar.endret.endretTid)
        assertThat(vilkårFørstegangsbehandling.barnId).isNotEqualTo(vilkårBarn1.barnId)
        assertThat(vilkårFørstegangsbehandling.barnId).isEqualTo(barnPåFørsteSøknad.first().id)
        assertThat(vilkårFørstegangsbehandling.opphavsvilkår).isNull()
        assertThat(vilkårBarn1.opphavsvilkår)
            .isEqualTo(Opphavsvilkår(førstegangsbehandling.id, vilkårFørstegangsbehandling.sporbar.endret.endretTid))

        assertThat(vilkårFørstegangsbehandling).usingRecursiveComparison()
            .ignoringFields("id", "sporbar", "behandlingId", "barnId", "opphavsvilkår")
            .isEqualTo(vilkårBarn1)

        val barn2Id = barnRevurdering.single { it.ident == barn2.ident.verdi }.id
        val vilkårBarn2 = vilkårRevurdering.single { it.barnId == barn2Id }

        assertThat(vilkårBarn2.barnId).isEqualTo(barn2Id)
        assertThat(vilkårBarn2.behandlingId).isEqualTo(revurdering.id)
        assertThat(vilkårBarn2.opphavsvilkår).isNull()
    }

    /**
     * Søknad 1: Barn1
     *
     * Søknad 2: Barn 1 og Barn2
     */
    @Test
    internal fun `hentEllerOpprettVilkår skal opprette vilkår for nytt barn og gjennbruke eksisterende vilkår for barn`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())

        val barn1 = SøknadUtil.barnMedBarnepass()
        val førstegangsbehandling = testoppsettService.lagre(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        val førsteSøknad = lagreSøknad(førstegangsbehandling, listOf(barn1))
        val barnPåFørsteSøknad = barnService.opprettBarn(søknadBarnTilBehandlingBarn(førsteSøknad.barn, førstegangsbehandling.id))
        opprettVilkårsvurderinger(førsteSøknad, førstegangsbehandling, barnPåFørsteSøknad)

        val barn2 = SøknadUtil.barnMedBarnepass()
        val revurdering = testoppsettService.lagre(behandling(fagsak))
        val revurderingSøknad = lagreSøknad(revurdering, listOf(barn1, barn2))
        gjennbrukDataRevurderingService.gjenbrukData(revurdering, førstegangsbehandling.id)
        barnService.opprettBarn(søknadBarnTilBehandlingBarn(revurderingSøknad.barn.filter { it.ident == barn2.ident.verdi }, revurdering.id))

        val barnRevurdering = barnService.finnBarnPåBehandling(revurdering.id)
        vilkårService.hentEllerOpprettVilkår(revurdering.id, HovedregelMetadata(barnRevurdering, revurdering))

        val vilkårFørstegangsbehandling = vilkårRepository.findByBehandlingId(førstegangsbehandling.id).single()
        assertThat(vilkårFørstegangsbehandling.barnId).isEqualTo(barnPåFørsteSøknad.single().id)

        val vilkårRevurdering = vilkårRepository.findByBehandlingId(revurdering.id)
        assertThat(vilkårRevurdering.size).isEqualTo(2)

        val barn1Id = barnRevurdering.single { it.ident == barn1.ident.verdi }.id
        val vilkårBarn1 = vilkårRevurdering.single { it.barnId == barn1Id }

        assertThat(vilkårFørstegangsbehandling.id).isNotEqualTo(vilkårBarn1.id)
        assertThat(vilkårFørstegangsbehandling.behandlingId).isNotEqualTo(vilkårBarn1.behandlingId)
        assertThat(vilkårFørstegangsbehandling.sporbar.opprettetTid).isNotEqualTo(vilkårBarn1.sporbar.opprettetTid)
        assertThat(vilkårFørstegangsbehandling.sporbar.endret.endretTid).isNotEqualTo(vilkårBarn1.sporbar.endret.endretTid)
        assertThat(vilkårFørstegangsbehandling.barnId).isNotEqualTo(vilkårBarn1.barnId)
        assertThat(vilkårFørstegangsbehandling.barnId).isEqualTo(barnPåFørsteSøknad.first().id)
        assertThat(vilkårFørstegangsbehandling.opphavsvilkår).isNull()
        assertThat(vilkårBarn1.opphavsvilkår)
            .isEqualTo(Opphavsvilkår(førstegangsbehandling.id, vilkårFørstegangsbehandling.sporbar.endret.endretTid))

        assertThat(vilkårFørstegangsbehandling).usingRecursiveComparison()
            .ignoringFields("id", "sporbar", "behandlingId", "barnId", "opphavsvilkår")
            .isEqualTo(vilkårBarn1)

        val barn2Id = barnRevurdering.single { it.ident == barn2.ident.verdi }.id
        val vilkårBarn2 = vilkårRevurdering.single { it.barnId == barn2Id }

        assertThat(vilkårBarn2.barnId).isEqualTo(barn2Id)
        assertThat(vilkårBarn2.behandlingId).isEqualTo(revurdering.id)
        assertThat(vilkårBarn2.opphavsvilkår).isNull()
    }

    private fun opprettVilkårsvurderinger(
        søknadskjema: SøknadBarnetilsyn,
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

    private fun lagreSøknad(
        behandling: Behandling,
        barn: List<BarnMedBarnepass> = listOf(SøknadUtil.barnMedBarnepass()),
    ): SøknadBarnetilsyn {
        søknadService.lagreSøknad(behandling.id, lagJournalpost(), SøknadUtil.søknadskjemaBarnetilsyn(barnMedBarnepass = barn))
        return søknadService.hentSøknadBarnetilsyn(behandling.id)!!
    }
}
