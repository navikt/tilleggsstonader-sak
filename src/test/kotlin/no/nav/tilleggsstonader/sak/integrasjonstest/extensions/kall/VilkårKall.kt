package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.LagreDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.SlettVilkårRequestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.SlettVilkårResultatDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.VilkårDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelstrukturDto
import org.springframework.http.HttpMethod

class VilkårKall(
    private val test: IntegrationTest,
) {
    fun dagligReise(behandlingId: BehandlingId): List<VilkårDagligReiseDto> = dagligReiseResponse(behandlingId).expectOkWithBody()

    fun dagligReiseResponse(behandlingId: BehandlingId) =
        with(test) {
            webTestClient
                .get()
                .uri("/api/vilkar/daglig-reise/$behandlingId")
                .medOnBehalfOfToken()
                .exchange()
        }

    fun opprettDagligReise(
        lagreVilkår: LagreDagligReiseDto,
        behandlingId: BehandlingId,
    ): VilkårDagligReiseDto = opprettDagligReiseResponse(lagreVilkår, behandlingId).expectOkWithBody()

    fun opprettDagligReiseResponse(
        lagreVilkår: LagreDagligReiseDto,
        behandlingId: BehandlingId,
    ) = with(test) {
        webTestClient
            .post()
            .uri("/api/vilkar/daglig-reise/$behandlingId")
            .bodyValue(lagreVilkår)
            .medOnBehalfOfToken()
            .exchange()
    }

    fun oppdaterDagligReise(
        lagreVilkår: LagreDagligReiseDto,
        vilkårId: VilkårId,
        behandlingId: BehandlingId,
    ): VilkårDagligReiseDto = oppdaterDagligReiseResponse(lagreVilkår, vilkårId, behandlingId).expectOkWithBody()

    fun oppdaterDagligReiseResponse(
        lagreVilkår: LagreDagligReiseDto,
        vilkårId: VilkårId,
        behandlingId: BehandlingId,
    ) = with(test) {
        webTestClient
            .put()
            .uri("/api/vilkar/daglig-reise/$behandlingId/$vilkårId")
            .bodyValue(lagreVilkår)
            .medOnBehalfOfToken()
            .exchange()
    }

    fun slettDagligReise(
        slettVilkår: SlettVilkårRequestDto,
        vilkårId: VilkårId,
        behandlingId: BehandlingId,
    ): SlettVilkårResultatDto = slettDagligReiseResponse(slettVilkår, vilkårId, behandlingId).expectOkWithBody()

    fun slettDagligReiseResponse(
        slettVilkår: SlettVilkårRequestDto,
        vilkårId: VilkårId,
        behandlingId: BehandlingId,
    ) = with(test) {
        webTestClient
            .method(HttpMethod.DELETE)
            .uri("/api/vilkar/daglig-reise/$behandlingId/$vilkårId")
            .bodyValue(slettVilkår)
            .medOnBehalfOfToken()
            .exchange()
    }

    fun regler(): RegelstrukturDto = reglerResponse().expectOkWithBody()

    fun reglerResponse() =
        with(test) {
            webTestClient
                .get()
                .uri("/api/vilkar/daglig-reise/regler")
                .medOnBehalfOfToken()
                .exchange()
        }
}
