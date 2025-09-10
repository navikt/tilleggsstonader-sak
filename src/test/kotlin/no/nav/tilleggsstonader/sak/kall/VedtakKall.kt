package no.nav.tilleggsstonader.sak.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.AvslagBoutgifterDto
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.InnvilgelseBoutgifterRequest
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.OpphørBoutgifterRequest
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.VedtakBoutgifterRequest
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.VedtakBoutgifterResponse
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.AvslagDagligReiseDto
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.VedtakDagligReiseRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.VedtakDagligReiseResponse
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.AvslagLæremidlerDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.OpphørLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtakLæremidlerRequest
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtakLæremidlerResponse
import org.springframework.test.web.reactive.server.expectBody

fun IntegrationTest.lagreVedtak(
    behandlingId: BehandlingId,
    vedtakRequest: VedtakRequest,
    stønadsPath: String,
    resultatPath: String,
) = webTestClient
    .post()
    .uri("/api/vedtak/$stønadsPath/$behandlingId/$resultatPath")
    .bodyValue(vedtakRequest)
    .medOnBehalfOfToken()
    .exchange()
    .expectStatus()
    .isOk

// Tilsyn barn
fun IntegrationTest.hentVedtakTilsynBarnKall(behandlingId: BehandlingId) =
    webTestClient
        .get()
        .uri("/api/vedtak/tilsyn-barn/$behandlingId")
        .medOnBehalfOfToken()
        .exchange()

inline fun <reified T : VedtakTilsynBarnResponse> IntegrationTest.hentVedtakTilsynBarn(behandlingId: BehandlingId) =
    hentVedtakTilsynBarnKall(behandlingId)
        .expectStatus()
        .isOk
        .expectBody<T>()
        .returnResult()
        .responseBody!!

fun IntegrationTest.lagreVedtakTilsynBarn(
    behandlingId: BehandlingId,
    vedtakTilsynBarnRequest: VedtakTilsynBarnRequest,
    resultat: String,
) = lagreVedtak(behandlingId, vedtakTilsynBarnRequest, "tilsyn-barn", resultat)

fun IntegrationTest.innvilgeVedtakTilsynBarn(
    behandlingId: BehandlingId,
    innvilgeVedtakTilsynBarnRequest: InnvilgelseTilsynBarnRequest,
) {
    lagreVedtakTilsynBarn(behandlingId, innvilgeVedtakTilsynBarnRequest, "innvilgelse")
        .expectStatus()
        .isOk
        .expectBody()
        .isEmpty
}

fun IntegrationTest.avslåVedtakTilsynBarn(
    behandlingId: BehandlingId,
    innvilgeVedtakTilsynBarnRequest: AvslagTilsynBarnDto,
) {
    lagreVedtakTilsynBarn(behandlingId, innvilgeVedtakTilsynBarnRequest, "avslag")
        .expectStatus()
        .isOk
        .expectBody()
        .isEmpty
}

fun IntegrationTest.opphørVedtakTilsynBarn(
    behandlingId: BehandlingId,
    innvilgeVedtakTilsynBarnRequest: OpphørTilsynBarnRequest,
) {
    lagreVedtakTilsynBarn(behandlingId, innvilgeVedtakTilsynBarnRequest, "opphor")
        .expectStatus()
        .isOk
        .expectBody()
        .isEmpty
}

// Boutgifter
fun IntegrationTest.hentVedtakBoutgifterKall(behandlingId: BehandlingId) =
    webTestClient
        .get()
        .uri("/api/vedtak/boutgifter/$behandlingId")
        .medOnBehalfOfToken()
        .exchange()

inline fun <reified T : VedtakBoutgifterResponse> IntegrationTest.hentVedtakBoutgifter(behandlingId: BehandlingId) =
    hentVedtakBoutgifterKall(behandlingId)
        .expectStatus()
        .isOk
        .expectBody<T>()
        .returnResult()
        .responseBody!!

fun IntegrationTest.lagreVedtakBoutgifter(
    behandlingId: BehandlingId,
    vedtakBoutgifterRequest: VedtakBoutgifterRequest,
    resultat: String,
) = lagreVedtak(behandlingId, vedtakBoutgifterRequest, "boutgifter", resultat)

fun IntegrationTest.innvilgeVedtakBoutgifter(
    behandlingId: BehandlingId,
    innvilgeVedtakBoutgifterRequest: InnvilgelseBoutgifterRequest,
) {
    lagreVedtakBoutgifter(behandlingId, innvilgeVedtakBoutgifterRequest, "innvilgelse")
        .expectStatus()
        .isOk
        .expectBody()
        .isEmpty
}

