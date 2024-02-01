package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.util.ProblemDetailUtil.catchProblemDetailException
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.delvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperioderDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.exchange
import java.time.LocalDate
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
            LagreVilkårperiode(
                type = MålgruppeType.AAP,
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                delvilkår = delvilkårMålgruppeDto(),
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
    fun `skal feile hvis man ikke sender inn lik behandlingId som det er på vilkårperioden`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        val behandlingForAnnenFagsak = testoppsettService.lagreFagsak(fagsak(setOf(PersonIdent("1")))).let {
            testoppsettService.lagre(behandling(it))
        }

        val periode = opprettVilkårperiode(
            LagreVilkårperiode(
                type = MålgruppeType.AAP,
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                delvilkår = delvilkårMålgruppeDto(),
                behandlingId = behandling.id,
            ),
        )
        val exception = catchProblemDetailException {
            slettVilkårperiode(
                vilkårperiodeId = periode.id,
                SlettVikårperiode(behandlingForAnnenFagsak.id, "test"),
            )
        }
        assertThat(exception.detail.detail).contains("BehandlingId er ikke lik")
    }

    private fun hentVilkårperioder(behandling: Behandling) =
        restTemplate.exchange<VilkårperioderDto>(
            localhost("api/vilkarperiode/behandling/${behandling.id}"),
            HttpMethod.GET,
            HttpEntity(null, headers),
        ).body!!

    private fun opprettVilkårperiode(
        lagreVilkårperiode: LagreVilkårperiode,
    ) = restTemplate.exchange<VilkårperiodeDto>(
        localhost("api/vilkarperiode"),
        HttpMethod.POST,
        HttpEntity(lagreVilkårperiode, headers),
    ).body!!

    private fun slettVilkårperiode(
        vilkårperiodeId: UUID,
        slettVikårperiode: SlettVikårperiode,
    ) = restTemplate.exchange<VilkårperiodeDto>(
        localhost("api/vilkarperiode/$vilkårperiodeId"),
        HttpMethod.DELETE,
        HttpEntity(slettVikårperiode, headers),
    ).body!!
}
