package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiodeResponse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperioderResponse
import org.springframework.http.HttpMethod
import java.util.UUID

class VilkårperiodeKall(
    private val test: IntegrationTest,
) {
    fun hentForBehandling(behandling: Behandling): VilkårperioderResponse = hentForBehandlingResponse(behandling).expectOkWithBody()

    fun hentForBehandlingResponse(behandling: Behandling) =
        with(test) {
            webTestClient
                .get()
                .uri("/api/vilkarperiode/behandling/${behandling.id}")
                .medOnBehalfOfToken()
                .exchange()
        }

    fun opprett(lagreVilkårperiode: LagreVilkårperiode): LagreVilkårperiodeResponse = opprettResponse(lagreVilkårperiode).expectOkWithBody()

    fun opprettResponse(lagreVilkårperiode: LagreVilkårperiode) =
        with(test) {
            webTestClient
                .post()
                .uri("/api/vilkarperiode/v2")
                .bodyValue(lagreVilkårperiode)
                .medOnBehalfOfToken()
                .exchange()
        }

    fun oppdater(
        lagreVilkårperiode: LagreVilkårperiode,
        vilkårperiodeId: UUID,
    ): LagreVilkårperiodeResponse = oppdaterResponse(lagreVilkårperiode, vilkårperiodeId).expectOkWithBody()

    fun oppdaterResponse(
        lagreVilkårperiode: LagreVilkårperiode,
        vilkårperiodeId: UUID,
    ) = with(test) {
        webTestClient
            .post()
            .uri("/api/vilkarperiode/v2/$vilkårperiodeId")
            .bodyValue(lagreVilkårperiode)
            .medOnBehalfOfToken()
            .exchange()
    }

    fun slett(
        vilkårperiodeId: UUID,
        slettVikårperiode: SlettVikårperiode,
    ): LagreVilkårperiodeResponse = slettResponse(vilkårperiodeId, slettVikårperiode).expectOkWithBody()

    fun slettResponse(
        vilkårperiodeId: UUID,
        slettVikårperiode: SlettVikårperiode,
    ) = with(test) {
        webTestClient
            .method(HttpMethod.DELETE) // Delete's ikke spesifisert skal ha body, så er en "hack"
            .uri("/api/vilkarperiode/$vilkårperiodeId")
            .bodyValue(slettVikårperiode)
            .medOnBehalfOfToken()
            .exchange()
    }

    fun oppdaterGrunnlag(behandlingId: BehandlingId) = oppdaterGrunnlagResponse(behandlingId).expectOkEmpty()

    fun oppdaterGrunnlagResponse(behandlingId: BehandlingId) =
        with(test) {
            webTestClient
                .post()
                .uri("/api/vilkarperiode/behandling/$behandlingId/oppdater-grunnlag")
                .medOnBehalfOfToken()
                .exchange()
        }
}
