package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain

import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

internal class VilkårRepositoryTest : CleanDatabaseIntegrationTest() {
    @Autowired
    private lateinit var vilkårRepository: VilkårRepository

    @Test
    fun `midlertidig bostedsperiode med fakta og opphav kan lagres og leses`() {
        val behandling = testoppsettService.lagre(behandling(testoppsettService.lagreFagsak(fagsak())))
        val fakta = FaktaROAHMidlertidigBostedPeriode(VilkårperiodeGlobalId.random(), "Testveien 1", "Testveien 2")
        val lagret =
            vilkårRepository.insert(
                vilkår(
                    behandlingId = behandling.id,
                    type = VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE,
                    fom = 5 januar 2026,
                    tom = 15 januar 2026,
                    utgift = null,
                    fakta = fakta,
                    status = VilkårStatus.UENDRET,
                    opphavsvilkår = Opphavsvilkår(behandling.id, SporbarUtils.now()),
                    delvilkår =
                        listOf(
                            Delvilkår(Vilkårsresultat.OPPFYLT, listOf(Vurdering(RegelId.ROAH_MÅ_BO_MIDLERTIDIG_BORTE, SvarId.JA))),
                        ),
                ),
            )
        val hentet = vilkårRepository.findByIdOrThrow(lagret.id)
        assertThat(hentet).isEqualTo(lagret)
        assertThat(hentet.fakta).isEqualTo(fakta)
        val oppdatert = vilkårRepository.update(hentet.copy(tom = 16 januar 2026, status = VilkårStatus.ENDRET))
        assertThat(vilkårRepository.findByIdOrThrow(lagret.id)).isEqualTo(oppdatert)
        assertThat(oppdatert.opphavsvilkår).isEqualTo(lagret.opphavsvilkår)
    }

    @Test
    internal fun findByBehandlingId() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))

        val vurderinger = listOf(Vurdering(RegelId.HAR_ET_NAVN, SvarId.JA, "ja"))
        val vilkår =
            vilkårRepository.insert(
                vilkår(
                    behandlingId = behandling.id,
                    type = VilkårType.PASS_BARN,
                    resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                    delvilkår = listOf(Delvilkår(Vilkårsresultat.OPPFYLT, vurderinger)),
                    barnId = null,
                    opphavsvilkår = Opphavsvilkår(behandling.id, SporbarUtils.now()),
                ),
            )

        assertThat(vilkårRepository.findByBehandlingId(BehandlingId.random())).isEmpty()
        assertThat(vilkårRepository.findByBehandlingId(behandling.id)).containsOnly(vilkår)
    }

    @Test
    internal fun `vilkårsvurdering uten opphavsvilkår`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))
        val vilkår =
            vilkårRepository.insert(
                vilkår(
                    behandlingId = behandling.id,
                    type = VilkårType.PASS_BARN,
                    resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                ),
            )
        assertThat(vilkårRepository.findByBehandlingId(behandling.id)).containsOnly(vilkår)
    }

    @Test
    internal fun oppdaterEndretTid() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))

        val vilkår =
            vilkårRepository.insert(
                vilkår(behandling.id, VilkårType.PASS_BARN, Vilkårsresultat.IKKE_TATT_STILLING_TIL),
            )
        val nyttTidspunkt = LocalDateTime.now().minusDays(1).truncatedTo(ChronoUnit.MILLIS)

        vilkårRepository.oppdaterEndretTid(vilkår.id, nyttTidspunkt)

        assertThat(
            vilkårRepository
                .findByIdOrThrow(vilkår.id)
                .sporbar.endret.endretTid,
        ).isEqualTo(nyttTidspunkt)
    }

    @Test
    internal fun `setter maskinellt opprettet på vilkår`() {
        val saksbehandler = "C000"
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = testoppsettService.lagre(behandling(fagsak))

        val vilkår: Vilkår =
            testWithBrukerContext(preferredUsername = saksbehandler) {
                vilkårRepository.insert(
                    vilkår(behandling.id, VilkårType.PASS_BARN, Vilkårsresultat.IKKE_TATT_STILLING_TIL),
                )
            }
        assertThat(vilkår.sporbar.opprettetAv).isEqualTo(saksbehandler)
        assertThat(vilkår.sporbar.endret.endretAv).isEqualTo(saksbehandler)

        vilkårRepository.settMaskinelltOpprettet(vilkår.id)
        val oppdatertVilkår = vilkårRepository.findByIdOrThrow(vilkår.id)
        assertThat(oppdatertVilkår.sporbar.opprettetAv).isEqualTo(SikkerhetContext.SYSTEM_FORKORTELSE)
        assertThat(oppdatertVilkår.sporbar.endret.endretAv).isEqualTo(SikkerhetContext.SYSTEM_FORKORTELSE)
    }

    @Nested
    inner class GitVersjon {
        @Test
        fun `skal returnere null hvis det ikke finnes en versjon`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(), opprettGrunnlagsdata = false)
            val vilkår =
                vilkårRepository.insert(
                    vilkår(behandling.id, VilkårType.PASS_BARN, Vilkårsresultat.IKKE_TATT_STILLING_TIL)
                        .copy(gitVersjon = null),
                )
            assertThat(vilkårRepository.findByIdOrThrow(vilkår.id).gitVersjon).isNull()
        }

        @Test
        fun `skal returnere versjon hvis det finnes en versjon`() {
            val gitVersjon = UUID.randomUUID().toString()
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling(), opprettGrunnlagsdata = false)
            val vilkår =
                vilkårRepository.insert(
                    vilkår(behandling.id, VilkårType.PASS_BARN, Vilkårsresultat.IKKE_TATT_STILLING_TIL)
                        .copy(gitVersjon = gitVersjon),
                )
            assertThat(vilkårRepository.findByIdOrThrow(vilkår.id).gitVersjon).isEqualTo(gitVersjon)
        }
    }
}
