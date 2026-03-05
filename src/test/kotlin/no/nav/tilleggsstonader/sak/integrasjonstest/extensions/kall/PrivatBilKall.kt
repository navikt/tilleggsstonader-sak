package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.ekstern.stønad.dto.RammevedtakDto
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.privatbil.ReisevurderingPrivatBilDto
import no.nav.tilleggsstonader.sak.privatbil.UkeVurderingDto
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.EndreAvklartDagRequest
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
    }
}
