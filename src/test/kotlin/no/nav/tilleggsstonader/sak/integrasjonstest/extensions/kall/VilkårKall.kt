package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpprettVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SlettVilkårRequest
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SlettVilkårResponse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarPåVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårsvurderingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregler
import org.springframework.http.HttpMethod.DELETE

class VilkårKall(
    private val test: IntegrationTest,
) {
    fun hentVilkår(behandlingId: BehandlingId): VilkårsvurderingDto =
        with(test) {
            webTestClient
                .get()
                .uri("/api/vilkar/$behandlingId")
                .medOnBehalfOfToken()
                .exchange()
                .expectOkWithBody<VilkårsvurderingDto>()
        }

    fun opprettVilkår(dto: OpprettVilkårDto): VilkårDto =
        with(test) {
            webTestClient
                .post()
                .uri("/api/vilkar/opprett")
                .bodyValue(dto)
                .medOnBehalfOfToken()
                .exchange()
                .expectOkWithBody<VilkårDto>()
        }

    fun oppdaterVilkår(dto: SvarPåVilkårDto): VilkårDto =
        with(test) {
            webTestClient
                .post()
                .uri("/api/vilkar")
                .bodyValue(dto)
                .medOnBehalfOfToken()
                .exchange()
                .expectOkWithBody<VilkårDto>()
        }

    fun slettVilkår(dto: SlettVilkårRequest): SlettVilkårResponse =
        with(test) {
            webTestClient
                .method(DELETE)
                .uri("/api/vilkar")
                .bodyValue(dto)
                .medOnBehalfOfToken()
                .exchange()
                .expectOkWithBody<SlettVilkårResponse>()
        }

    fun regler(): Vilkårsregler =
        with(test) {
            webTestClient
                .get()
                .uri("/api/regler")
                .medOnBehalfOfToken()
                .exchange()
                .expectOkWithBody<Vilkårsregler>()
        }
}
