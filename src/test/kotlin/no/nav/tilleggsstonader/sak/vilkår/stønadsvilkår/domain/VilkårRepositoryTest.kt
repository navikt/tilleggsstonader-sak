package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain

import no.nav.tilleggsstonader.libs.utils.osloNow
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.temporal.ChronoUnit
import java.util.UUID

internal class VilkårRepositoryTest : IntegrationTest() {

    @Autowired
    private lateinit var vilkårRepository: VilkårRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Test
    internal fun findByBehandlingId() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))

        val vurderinger = listOf(Vurdering(RegelId.HAR_ET_NAVN, SvarId.JA, "ja"))
        val vilkår = vilkårRepository.insert(
            vilkår(
                behandlingId = behandling.id,
                resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                type = VilkårType.PASS_BARN,
                delvilkår = listOf(Delvilkår(Vilkårsresultat.OPPFYLT, vurderinger)),
                barnId = null,
                opphavsvilkår = Opphavsvilkår(behandling.id, SporbarUtils.now()),
            ),
        )

        Assertions.assertThat(vilkårRepository.findByBehandlingId(UUID.randomUUID())).isEmpty()
        Assertions.assertThat(vilkårRepository.findByBehandlingId(behandling.id)).containsOnly(vilkår)
    }

    @Test
    internal fun `vilkårsvurdering uten opphavsvilkår`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))
        val vilkår = vilkårRepository.insert(
            vilkår(
                behandlingId = behandling.id,
                resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                type = VilkårType.PASS_BARN,
                opphavsvilkår = null,
            ),
        )
        Assertions.assertThat(vilkårRepository.findByBehandlingId(behandling.id)).containsOnly(vilkår)
    }

    @Test
    internal fun oppdaterEndretTid() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))

        val vilkår = vilkårRepository.insert(
            vilkår(
                behandling.id,
                VilkårType.PASS_BARN,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
            ),
        )
        val nyttTidspunkt = osloNow().minusDays(1).truncatedTo(ChronoUnit.MILLIS)

        vilkårRepository.oppdaterEndretTid(vilkår.id, nyttTidspunkt)

        Assertions.assertThat(vilkårRepository.findByIdOrThrow(vilkår.id).sporbar.endret.endretTid).isEqualTo(
            nyttTidspunkt,
        )
    }

    @Test
    internal fun `setter maskinellt opprettet på vilkår`() {
        val saksbehandler = "C000"
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))

        val vilkår: Vilkår = BrukerContextUtil.testWithBrukerContext(preferredUsername = saksbehandler) {
            vilkårRepository.insert(
                vilkår(behandling.id, VilkårType.PASS_BARN, Vilkårsresultat.IKKE_TATT_STILLING_TIL),
            )
        }
        Assertions.assertThat(vilkår.sporbar.opprettetAv).isEqualTo(saksbehandler)
        Assertions.assertThat(vilkår.sporbar.endret.endretAv).isEqualTo(saksbehandler)

        vilkårRepository.settMaskinelltOpprettet(vilkår.id)
        val oppdatertVilkår = vilkårRepository.findByIdOrThrow(vilkår.id)
        Assertions.assertThat(oppdatertVilkår.sporbar.opprettetAv).isEqualTo(SikkerhetContext.SYSTEM_FORKORTELSE)
        Assertions.assertThat(oppdatertVilkår.sporbar.endret.endretAv).isEqualTo(SikkerhetContext.SYSTEM_FORKORTELSE)
    }
}
