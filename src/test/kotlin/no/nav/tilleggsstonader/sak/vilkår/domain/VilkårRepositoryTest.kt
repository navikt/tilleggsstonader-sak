package no.nav.tilleggsstonader.sak.vilkår.domain

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.regler.SvarId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
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
                type = VilkårType.EKSEMPEL,
                delvilkår = listOf(Delvilkår(Vilkårsresultat.OPPFYLT, vurderinger)),
                barnId = null,
                opphavsvilkår = Opphavsvilkår(behandling.id, SporbarUtils.now()),
            ),
        )

        assertThat(vilkårRepository.findByBehandlingId(UUID.randomUUID())).isEmpty()
        assertThat(vilkårRepository.findByBehandlingId(behandling.id)).containsOnly(vilkår)
    }

    @Test
    internal fun `vilkårsvurdering uten opphavsvilkår`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))
        val vilkår = vilkårRepository.insert(
            vilkår(
                behandlingId = behandling.id,
                resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                type = VilkårType.EKSEMPEL,
                opphavsvilkår = null,
            ),
        )
        assertThat(vilkårRepository.findByBehandlingId(behandling.id)).containsOnly(vilkår)
    }

    @Test
    internal fun oppdaterEndretTid() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))

        val vilkår = vilkårRepository.insert(
            vilkår(
                behandling.id,
                Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                VilkårType.EKSEMPEL,
            ),
        )
        val nyttTidspunkt = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.MILLIS)

        vilkårRepository.oppdaterEndretTid(vilkår.id, nyttTidspunkt)

        assertThat(vilkårRepository.findByIdOrThrow(vilkår.id).sporbar.endret.endretTid).isEqualTo(
            nyttTidspunkt,
        )
    }

    @Test
    internal fun `setter maskinellt opprettet på vilkår`() {
        val saksbehandler = "C000"
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))

        val vilkår: Vilkår = testWithBrukerContext(preferredUsername = saksbehandler) {
            vilkårRepository.insert(
                vilkår(behandling.id, Vilkårsresultat.IKKE_TATT_STILLING_TIL, VilkårType.EKSEMPEL),
            )
        }
        assertThat(vilkår.sporbar.opprettetAv).isEqualTo(saksbehandler)
        assertThat(vilkår.sporbar.endret.endretAv).isEqualTo(saksbehandler)

        vilkårRepository.settMaskinelltOpprettet(vilkår.id)
        val oppdatertVilkår = vilkårRepository.findByIdOrThrow(vilkår.id)
        assertThat(oppdatertVilkår.sporbar.opprettetAv).isEqualTo(SikkerhetContext.SYSTEM_FORKORTELSE)
        assertThat(oppdatertVilkår.sporbar.endret.endretAv).isEqualTo(SikkerhetContext.SYSTEM_FORKORTELSE)
    }
}
