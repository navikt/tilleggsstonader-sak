package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.ProblemDetailUtil.catchProblemDetailException
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingerMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiodeNy
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiodeResponse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperioderResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.exchange
import java.util.UUID

class VilkårperiodeControllerTest : IntegrationTest() {

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(onBehalfOfToken())
    }

    @Test
    fun `skal kunne lagre og hente vilkarperioder for AAP`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())

        opprettVilkårperiode(
            LagreVilkårperiodeNy(
                type = MålgruppeType.AAP,
                fom = osloDateNow(),
                tom = osloDateNow(),
                faktaOgVurderinger = faktaOgVurderingerMålgruppeDto(),
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

        val originalLagreRequest = LagreVilkårperiodeNy(
            type = MålgruppeType.AAP,
            fom = osloDateNow(),
            tom = osloDateNow(),
            faktaOgVurderinger = faktaOgVurderingerMålgruppeDto(),
            behandlingId = behandling.id,
        )

        val response = opprettVilkårperiode(originalLagreRequest)

        val nyTom = osloDateNow()

        oppdaterVilkårperiode(
            lagreVilkårperiode = originalLagreRequest.copy(behandlingId = behandling.id, tom = nyTom),
            vilkårperiodeId = response.periode!!.id,
        )

        val lagredeVilkårperioder = hentVilkårperioder(behandling)

        assertThat(lagredeVilkårperioder.målgrupper.single().tom).isEqualTo(nyTom)
    }

    @Test
    fun `skal feile hvis man ikke sender inn lik behandlingId som det er på vilkårperioden`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val behandlingForAnnenFagsak = testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("1")))).let {
            testoppsettService.lagre(behandling(it))
        }

        val response = opprettVilkårperiode(
            LagreVilkårperiodeNy(
                type = MålgruppeType.AAP,
                fom = osloDateNow(),
                tom = osloDateNow(),
                faktaOgVurderinger = faktaOgVurderingerMålgruppeDto(),
                behandlingId = behandling.id,
            ),
        )
        val exception = catchProblemDetailException {
            slettVilkårperiode(
                vilkårperiodeId = response.periode!!.id,
                SlettVikårperiode(behandlingForAnnenFagsak.id, "test"),
            )
        }
        assertThat(exception.detail.detail).contains("BehandlingId er ikke lik")
    }

    @Nested
    inner class OppdateringAvGrunnlag {

        @Test
        fun `må ha saksbehandlerrolle for å kunne oppdatere grunnlag`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
            headers.setBearerAuth(onBehalfOfToken(rolleConfig.veilederRolle))
            val exception = catchProblemDetailException {
                oppdaterGrunnlag(behandling.id)
            }
            assertThat(exception.detail.detail)
                .contains("Mangler nødvendig saksbehandlerrolle for å utføre handlingen")
        }
    }

    private fun hentVilkårperioder(behandling: Behandling) =
        restTemplate.exchange<VilkårperioderResponse>(
            localhost("api/vilkarperiode/behandling/${behandling.id}"),
            HttpMethod.GET,
            HttpEntity(null, headers),
        ).body!!.vilkårperioder

    private fun opprettVilkårperiode(
        lagreVilkårperiode: LagreVilkårperiodeNy,
    ) = restTemplate.exchange<LagreVilkårperiodeResponse>(
        localhost("api/vilkarperiode/v2"),
        HttpMethod.POST,
        HttpEntity(lagreVilkårperiode, headers),
    ).body!!

    private fun oppdaterVilkårperiode(
        lagreVilkårperiode: LagreVilkårperiodeNy,
        vilkårperiodeId: UUID,
    ) = restTemplate.exchange<LagreVilkårperiodeResponse>(
        localhost("api/vilkarperiode/v2/$vilkårperiodeId"),
        HttpMethod.POST,
        HttpEntity(lagreVilkårperiode, headers),
    ).body!!

    private fun slettVilkårperiode(
        vilkårperiodeId: UUID,
        slettVikårperiode: SlettVikårperiode,
    ) = restTemplate.exchange<LagreVilkårperiodeResponse>(
        localhost("api/vilkarperiode/$vilkårperiodeId"),
        HttpMethod.DELETE,
        HttpEntity(slettVikårperiode, headers),
    ).body!!

    private fun oppdaterGrunnlag(
        behandlingId: BehandlingId,
    ) = restTemplate.exchange<LagreVilkårperiodeResponse>(
        localhost("api/vilkarperiode/behandling/$behandlingId/oppdater-grunnlag"),
        HttpMethod.POST,
        HttpEntity(null, headers),
    ).body!!
}
