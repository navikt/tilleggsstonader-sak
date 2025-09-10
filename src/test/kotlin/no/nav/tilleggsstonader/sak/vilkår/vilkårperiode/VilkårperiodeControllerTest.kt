package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.kall.hentVilkårperioder
import no.nav.tilleggsstonader.sak.kall.oppdaterGrunnlagKall
import no.nav.tilleggsstonader.sak.kall.oppdaterVikårperiode
import no.nav.tilleggsstonader.sak.kall.opprettVilkårperiode
import no.nav.tilleggsstonader.sak.kall.slettVilkårperiodeKall
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingerMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårperiodeControllerTest : IntegrationTest() {
    @Test
    fun `skal kunne lagre og hente vilkarperioder for AAP`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        opprettVilkårperiode(
            LagreVilkårperiode(
                type = MålgruppeType.AAP,
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                faktaOgSvar = faktaOgVurderingerMålgruppeDto(),
                behandlingId = behandling.id,
            ),
        )

        val hentedeVilkårperioder = hentVilkårperioder(behandling)

        assertThat(hentedeVilkårperioder.målgrupper).hasSize(1)
        assertThat(hentedeVilkårperioder.aktiviteter).isEmpty()

        val målgruppe = hentedeVilkårperioder.målgrupper[0]
        assertThat(målgruppe.type).isEqualTo(MålgruppeType.AAP)
    }

    @Test
    fun `skal kunne oppdatere eksisterende aktivitet`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        val originalLagreRequest =
            LagreVilkårperiode(
                type = MålgruppeType.AAP,
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                faktaOgSvar = faktaOgVurderingerMålgruppeDto(),
                behandlingId = behandling.id,
            )

        val response = opprettVilkårperiode(originalLagreRequest)

        val nyTom = LocalDate.now()

        oppdaterVikårperiode(
            lagreVilkårperiode = originalLagreRequest.copy(behandlingId = behandling.id, tom = nyTom),
            vilkårperiodeId = response.periode!!.id,
        )

        val lagredeVilkårperioder = hentVilkårperioder(behandling)

        assertThat(lagredeVilkårperioder.målgrupper.single().tom).isEqualTo(nyTom)
    }

    @Test
    fun `skal feile hvis man ikke sender inn lik behandlingId som det er på vilkårperioden`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val behandlingForAnnenFagsak =
            testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("1")))).let {
                testoppsettService.lagre(behandling(it))
            }

        val response =
            opprettVilkårperiode(
                LagreVilkårperiode(
                    type = MålgruppeType.AAP,
                    fom = LocalDate.now(),
                    tom = LocalDate.now(),
                    faktaOgSvar = faktaOgVurderingerMålgruppeDto(),
                    behandlingId = behandling.id,
                ),
            )

        slettVilkårperiodeKall(
            vilkårperiodeId = response.periode!!.id,
            SlettVikårperiode(behandlingForAnnenFagsak.id, "test"),
        ).expectStatus()
            .is5xxServerError
            .expectBody()
            .jsonPath("$.detail")
            .value<String> {
                assertThat(it).startsWith("BehandlingId er ikke lik")
            }
    }

    @Nested
    inner class OppdateringAvGrunnlag {
        @Test
        fun `må ha saksbehandlerrolle for å kunne oppdatere grunnlag`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

            medBrukercontext(rolle = rolleConfig.veilederRolle) {
                oppdaterGrunnlagKall(behandling.id)
                    .expectStatus()
                    .isForbidden
                    .expectBody()
                    .jsonPath("$.detail")
                    .value<String> {
                        assertThat(it).startsWith("Mangler nødvendig saksbehandlerrolle for å utføre handlingen")
                    }
            }
        }
    }
}
