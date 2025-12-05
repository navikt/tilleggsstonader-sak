package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall.expectProblemDetail
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.opprettOgTilordneOppgaveForBehandling
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingerMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDate

class VilkårperiodeControllerTest : CleanDatabaseIntegrationTest() {
    @Test
    fun `skal kunne lagre og hente vilkarperioder for AAP`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        opprettOgTilordneOppgaveForBehandling(behandling.id)

        kall.vilkårperiode.opprett(
            LagreVilkårperiode(
                type = MålgruppeType.AAP,
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                faktaOgSvar = faktaOgVurderingerMålgruppeDto(),
                behandlingId = behandling.id,
            ),
        )

        val hentedeVilkårperioder = kall.vilkårperiode.hentForBehandling(behandling.id).vilkårperioder

        assertThat(hentedeVilkårperioder.målgrupper).hasSize(1)
        assertThat(hentedeVilkårperioder.aktiviteter).isEmpty()

        val målgruppe = hentedeVilkårperioder.målgrupper[0]
        assertThat(målgruppe.type).isEqualTo(MålgruppeType.AAP)
    }

    @Test
    fun `skal kunne oppdatere eksisterende aktivitet`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        opprettOgTilordneOppgaveForBehandling(behandling.id)

        val originalLagreRequest =
            LagreVilkårperiode(
                type = MålgruppeType.AAP,
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                faktaOgSvar = faktaOgVurderingerMålgruppeDto(),
                behandlingId = behandling.id,
            )

        val response = kall.vilkårperiode.opprett(originalLagreRequest)

        val nyTom = LocalDate.now()

        kall.vilkårperiode.oppdater(
            lagreVilkårperiode = originalLagreRequest.copy(behandlingId = behandling.id, tom = nyTom),
            vilkårperiodeId = response.periode!!.id,
        )

        val lagredeVilkårperioder = kall.vilkårperiode.hentForBehandling(behandling.id).vilkårperioder

        assertThat(lagredeVilkårperioder.målgrupper.single().tom).isEqualTo(nyTom)
    }

    @Test
    fun `skal feile hvis man ikke sender inn lik behandlingId som det er på vilkårperioden`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        opprettOgTilordneOppgaveForBehandling(behandling.id)
        val behandlingForAnnenFagsak =
            testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("1")))).let {
                testoppsettService.lagre(behandling(it))
            }

        val response =
            kall.vilkårperiode.opprett(
                LagreVilkårperiode(
                    type = MålgruppeType.AAP,
                    fom = LocalDate.now(),
                    tom = LocalDate.now(),
                    faktaOgSvar = faktaOgVurderingerMålgruppeDto(),
                    behandlingId = behandling.id,
                ),
            )

        opprettOgTilordneOppgaveForBehandling(behandlingForAnnenFagsak.id)
        kall.vilkårperiode.apiRespons
            .slett(
                vilkårperiodeId = response.periode!!.id,
                SlettVikårperiode(behandlingForAnnenFagsak.id, "test"),
            ).expectProblemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "BehandlingId er ikke lik")
    }

    @Nested
    inner class OppdateringAvGrunnlag {
        @Test
        fun `må ha saksbehandlerrolle for å kunne oppdatere grunnlag`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            medBrukercontext(roller = listOf(rolleConfig.veilederRolle)) {
                opprettOgTilordneOppgaveForBehandling(behandling.id)
                kall.vilkårperiode.apiRespons
                    .oppdaterGrunnlag(behandling.id)
                    .expectProblemDetail(
                        HttpStatus.FORBIDDEN,
                        "Mangler nødvendig saksbehandlerrolle for å utføre handlingen",
                    )
            }
        }
    }
}
