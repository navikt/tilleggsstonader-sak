package no.nav.tilleggsstonader.sak.vilkår

import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext.SYSTEM_FORKORTELSE
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadBarnetilsyn
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.SøknadUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.søknadBarnTilBehandlingBarn
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.Opphavsvilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårperiodeDomainUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.regler.vilkår.EksempelRegel
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
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
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    @Test
    internal fun `kopierVilkårsettTilNyBehandling - skal kopiere vilkår til ny behandling`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        val revurdering = testoppsettService.lagre(behandling(fagsak))
        val søknadskjema = lagreSøknad(behandling)
        val barnPåFørsteSøknad = barnRepository.insertAll(søknadBarnTilBehandlingBarn(søknadskjema.barn, behandling.id))
        val barnPåRevurdering = barnRepository.insertAll(søknadBarnTilBehandlingBarn(søknadskjema.barn, revurdering.id))

        val vilkårForBehandling = opprettVilkårsvurderinger(søknadskjema, behandling, barnPåFørsteSøknad).first()
        val metadata = HovedregelMetadata(
            barnPåRevurdering,
            mockk(),
        )
        vilkårService.kopierVilkårsettTilNyBehandling(
            behandling.id,
            revurdering.id,
            metadata,
            Stønadstype.BARNETILSYN,
        )

        val vilkårForRevurdering = vilkårRepository.findByBehandlingId(revurdering.id).first()

        assertThat(vilkårForBehandling.id).isNotEqualTo(vilkårForRevurdering.id)
        assertThat(vilkårForBehandling.behandlingId).isNotEqualTo(vilkårForRevurdering.behandlingId)
        assertThat(vilkårForBehandling.sporbar.opprettetTid).isNotEqualTo(vilkårForRevurdering.sporbar.opprettetTid)
        assertThat(vilkårForBehandling.sporbar.endret.endretTid).isNotEqualTo(vilkårForRevurdering.sporbar.endret.endretTid)
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
        val behandling = testoppsettService.lagre(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        assertThat(catchThrowable { vilkårService.oppdaterGrunnlagsdataOgHentEllerOpprettVurderinger(behandling.id) })
            .hasMessage("Kan ikke laste inn nye grunnlagsdata for behandling med status ${behandling.status}")
    }

    @Test
    internal fun `kopierVilkårsettTilNyBehandling - skal kaste feil hvis det ikke finnes noen vurderinger`() {
        val tidligereBehandlingId = UUID.randomUUID()
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val revurdering = testoppsettService.lagre(behandling(fagsak))
        val metadata = HovedregelMetadata(
            emptyList(),
            mockk(),
        )
        assertThat(
            catchThrowable {
                vilkårService.kopierVilkårsettTilNyBehandling(
                    tidligereBehandlingId,
                    revurdering.id,
                    metadata,
                    Stønadstype.BARNETILSYN,
                )
            },
        )
            .hasMessage("Tidligere behandling=$tidligereBehandlingId har ikke noen vilkår")
    }

    @Nested
    inner class SlettVilkårperiode {

        @Test
        fun `skal ikke kunne slette kommentar hvis behandlingen ikke er under behandling`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(behandling(status = BehandlingStatus.FERDIGSTILT))
            val målgruppe = målgruppe(
                behandlingId = behandling.id,
                kilde = KildeVilkårsperiode.MANUELL,
            )
            val periode = vilkårperiodeRepository.insert(målgruppe)

            assertThatThrownBy {
                vilkårService.slettVilkårperiode(behandling.id, periode.id, SlettVikårperiode("kommentar"))
            }.hasMessageContaining("Kan ikke slette vilkårperiode når behandling er låst for videre redigering")
        }

        @Test
        fun `skal ikke kunne slette kommentar hvis man mangler kommentar`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(behandling())
            val målgruppe = målgruppe(
                behandlingId = behandling.id,
                kilde = KildeVilkårsperiode.MANUELL,
            )
            val periode = vilkårperiodeRepository.insert(målgruppe)

            assertThatThrownBy {
                vilkårService.slettVilkårperiode(behandling.id, periode.id, SlettVikårperiode("    "))
            }.hasMessageContaining("Mangler kommentar")
        }

        @Test
        fun `skal ikke kunne slette kommentar hvis kilden er system`() {
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(behandling())
            val målgruppe = målgruppe(
                behandlingId = behandling.id,
                kilde = KildeVilkårsperiode.SYSTEM,
            )
            val periode = vilkårperiodeRepository.insert(målgruppe)

            assertThatThrownBy {
                vilkårService.slettVilkårperiode(behandling.id, periode.id, SlettVikårperiode("kommentar"))
            }.hasMessageContaining("Kan ikke slette når kilde=")
        }

        @Test
        fun `skal kunne slette kommentar som er manuellt opprettet`() {
            val saksbehandler = "saksbehandlerX"
            val behandling =
                testoppsettService.opprettBehandlingMedFagsak(behandling())
            val målgruppe = målgruppe(
                behandlingId = behandling.id,
                kilde = KildeVilkårsperiode.MANUELL,
            )
            val periode = vilkårperiodeRepository.insert(målgruppe)

            assertThat(periode.sporbar.endret.endretAv).isEqualTo(SYSTEM_FORKORTELSE)

            testWithBrukerContext(saksbehandler) {
                vilkårService.slettVilkårperiode(behandling.id, periode.id, SlettVikårperiode("kommentar"))
            }

            val oppdatertPeriode = vilkårperiodeRepository.findByIdOrThrow(periode.id)
            assertThat(oppdatertPeriode.resultat).isEqualTo(ResultatVilkårperiode.SLETTET)
            assertThat(oppdatertPeriode.sporbar.endret.endretAv).isEqualTo(saksbehandler)
        }
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
                type = VilkårType.EKSEMPEL,
                behandlingId = behandling.id,
                barnId = barn.first().id,
                delvilkår = delvilkårsett,
            ),
        )
        return vilkårRepository.insertAll(vilkårsett)
    }

    private fun lagreSøknad(
        behandling: Behandling,
    ): SøknadBarnetilsyn {
        søknadService.lagreSøknad(behandling.id, "123", SøknadUtil.søknadskjemaBarnetilsyn())
        return søknadService.hentSøknadBarnetilsyn(behandling.id)!!
    }
}
