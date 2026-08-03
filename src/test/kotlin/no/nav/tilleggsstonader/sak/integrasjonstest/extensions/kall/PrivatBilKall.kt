package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.kontrakter.søknad.RammevedtakDto
import no.nav.tilleggsstonader.sak.brev.kjørelistebrev.GenererKjørelistebrevDto
import no.nav.tilleggsstonader.sak.brev.kjørelistebrev.KjørelistebrevResponseDto
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteId
import no.nav.tilleggsstonader.sak.privatbil.ReisevurderingPrivatBilDto
import no.nav.tilleggsstonader.sak.privatbil.UkeVurderingDto
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.EndreAvklartDagRequest
import no.nav.tilleggsstonader.sak.privatbil.manuellRegistrering.KjørelisteOversiktDto
import no.nav.tilleggsstonader.sak.privatbil.manuellRegistrering.LagreManuellKjørelisteRequest
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.PrivatBilOppsummertBeregningDto
import java.util.UUID

class PrivatBilKall(
    private val testklient: Testklient,
) {
    fun hentRammevedtak(ident: String) = apiRespons.hentRammevedtak(ident).expectOkWithBody<List<RammevedtakDto>>()

    fun hentReisevurderingForBehandling(behandlingId: BehandlingId) =
        apiRespons.hentReisevurderingForBehandling(behandlingId).expectOkWithBody<List<ReisevurderingPrivatBilDto>>()

    fun oppdaterUke(
        behandlingId: BehandlingId,
        avklartUkeId: UUID,
        avklarteDager: List<EndreAvklartDagRequest>,
    ) = apiRespons.oppdaterUke(behandlingId, avklartUkeId, avklarteDager).expectOkWithBody<UkeVurderingDto>()

    fun hentOppsummertBeregning(behandlingId: BehandlingId) =
        apiRespons.hentOppsummertBeregning(behandlingId).expectOkWithBody<PrivatBilOppsummertBeregningDto>()

    fun genererKjørelisteVedtaksbrev(behandlingId: BehandlingId) =
        apiRespons.genererKjørelisteVedtaksbrev(behandlingId).expectOkWithBody<KjørelistebrevResponseDto>()

    fun hentKjørelisteOversikt(behandlingId: BehandlingId) =
        apiRespons.hentKjørelisteOversikt(behandlingId).expectOkWithBody<KjørelisteOversiktDto>()

    fun lagreManuellKjøreliste(
        behandlingId: BehandlingId,
        request: LagreManuellKjørelisteRequest,
    ) = apiRespons.lagreManuellKjøreliste(behandlingId, request).expectOkWithBody<KjørelisteId>()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = PrivatBilApi()

    inner class PrivatBilApi {
        fun hentRammevedtak(ident: String) =
            with(testklient.testkontekst) {
                restTestClient
                    .get()
                    .uri("/api/ekstern/privat-bil/rammevedtak")
                    .medTokenXToken(eksternApplikasjon.soknadApi, ident)
                    .exchange()
            }

        fun hentReisevurderingForBehandling(behandlingId: BehandlingId) =
            with(testklient.testkontekst) {
                restTestClient
                    .get()
                    .uri("/api/kjoreliste/$behandlingId")
                    .medOnBehalfOfToken()
                    .exchange()
            }

        fun oppdaterUke(
            behandlingId: BehandlingId,
            avklartUkeId: UUID,
            avklarteDager: List<EndreAvklartDagRequest>,
        ) = with(testklient.testkontekst) {
            restTestClient
                .put()
                .uri("/api/kjoreliste/$behandlingId/$avklartUkeId")
                .body(avklarteDager)
                .medOnBehalfOfToken()
                .exchange()
        }

        fun hentOppsummertBeregning(behandlingId: BehandlingId) =
            with(testklient.testkontekst) {
                restTestClient
                    .get()
                    .uri("/api/vedtak/daglig-reise/$behandlingId/privat-bil/oppsummer-beregning")
                    .medOnBehalfOfToken()
                    .exchange()
            }

        fun genererKjørelisteVedtaksbrev(behandlingId: BehandlingId) =
            with(testklient.testkontekst) {
                restTestClient
                    .post()
                    .uri("/api/kjorelistebrev/$behandlingId")
                    .body(GenererKjørelistebrevDto(begrunnelse = ""))
                    .medOnBehalfOfToken()
                    .exchange()
            }

        // Endepunkter for manuell registrering av kjøreliste
        fun hentKjørelisteOversikt(behandlingId: BehandlingId) =
            with(testklient.testkontekst) {
                restTestClient
                    .get()
                    .uri("api/kjoreliste/manuell-registrering/$behandlingId")
                    .medOnBehalfOfToken()
                    .exchange()
            }

        fun lagreManuellKjøreliste(
            behandlingId: BehandlingId,
            request: LagreManuellKjørelisteRequest,
        ) = with(testklient.testkontekst) {
            restTestClient
                .post()
                .uri("/api/kjoreliste/manuell-registrering/$behandlingId")
                .body(request)
                .medOnBehalfOfToken()
                .exchange()
        }
    }
}
