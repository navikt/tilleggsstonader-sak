package no.nav.tilleggsstonader.sak.vilkår

import io.mockk.mockk
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.fagsak.Stønadstype
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import no.nav.tilleggsstonader.sak.util.SøknadUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.søknadBarnTilBehandlingBarn
import no.nav.tilleggsstonader.sak.util.vilkårsvurdering
import no.nav.tilleggsstonader.sak.vilkår.domain.Opphavsvilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsvurdering
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårsvurderingRepository
import no.nav.tilleggsstonader.sak.vilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.EksempelRegel
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

internal class VurderingServiceIntegrasjonsTest : IntegrationTest() {

    @Autowired
    lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var vurderingService: VurderingService

    @Autowired
    lateinit var søknadService: SøknadService

    @Autowired
    lateinit var barnRepository: BarnRepository

    @Test
    internal fun `kopierVurderingerTilNyBehandling - skal kopiere vurderinger til ny behandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        val revurdering = behandlingRepository.insert(behandling(fagsak))
        val søknadskjema = lagreSøknad(behandling)
        val barnPåFørsteSøknad = barnRepository.insertAll(søknadBarnTilBehandlingBarn(søknadskjema.barn, behandling.id))
        val barnPåRevurdering = barnRepository.insertAll(søknadBarnTilBehandlingBarn(søknadskjema.barn, revurdering.id))

        val vilkårForBehandling = opprettVilkårsvurderinger(søknadskjema, behandling, barnPåFørsteSøknad).first()
        val metadata = HovedregelMetadata(
            barnPåRevurdering,
            mockk(),
        )
        vurderingService.kopierVurderingerTilNyBehandling(
            behandling.id,
            revurdering.id,
            metadata,
            Stønadstype.BARNETILSYN,
        )

        val vilkårForRevurdering = vilkårsvurderingRepository.findByBehandlingId(revurdering.id).first()

        assertThat(vilkårForBehandling.id).isNotEqualTo(vilkårForRevurdering.id)
        assertThat(vilkårForBehandling.behandlingId).isNotEqualTo(vilkårForRevurdering.behandlingId)
        assertThat(vilkårForBehandling.sporbar.opprettetTid).isNotEqualTo(vilkårForRevurdering.sporbar.opprettetTid)
        assertThat(vilkårForBehandling.sporbar.endret).isEqualTo(vilkårForRevurdering.sporbar.endret)
        assertThat(vilkårForBehandling.barnId).isNotEqualTo(vilkårForRevurdering.barnId)
        assertThat(vilkårForBehandling.barnId).isEqualTo(barnPåFørsteSøknad.first().id)
        assertThat(vilkårForBehandling.opphavsvilkår).isNull()
        assertThat(vilkårForRevurdering.barnId).isEqualTo(barnPåRevurdering.first().id)
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
        val behandling = behandlingRepository.insert(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        assertThat(catchThrowable { vurderingService.oppdaterGrunnlagsdataOgHentEllerOpprettVurderinger(behandling.id) })
            .hasMessage("Kan ikke laste inn nye grunnlagsdata for behandling med status ${behandling.status}")
    }

    @Test
    internal fun `kopierVurderingerTilNyBehandling - skal kaste feil hvis det ikke finnes noen vurderinger`() {
        val tidligereBehandlingId = UUID.randomUUID()
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val revurdering = behandlingRepository.insert(behandling(fagsak))
        val metadata = HovedregelMetadata(
            emptyList(),
            mockk(),
        )
        assertThat(
            catchThrowable {
                vurderingService.kopierVurderingerTilNyBehandling(
                    tidligereBehandlingId,
                    revurdering.id,
                    metadata,
                    Stønadstype.BARNETILSYN,
                )
            },
        )
            .hasMessage("Tidligere behandling=$tidligereBehandlingId har ikke noen vilkår")
    }

    private fun opprettVilkårsvurderinger(
        søknadskjema: SøknadBarnetilsyn,
        behandling: Behandling,
        barn: List<BehandlingBarn>,
    ): List<Vilkårsvurdering> {
        val hovedregelMetadata =
            HovedregelMetadata(
                barn = barn,
                behandling = mockk(),
            )
        val delvilkårsvurdering = EksempelRegel().initiereDelvilkårsvurdering(hovedregelMetadata)
        val vilkårsvurderinger = listOf(
            vilkårsvurdering(
                resultat = Vilkårsresultat.OPPFYLT,
                type = VilkårType.EKSEMPEL,
                behandlingId = behandling.id,
                barnId = barn.first().id,
                delvilkårsvurdering = delvilkårsvurdering,
            ),
        )
        return vilkårsvurderingRepository.insertAll(vilkårsvurderinger)
    }

    private fun lagreSøknad(
        behandling: Behandling,
    ): SøknadBarnetilsyn {
        søknadService.lagreSøknad(behandling.id, "123", SøknadUtil.søknadskjemaBarnetilsyn())
        return søknadService.hentSøknadBarnetilsyn(behandling.id)!!
    }
}
