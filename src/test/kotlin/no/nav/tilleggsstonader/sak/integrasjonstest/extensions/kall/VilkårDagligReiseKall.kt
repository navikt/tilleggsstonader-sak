package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.LagreDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.SlettVilkårRequestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.SlettVilkårResultatDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.VilkårDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelstrukturDto
import org.springframework.http.HttpMethod.DELETE

class VilkårDagligReiseKall(
    private val test: IntegrationTest,
) {
    fun hentVilkår(behandlingId: BehandlingId) =
        with(test) {
            webTestClient
                .get()
                .uri("/api/vilkar/daglig-reise/$behandlingId")
                .medOnBehalfOfToken()
                .exchange()
                .expectOkWithBody<List<VilkårDagligReiseDto>>()
        }

    fun opprettVilkår(
        behandlingId: BehandlingId,
        dto: LagreDagligReiseDto,
    ) = with(test) {
        webTestClient
            .post()
            .uri("/api/vilkar/daglig-reise/$behandlingId")
            .bodyValue(dto)
            .medOnBehalfOfToken()
            .exchange()
            .expectOkWithBody<VilkårDagligReiseDto>()
    }

    fun oppdaterVilkår(
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
            .expectOkWithBody<VilkårDagligReiseDto>()
    }

    fun slettVilkår(
        behandlingId: BehandlingId,
        vilkårId: VilkårId,
        dto: SlettVilkårRequestDto,
    ) = with(test) {
        webTestClient
            .method(DELETE)
            .uri("/api/vilkar/daglig-reise/$behandlingId/$vilkårId")
            .bodyValue(dto)
            .medOnBehalfOfToken()
            .exchange()
            .expectOkWithBody<SlettVilkårResultatDto>()
    }

    fun regler() =
        with(test) {
            webTestClient
                .get()
                .uri("/api/vilkar/daglig-reise/regler")
                .medOnBehalfOfToken()
                .exchange()
                .expectOkWithBody<RegelstrukturDto>()
        }
}
