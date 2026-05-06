package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.kontrakter.søknad.RammevedtakDto
import no.nav.tilleggsstonader.sak.brev.kjørelistebrev.GenererKjørelistebrevDto
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.privatbil.ReisevurderingPrivatBilDto
import no.nav.tilleggsstonader.sak.privatbil.UkeVurderingDto
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.EndreAvklartDagRequest
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

    fun fullførKjørelisteBehandling(behandlingId: BehandlingId) = apiRespons.fullførKjørelisteBehandling(behandlingId).expectStatus().isOk

    fun hentOppsummertBeregning(behandlingId: BehandlingId) =
        apiRespons.hentOppsummertBeregning(behandlingId).expectOkWithBody<PrivatBilOppsummertBeregningDto>()

    fun genererKjørelisteVedtaksbrev(behandlingId: BehandlingId) =
        apiRespons.genererKjørelisteVedtaksbrev(behandlingId).expectOkWithBody<ByteArray>()

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

        fun fullførKjørelisteBehandling(behandlingId: BehandlingId) =
            with(testklient.testkontekst) {
                restTestClient
                    .post()
                    .uri("/api/behandling/$behandlingId/fullfør-kjørelistebehandling")
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
                    .body(GenererKjørelistebrevDto(begrunnelse = null))
                    .medOnBehalfOfToken()
                    .exchange()
            }
    }
}
