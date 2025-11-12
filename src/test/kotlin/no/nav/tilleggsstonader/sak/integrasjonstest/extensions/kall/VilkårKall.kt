package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.OpprettVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SlettVilkårRequest
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SlettVilkårResponse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.SvarPåVilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.VilkårsvurderingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregler

class VilkårKall(
    private val testklient: Testklient,
) {
    fun hentVilkår(behandlingId: BehandlingId): VilkårsvurderingDto = apiRespons.hentVilkår(behandlingId).expectOkWithBody()

    fun opprettVilkår(dto: OpprettVilkårDto): VilkårDto = apiRespons.opprettVilkår(dto).expectOkWithBody()

    fun oppdaterVilkår(dto: SvarPåVilkårDto): VilkårDto = apiRespons.oppdaterVilkår(dto).expectOkWithBody()

    fun slettVilkår(dto: SlettVilkårRequest): SlettVilkårResponse = apiRespons.slettVilkår(dto).expectOkWithBody()

    fun regler(): Vilkårsregler = apiRespons.regler().expectOkWithBody()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = VilkårApi()

    inner class VilkårApi {
        fun hentVilkår(behandlingId: BehandlingId) = testklient.get("/api/vilkar/$behandlingId")

        fun opprettVilkår(dto: OpprettVilkårDto) = testklient.post("/api/vilkar/opprett", dto)

        fun oppdaterVilkår(dto: SvarPåVilkårDto) = testklient.post("/api/vilkar", dto)

        fun slettVilkår(dto: SlettVilkårRequest) = testklient.delete("/api/vilkar", dto)

        fun regler() = testklient.get("/api/regler")
    }
}
