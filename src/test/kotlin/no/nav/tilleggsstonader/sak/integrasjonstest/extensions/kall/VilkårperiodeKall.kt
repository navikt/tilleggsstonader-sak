package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiodeResponse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.SlettVikårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.VilkårperioderResponse
import java.util.UUID

class VilkårperiodeKall(
    private val testklient: Testklient,
) {
    fun hentForBehandling(behandling: Behandling): VilkårperioderResponse = apiRespons.hentForBehandling(behandling).expectOkWithBody()

    fun opprett(lagreVilkårperiode: LagreVilkårperiode): LagreVilkårperiodeResponse =
        apiRespons.opprett(lagreVilkårperiode).expectOkWithBody()

    fun oppdater(
        lagreVilkårperiode: LagreVilkårperiode,
        vilkårperiodeId: UUID,
    ): LagreVilkårperiodeResponse = apiRespons.oppdater(lagreVilkårperiode, vilkårperiodeId).expectOkWithBody()

    fun slett(
        vilkårperiodeId: UUID,
        slettVikårperiode: SlettVikårperiode,
    ): LagreVilkårperiodeResponse = apiRespons.slett(vilkårperiodeId, slettVikårperiode).expectOkWithBody()

    fun oppdaterGrunnlag(behandlingId: BehandlingId) {
        apiRespons
            .oppdaterGrunnlag(behandlingId)
            .expectOkEmpty()
    }

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = VilkårperiodeApi()

    inner class VilkårperiodeApi {
        fun hentForBehandling(behandling: Behandling) = testklient.get("/api/vilkarperiode/behandling/${behandling.id}")

        fun opprett(lagreVilkårperiode: LagreVilkårperiode) = testklient.post("/api/vilkarperiode/v2", lagreVilkårperiode)

        fun oppdater(
            lagreVilkårperiode: LagreVilkårperiode,
            vilkårperiodeId: UUID,
        ) = testklient.post("/api/vilkarperiode/v2/$vilkårperiodeId", lagreVilkårperiode)

        fun slett(
            vilkårperiodeId: UUID,
            slettVikårperiode: SlettVikårperiode,
        ) = testklient.delete("/api/vilkarperiode/$vilkårperiodeId", slettVikårperiode)

        fun oppdaterGrunnlag(behandlingId: BehandlingId) =
            testklient.post("/api/vilkarperiode/behandling/$behandlingId/oppdater-grunnlag", Unit)
    }
}
