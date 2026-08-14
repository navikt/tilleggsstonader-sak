package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.util.FileUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.FaktaReiseTilSamlingOffentligTransportDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.FaktaReiseTilSamlingPrivatBilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.FaktaReiseTilSamlingUbestemtDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.LagreVilkårReiseTilSamlingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.SlettVilkårRequestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.VilkårReiseTilSamlingDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ReiseTilSamlingVilkårControllerTest : CleanDatabaseIntegrationTest() {
    val fagsak = fagsak(stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO)
    val behandling = behandling(fagsak = fagsak, steg = StegType.VILKÅR)

    val svarOffentligTransport =
        mapOf(
            RegelId.AVSTAND_OVER_TRETTI_KM to SvarOgBegrunnelseDto(svar = SvarId.JA, begrunnelse = "antall km"),
            RegelId.DOKUMENTERTE_UTGIFTER to SvarOgBegrunnelseDto(svar = SvarId.JA, begrunnelse = "dokumentert"),
            RegelId.DEKKET_AV_ANNET_STIPEND to SvarOgBegrunnelseDto(svar = SvarId.NEI),
            RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT to SvarOgBegrunnelseDto(svar = SvarId.JA),
        )

    val svarPrivatBil =
        mapOf(
            RegelId.AVSTAND_OVER_TRETTI_KM to SvarOgBegrunnelseDto(svar = SvarId.JA, begrunnelse = "antall km"),
            RegelId.DOKUMENTERTE_UTGIFTER to SvarOgBegrunnelseDto(svar = SvarId.JA, begrunnelse = "dokumentert"),
            RegelId.DEKKET_AV_ANNET_STIPEND to SvarOgBegrunnelseDto(svar = SvarId.NEI),
            RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT to SvarOgBegrunnelseDto(svar = SvarId.NEI, begrunnelse = "begrunnelse"),
            RegelId.KAN_REISE_MED_EGEN_BIL to SvarOgBegrunnelseDto(svar = SvarId.JA),
        )

    @BeforeEach
    fun setUp() {
        testoppsettService.opprettBehandlingMedFagsak(behandling)
        opprettOgTilordneOppgaveForBehandling(behandling.id)
    }

    @Test
    fun `skal kunne lagre, endre og slette vilkår for reise til samling - offentlig transport`() {
        val nyttVilkår =
            LagreVilkårReiseTilSamlingDto(
                fom = 1 januar 2025,
                tom = 31 januar 2025,
                adresse = "Samlingsveien 1",
                reiseId = dummyReiseId,
                svar = svarOffentligTransport,
                fakta = faktaOffentligTransport(),
            )

        val resultat = kall.vilkårReiseTilSamling.opprettVilkår(behandling.id, nyttVilkår)

        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(resultat.status).isEqualTo(VilkårStatus.NY)
        assertLagretVilkår(nyttVilkår, resultat)

        val oppdatertVilkår =
            nyttVilkår.copy(
                fakta = faktaOffentligTransport(utgifterOffentligTransport = BigDecimal("200")),
            )

        val resultatOppdatert = kall.vilkårReiseTilSamling.oppdaterVilkår(oppdatertVilkår, resultat.id, behandling.id)

        assertThat(resultatOppdatert.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(resultatOppdatert.status).isEqualTo(VilkårStatus.NY)
        assertLagretVilkår(oppdatertVilkår, resultatOppdatert)

        val resultatSlettet =
            kall.vilkårReiseTilSamling.slettVilkår(
                behandlingId = behandling.id,
                vilkårId = resultatOppdatert.id,
                dto = SlettVilkårRequestDto(),
            )

        assertThat(resultatSlettet.slettetPermanent).isTrue
        assertThat(resultatSlettet.vilkår.slettetKommentar).isNull()

        val hentedeVilkår = kall.vilkårReiseTilSamling.hentVilkår(behandling.id)
        assertThat(hentedeVilkår).isEmpty()
    }

    @Test
    fun `skal kunne lagre, endre og slette vilkår for reise til samling - privat bil`() {
        val nyttVilkår =
            LagreVilkårReiseTilSamlingDto(
                fom = 1 januar 2025,
                tom = 31 januar 2025,
                adresse = "Samlingsveien 1",
                reiseId = dummyReiseId,
                svar = svarPrivatBil,
                fakta = faktaPrivatBil(),
            )

        val resultat = kall.vilkårReiseTilSamling.opprettVilkår(behandling.id, nyttVilkår)

        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(resultat.status).isEqualTo(VilkårStatus.NY)
        assertLagretVilkår(nyttVilkår, resultat)

        val oppdatertVilkår =
            nyttVilkår.copy(
                fakta = faktaPrivatBil(reiseavstand = BigDecimal("50")),
            )

        val resultatOppdatert = kall.vilkårReiseTilSamling.oppdaterVilkår(oppdatertVilkår, resultat.id, behandling.id)

        assertThat(resultatOppdatert.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(resultatOppdatert.status).isEqualTo(VilkårStatus.NY)
        assertLagretVilkår(oppdatertVilkår, resultatOppdatert)

        val resultatSlettet =
            kall.vilkårReiseTilSamling.slettVilkår(
                behandlingId = behandling.id,
                vilkårId = resultatOppdatert.id,
                dto = SlettVilkårRequestDto(),
            )

        assertThat(resultatSlettet.slettetPermanent).isTrue
        assertThat(resultatSlettet.vilkår.slettetKommentar).isNull()

        val hentedeVilkår = kall.vilkårReiseTilSamling.hentVilkår(behandling.id)
        assertThat(hentedeVilkår).isEmpty()
    }

    @Test
    fun `skal kunne lagre ned et vilkår med fakta UBESTEMT om vilkår ikke er oppfylt`() {
        val svarAvstandIkkeOppfylt =
            mapOf(
                RegelId.AVSTAND_OVER_TRETTI_KM to SvarOgBegrunnelseDto(svar = SvarId.NEI, begrunnelse = "antall km"),
            )

        val nyttVilkår =
            LagreVilkårReiseTilSamlingDto(
                fom = 1 januar 2025,
                tom = 31 januar 2025,
                adresse = "Samlingsveien 1",
                reiseId = dummyReiseId,
                svar = svarAvstandIkkeOppfylt,
                fakta = FaktaReiseTilSamlingUbestemtDto,
            )

        val resultat = kall.vilkårReiseTilSamling.opprettVilkår(behandling.id, nyttVilkår)

        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
        assertThat(resultat.reiseId).isEqualTo(dummyReiseId)
        assertThat(resultat.adresse).isEqualTo("Samlingsveien 1")
        assertThat(resultat.fakta).isNotNull
    }

    @Test
    fun `skal hente alle regler som tilhører reise til samling`() {
        val resultat = kall.vilkårReiseTilSamling.regler()

        FileUtil.assertFileJsonIsEqual("vilkår/regelstruktur/REISE_TIL_SAMLING.json", resultat)
    }

    private fun faktaOffentligTransport(utgifterOffentligTransport: BigDecimal = BigDecimal("100")) =
        FaktaReiseTilSamlingOffentligTransportDto(
            utgifterOffentligTransport = utgifterOffentligTransport,
        )

    private fun faktaPrivatBil(reiseavstand: BigDecimal = BigDecimal("35")) =
        FaktaReiseTilSamlingPrivatBilDto(
            reiseavstand = reiseavstand,
        )

    private fun assertLagretVilkår(
        lagreVilkårRequest: LagreVilkårReiseTilSamlingDto,
        resultat: VilkårReiseTilSamlingDto,
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
