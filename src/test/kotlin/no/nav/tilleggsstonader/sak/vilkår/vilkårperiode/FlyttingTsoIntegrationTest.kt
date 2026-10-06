package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegController
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarOgBegrunnelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaFlyttebyråDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FaktaKjøreSelvDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.FlyttebyråTilbudDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.LagreVilkårFlyttingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class FlyttingTsoIntegrationTest : CleanDatabaseIntegrationTest() {
    private val ident = "12345678910"

    @Test
    fun `skal fullføre vilkår med tilbud fra to flyttebyråer`() {
        val behandling = opprettBehandlingPåVilkårssteg()
        val dto =
            LagreVilkårFlyttingDto(
                fom = 1 januar 2026,
                tom = 31 januar 2026,
                svar = mapOf(RegelId.HVORDAN_SKAL_BRUKER_FLYTTE to SvarOgBegrunnelseDto(SvarId.FLYTTEBYRÅ)),
                fakta =
                    FaktaFlyttebyråDto(
                        tilbud1 = FlyttebyråTilbudDto(navn = "Flyttebyrå A", pris = 10000),
                        tilbud2 = FlyttebyråTilbudDto(navn = "Flyttebyrå B", pris = 12000),
                        adresse = "Flytteveien 1",
                    ),
            )

        val lagret = kall.vilkårFlytting.opprettVilkår(behandling.id, dto)
        assertThat(lagret.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(lagret.fakta).isEqualTo(dto.fakta)
        assertThat(fullførVilkårssteg(behandling.id)).isEqualTo(StegType.BEREGNE_YTELSE)
        assertInngangsvilkårFortsattLagret(behandling.id)
    }

    @Test
    fun `skal fullføre vilkår med egen kjøring og kostnader`() {
        val behandling = opprettBehandlingPåVilkårssteg()
        val dto =
            LagreVilkårFlyttingDto(
                fom = 1 januar 2026,
                tom = 31 januar 2026,
                svar =
                    mapOf(
                        RegelId.HVORDAN_SKAL_BRUKER_FLYTTE to SvarOgBegrunnelseDto(SvarId.FLYTTER_SELV),
                    ),
                fakta =
                    FaktaKjøreSelvDto(
                        avstandEnVei = 250,
                        henger = 1500,
                        bompenger = 300,
                        ferge = 400,
                        parkering = 100,
                        adresse = "Flytteveien 1",
                    ),
            )

        val lagret = kall.vilkårFlytting.opprettVilkår(behandling.id, dto)
        assertThat(lagret.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(lagret.fakta).isEqualTo(dto.fakta)
        assertThat(fullførVilkårssteg(behandling.id)).isEqualTo(StegType.BEREGNE_YTELSE)
        assertInngangsvilkårFortsattLagret(behandling.id)
    }

    private fun opprettBehandlingPåVilkårssteg() =
        testoppsettService
            .opprettBehandlingMedFagsak(
                behandling = behandling(),
                stønadstype = Stønadstype.FLYTTING_TSO,
                identer = setOf(PersonIdent(ident = ident)),
            ).also { behandling ->
                opprettOgTilordneOppgaveForBehandling(behandling.id)
                gjennomførBehandlingsløp(
                    ident = ident,
                    behandlingId = behandling.id,
                    tilSteg = StegType.VILKÅR,
                ) {
                    defaultFlyttingTSOTestdata()
                }
            }

    private fun fullførVilkårssteg(behandlingId: BehandlingId) =
        kall.steg
            .ferdigstill(
                behandlingId,
                StegController.FerdigstillStegRequest(steg = StegType.VILKÅR),
            ).nesteSteg

    private fun assertInngangsvilkårFortsattLagret(behandlingId: BehandlingId) {
        val vilkårperioder = kall.vilkårperiode.hentForBehandling(behandlingId).vilkårperioder
        assertThat(vilkårperioder.målgrupper).hasSize(1)
        assertThat(vilkårperioder.aktiviteter).hasSize(1)
    }
}
