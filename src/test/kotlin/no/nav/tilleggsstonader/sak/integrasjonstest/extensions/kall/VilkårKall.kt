package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.LagreDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.VilkårDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelstrukturDto
import org.springframework.test.web.reactive.server.expectBody

fun IntegrationTest.opprettVilkårDagligReise(lagreVilkår: LagreDagligReiseDto, behandlingId: BehandlingId) =
    webTestClient
        .post()
        .uri("/api/vilkar/daglig-reise/$behandlingId")
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
