package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.util.FileUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDagligReiseOffentligTransportDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.LagreDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.SlettVilkårRequestDto
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
    val behandling = behandling(fagsak = fagsak, steg = StegType.VILKÅR)

    val svarOffentligTransport =
        mapOf(
            RegelId.AVSTAND_OVER_SEKS_KM to SvarOgBegrunnelseDto(svar = SvarId.JA),
            RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT to SvarOgBegrunnelseDto(svar = SvarId.JA),
        )

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettBehandlingMedFagsak(behandling)
        opprettOgTilordneOppgaveForBehandling(behandling.id)
    }

    @Test
    fun `skal kunne lagre, endre og slette vilkår for daglig reise - offentlig transport`() {
        val nyttVilkår =
            LagreDagligReiseDto(
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 1, 31),
                svar = svarOffentligTransport,
                fakta = faktaOffentligTransport(),
            )

        val resultat = kall.vilkårDagligReise.opprettVilkår(behandling.id, nyttVilkår)

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

        val resultatOppdatert = kall.vilkårDagligReise.oppdaterVilkår(oppdatertVilkår, resultat.id, behandling.id)

        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(resultat.status).isEqualTo(VilkårStatus.NY)
        assertLagretVilkår(oppdatertVilkår, resultatOppdatert)

        val resultatSlettet =
            kall.vilkårDagligReise.slettVilkår(
                behandling.id,
                vilkårId = resultatOppdatert.id,
                dto = SlettVilkårRequestDto(),
            )

        assertThat(resultatSlettet.slettetPermanent).isTrue
        assertThat(resultatSlettet.vilkår.slettetKommentar).isNull()

        val hentedeVilkår = kall.vilkårDagligReise.hentVilkår(behandling.id)
        assertThat(hentedeVilkår).isEmpty()
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

        val resultat = kall.vilkårDagligReise.opprettVilkår(behandling.id, nyttVilkår)

        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
        assertThat(resultat.fakta).isNull()
    }

    @Test
    fun `skal hente alle regler som tilhører daglig reise`() {
        val resultat = kall.vilkårDagligReise.regler()

        FileUtil.assertFileJsonIsEqual("vilkår/regelstruktur/DAGLIG_REISE.json", resultat)
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