fun IntegrationTest.avslåVedtakBoutgifter(
    behandlingId: BehandlingId,
    innvilgeVedtakBoutgifterRequest: AvslagBoutgifterDto,
) {
    lagreVedtakBoutgifter(behandlingId, innvilgeVedtakBoutgifterRequest, "avslag")
        .expectStatus()
        .isOk
        .expectBody()
        .isEmpty
}

fun IntegrationTest.opphørVedtakBoutgifter(
    behandlingId: BehandlingId,
    innvilgeVedtakBoutgifterRequest: OpphørBoutgifterRequest,
) {
    lagreVedtakBoutgifter(behandlingId, innvilgeVedtakBoutgifterRequest, "opphor")
        .expectStatus()
        .isOk
        .expectBody()
        .isEmpty
}

// Læremidler
fun IntegrationTest.hentVedtakLæremidlerKall(behandlingId: BehandlingId) =
    webTestClient
        .get()
        .uri("/api/vedtak/laremidler/$behandlingId")
        .medOnBehalfOfToken()
        .exchange()

inline fun <reified T : VedtakLæremidlerResponse> IntegrationTest.hentVedtakLæremidler(behandlingId: BehandlingId) =
    hentVedtakLæremidlerKall(behandlingId)
        .expectStatus()
        .isOk
        .expectBody<T>()
        .returnResult()
        .responseBody!!

fun IntegrationTest.lagreVedtakLæremidler(
    behandlingId: BehandlingId,
    vedtakLæremidlerRequest: VedtakLæremidlerRequest,
    resultat: String,
) = lagreVedtak(behandlingId, vedtakLæremidlerRequest, "laremidler", resultat)

fun IntegrationTest.innvilgeVedtakLæremidler(
    behandlingId: BehandlingId,
    innvilgeVedtakLæremidlerRequest: InnvilgelseLæremidlerRequest,
) {
    lagreVedtakLæremidler(behandlingId, innvilgeVedtakLæremidlerRequest, "innvilgelse")
        .expectStatus()
        .isOk
        .expectBody()
        .isEmpty
}

fun IntegrationTest.avslåVedtakLæremidler(
    behandlingId: BehandlingId,
    innvilgeVedtakLæremidlerRequest: AvslagLæremidlerDto,
) {
    lagreVedtakLæremidler(behandlingId, innvilgeVedtakLæremidlerRequest, "avslag")
        .expectStatus()
        .isOk
        .expectBody()
        .isEmpty
}

fun IntegrationTest.opphørVedtakLæremidler(
    behandlingId: BehandlingId,
    innvilgeVedtakLæremidlerRequest: OpphørLæremidlerRequest,
) {
    lagreVedtakLæremidler(behandlingId, innvilgeVedtakLæremidlerRequest, "opphor")
        .expectStatus()
        .isOk
        .expectBody()
        .isEmpty
}

// Daglig reise
fun IntegrationTest.hentVedtakDagligReiseKall(behandlingId: BehandlingId) =
    webTestClient
        .get()
        .uri("/api/vedtak/daglig-reise/$behandlingId")
        .medOnBehalfOfToken()
        .exchange()

inline fun <reified T : VedtakDagligReiseResponse> IntegrationTest.hentVedtakDagligReise(behandlingId: BehandlingId) =
    hentVedtakDagligReiseKall(behandlingId)
        .expectStatus()
        .isOk
        .expectBody<T>()
        .returnResult()
        .responseBody!!

fun IntegrationTest.lagreVedtakDagligReise(
    behandlingId: BehandlingId,
    vedtakDagligReiseRequest: VedtakDagligReiseRequest,
    resultat: String,
) = lagreVedtak(behandlingId, vedtakDagligReiseRequest, "daglig-reise", resultat)

fun IntegrationTest.innvilgeVedtakDagligReise(
    behandlingId: BehandlingId,
    innvilgeVedtakDagligReiseRequest: InnvilgelseDagligReiseRequest,
) = lagreVedtakDagligReise(behandlingId, innvilgeVedtakDagligReiseRequest, "innvilgelse")
    .expectStatus()
    .isOk
    .expectBody()
    .isEmpty

fun IntegrationTest.avslåVedtakDagligReise(
    behandlingId: BehandlingId,
    innvilgeVedtakDagligReiseRequest: AvslagDagligReiseDto,
) {
    lagreVedtakDagligReise(behandlingId, innvilgeVedtakDagligReiseRequest, "avslag")
        .expectStatus()
        .isOk
        .expectBody()
        .isEmpty
}

// fun IntegrationTest.opphørVedtakDagligReise(
//    behandlingId: BehandlingId,
//    innvilgeVedtakDagligReiseRequest: OpphørDagligReiseRequest,
// ) {
//    lagreVedtakDagligReise(behandlingId, innvilgeVedtakDagligReiseRequest, "opphor")
//        .expectStatus()
//        .isOk
//        .expectBody()
//        .isEmpty
// }
