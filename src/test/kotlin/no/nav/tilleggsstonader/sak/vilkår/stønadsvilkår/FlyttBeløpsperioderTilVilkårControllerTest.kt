package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Opphavsvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

class FlyttBeløpsperioderTilVilkårControllerTest : IntegrationTest() {

    @Autowired
    lateinit var vilkårRepository: VilkårRepository

    @Autowired
    lateinit var barnRepository: BarnRepository

    @Autowired
    lateinit var controller: FlyttBeløpsperioderTilVilkårController

    val fagsak = fagsak()
    val behandling = behandling(fagsak, status = BehandlingStatus.FERDIGSTILT)
    val behandling2 = behandling(fagsak, steg = StegType.VILKÅR, forrigeBehandlingId = behandling.id)
    val barn = behandlingBarn(behandlingId = behandling.id)
    val barn2 = behandlingBarn(behandlingId = behandling2.id)

    val vilkår = vilkår(
        behandlingId = behandling.id,
        type = VilkårType.PASS_BARN,
        resultat = Vilkårsresultat.OPPFYLT,
        barnId = barn.id,
        fom = LocalDate.of(2023, 1, 1),
        tom = LocalDate.of(2024, 12, 31),
        utgift = 1003,
    )

    val vilkår2 = vilkår(
        behandlingId = behandling2.id,
        type = VilkårType.PASS_BARN,
        resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
        barnId = barn2.id,
        fom = null,
        tom = null,
        utgift = null,
        opphavsvilkår = Opphavsvilkår(behandlingId = vilkår.behandlingId, LocalDateTime.now()),
    )

    @BeforeEach
    fun setUp() {
        testoppsettService.lagreFagsak(fagsak)
        testoppsettService.lagre(behandling)
        testoppsettService.lagre(behandling2)
        barnRepository.insert(barn)
        barnRepository.insert(barn2)

        vilkårRepository.insert(vilkår)
        vilkårRepository.insert(vilkår2)
    }

    @Test
    fun `skal oppdatere vilkår med utgift fra vedtak`() {
        testWithBrukerContext {
            controller.oppdaterVilkårSomManglerReferanseTilForrigeBehandling("false")
        }

        with(vilkårRepository.findByIdOrThrow(vilkår2.id)) {
            assertThat(fom).isEqualTo(LocalDate.of(2023, 1, 1))
            assertThat(tom).isEqualTo(LocalDate.of(2024, 12, 31))
            assertThat(utgift).isEqualTo(1003)
            assertThat(delvilkårwrapper).isEqualTo(vilkår.delvilkårwrapper)
            assertThat(resultat).isEqualTo(Vilkårsresultat.OPPFYLT)

            assertThat(opphavsvilkår!!.behandlingId).isEqualTo(vilkår.behandlingId)
            assertThat(opphavsvilkår!!.vurderingstidspunkt).isNotNull()
        }
    }
}
