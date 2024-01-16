package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.PdlClientConfig.Companion.barn2Fnr
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.PdlClientConfig.Companion.barnFnr
import no.nav.tilleggsstonader.sak.util.ProblemDetailUtil.catchProblemDetailException
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeDomainUtil.delvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.OpprettVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.Vilkårperioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.web.client.exchange
import java.time.LocalDate
import java.util.UUID

class VilkårControllerTest : IntegrationTest() {

    @Autowired
    lateinit var barnRepository: BarnRepository

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(onBehalfOfToken())
    }

    @Test
    fun `skal kunne lagre og hente vilkarperioder for AAP`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(behandling())
        barnRepository.insert(behandlingBarn(behandlingId = behandling.id, personIdent = barnFnr))
        barnRepository.insert(behandlingBarn(behandlingId = behandling.id, personIdent = barn2Fnr))

        opprettVilkårperiode(
            behandling,
            OpprettVilkårperiode(
                type = MålgruppeType.AAP,
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                delvilkår = delvilkårMålgruppeDto(),
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
            behandling,
            OpprettVilkårperiode(
                type = MålgruppeType.AAP,
                fom = LocalDate.now(),
                tom = LocalDate.now(),
                delvilkår = delvilkårMålgruppeDto(),
            ),
        )
        val exception = catchProblemDetailException {
            slettVilkårperiode(
                behandlingId = behandlingForAnnenFagsak.id,
                vilkårperiodeId = periode.id,
                SlettVikårperiode("test"),
            )
        }
        assertThat(exception.detail.detail).contains("BehandlingId er ikke lik")
    }

    private fun hentVilkårperioder(behandling: Behandling) =
        restTemplate.exchange<Vilkårperioder>(
            localhost("api/vilkar/${behandling.id}/periode"),
            HttpMethod.GET,
            HttpEntity(null, headers),
        ).body!!

    private fun opprettVilkårperiode(
        behandling: Behandling,
        opprettVilkårperiode: OpprettVilkårperiode,
    ) = restTemplate.exchange<VilkårperiodeDto>(
        localhost("api/vilkar/${behandling.id}/periode"),
        HttpMethod.POST,
        HttpEntity(opprettVilkårperiode, headers),
    ).body!!

    private fun slettVilkårperiode(
        behandlingId: UUID,
        vilkårperiodeId: UUID,
        slettVikårperiode: SlettVikårperiode,
    ) = restTemplate.exchange<VilkårperiodeDto>(
        localhost("api/vilkar/$behandlingId/periode/$vilkårperiodeId"),
        HttpMethod.DELETE,
        HttpEntity(slettVikårperiode, headers),
    ).body!!
}
