package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.hentReglerDagligReise
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.oppdaterVilkårDagligReise
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.opprettVilkårDagligReise
import no.nav.tilleggsstonader.sak.util.FileUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDagligReiseOffentligTransportDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.LagreDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.VilkårDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DagligReiseVilkårControllerTest : IntegrationTest() {
    val fagsak = fagsak(stønadstype = Stønadstype.DAGLIG_REISE_TSO)
    val behandling = behandling(fagsak)

    val svarOffentligTransport =
        mapOf(
            RegelId.AVSTAND_OVER_SEKS_KM to SvarOgBegrunnelseDto(svar = SvarId.JA),
            RegelId.KAN_BRUKER_REISE_MED_OFFENTLIG_TRANSPORT to SvarOgBegrunnelseDto(svar = SvarId.JA),
        )

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettBehandlingMedFagsak(behandling)
    }

    @Test
    fun `skal kunne lagre og endre vilkår for daglig reise - offentlig transport`() {
        val nyttVilkår =
            LagreDagligReiseDto(
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 1, 31),
                svar = svarOffentligTransport,
                fakta = faktaOffentligTransport(),
            )

        val resultat = opprettVilkårDagligReise(nyttVilkår, behandling.id)

        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(resultat.status).isEqualTo(VilkårStatus.NY)
        assertLagretVilkår(nyttVilkår, resultat)

        val oppdatertVilkår =
            nyttVilkår.copy(
                tom = LocalDate.of(2025, 2, 28),
                fakta =
                    faktaOffentligTransport(
                        reisedagerPerUke = 4,
                    ),
            )

        val resultatOppdatert = oppdaterVilkårDagligReise(oppdatertVilkår, resultat.id, behandling.id)

        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(resultat.status).isEqualTo(VilkårStatus.NY)
        assertLagretVilkår(oppdatertVilkår, resultatOppdatert)
    }

    @Test
    fun `skal kunne lagre ned et vilkår uten fakta om vilkår ikke er oppfylt`() {
        val svarAvstandIkkeOppfylt =
            mapOf(
                RegelId.AVSTAND_OVER_SEKS_KM to SvarOgBegrunnelseDto(svar = SvarId.NEI),
                RegelId.UNNTAK_SEKS_KM to SvarOgBegrunnelseDto(svar = SvarId.NEI, "Begrunnelse"),
            )

        val nyttVilkår =
            LagreDagligReiseDto(
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 1, 31),
                svar = svarAvstandIkkeOppfylt,
                fakta = null,
            )

        val resultat = opprettVilkårDagligReise(nyttVilkår, behandling.id)

        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
        assertThat(resultat.fakta).isNull()
    }

    @Test
    fun `skal hente alle regler som tilhører daglig reise`() {
        val resultat = hentReglerDagligReise()

        FileUtil.assertFileJsonIsEqual("vilkår/regelstruktur/DAGLIG_REISE_OFFENTLIG_TRANSPORT.json", resultat)
    }

    private fun faktaOffentligTransport(
        reisedagerPerUke: Int = 5,
        prisEnkelbillett: Int? = 40,
        prisSyvdagersbillett: Int? = null,
        prisTrettidagersbillett: Int? = 800,
    ) = FaktaDagligReiseOffentligTransportDto(
        reisedagerPerUke = reisedagerPerUke,
        prisEnkelbillett = prisEnkelbillett,
        prisSyvdagersbillett = prisSyvdagersbillett,
        prisTrettidagersbillett = prisTrettidagersbillett,
    )

    private fun assertLagretVilkår(
        lagreVilkårRequest: LagreDagligReiseDto,
        resultat: VilkårDagligReiseDto,
    ) {
        assertThat(resultat.fom).isEqualTo(lagreVilkårRequest.fom)
        assertThat(resultat.tom).isEqualTo(lagreVilkårRequest.tom)
        assertThat(resultat.fakta).isEqualTo(lagreVilkårRequest.fakta)
        assertThat(resultat.delvilkårsett).hasSize(1)

        assertAlleSvarHarFåttVurdering(delvilkår = resultat.delvilkårsett, svar = lagreVilkårRequest.svar)
    }

    private fun assertAlleSvarHarFåttVurdering(
        delvilkår: List<DelvilkårDto>,
        svar: Map<RegelId, SvarOgBegrunnelseDto>,
    ) {
        val brukteRegelIder = delvilkår.flatMap { it.vurderinger.map { vurdering -> vurdering.regelId } }.toSet()

        assertThat(brukteRegelIder).hasSize(svar.size)
    }
}
