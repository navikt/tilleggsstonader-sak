package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.LagreDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.VilkårDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelstrukturDto
import org.springframework.test.web.reactive.server.expectBody

fun IntegrationTest.opprettVilkårDagligReise(lagreVilkår: LagreDagligReise) =
    webTestClient
        .post()
        .uri("/api/vilkar/daglig-reise/opprett")
        .bodyValue(lagreVilkår)
        .medOnBehalfOfToken()
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<VilkårDagligReiseDto>()
        .returnResult()
        .responseBody!!

fun IntegrationTest.hentReglerDagligReise() =
    webTestClient
        .get()
        .uri("/api/vilkar/daglig-reise/regler")
        .medOnBehalfOfToken()
        .exchange()
        .expectStatus()
        .isOk
        .expectBody<RegelstrukturDto>()
        .returnResult()
        .responseBody!!
