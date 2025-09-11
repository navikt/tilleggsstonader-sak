package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiodeResponse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperioderResponse
import org.springframework.http.HttpMethod
import org.springframework.test.web.reactive.server.expectBody
import java.util.UUID

fun IntegrationTest.hentVilkårperioder(behandling: Behandling) =
    webTestClient
        .get()
        .uri("/api/vilkarperiode/behandling/${behandling.id}")
        .medOnBehalfOfToken()
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<VilkårperioderResponse>()
        .returnResult()
        .responseBody!!
        .vilkårperioder

fun IntegrationTest.opprettVilkårperiode(lagreVilkårperiode: LagreVilkårperiode) =
    webTestClient
        .post()
        .uri("/api/vilkarperiode/v2")
        .bodyValue(lagreVilkårperiode)
        .medOnBehalfOfToken()
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<LagreVilkårperiodeResponse>()
        .returnResult()
        .responseBody!!

fun IntegrationTest.oppdaterVikårperiode(
    lagreVilkårperiode: LagreVilkårperiode,
    vilkårperiodeId: UUID,
) = webTestClient
    .post()
    .uri("/api/vilkarperiode/v2/$vilkårperiodeId")
    .bodyValue(lagreVilkårperiode)
    .medOnBehalfOfToken()
    .exchange()
    .expectStatus()
    .isOk
    .expectBody<LagreVilkårperiodeResponse>()
    .returnResult()
    .responseBody!!

fun IntegrationTest.slettVilkårperiodeKall(
    vilkårperiodeId: UUID,
    slettVikårperiode: SlettVikårperiode,
) = webTestClient
    .method(HttpMethod.DELETE) // Delete's ikke spesifisert skal ha body, så er en "hack"
    .uri("/api/vilkarperiode/$vilkårperiodeId")
    .bodyValue(slettVikårperiode)
    .medOnBehalfOfToken()
    .exchange()

fun IntegrationTest.slettVilkårperiode(
    vilkårperiodeId: UUID,
    slettVikårperiode: SlettVikårperiode,
) = slettVilkårperiodeKall(vilkårperiodeId, slettVikårperiode)
    .expectStatus()
    .isOk
    .expectBody<LagreVilkårperiodeResponse>()
    .returnResult()
    .responseBody!!

fun IntegrationTest.oppdaterGrunnlagKall(behandlingId: BehandlingId) =
    webTestClient
        .post()
        .uri("/api/vilkarperiode/behandling/$behandlingId/oppdater-grunnlag")
        .medOnBehalfOfToken()
        .exchange()
