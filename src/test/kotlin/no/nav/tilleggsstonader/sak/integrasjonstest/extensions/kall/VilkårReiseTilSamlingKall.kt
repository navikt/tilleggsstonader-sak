package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelstrukturDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.LagreVilkårReiseTilSamlingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.SlettVilkårRequestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.SlettVilkårResultatDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.VilkårReiseTilSamlingDto

class VilkårReiseTilSamlingKall(
    private val testklient: Testklient,
) {
    fun hentVilkår(behandlingId: BehandlingId): List<VilkårReiseTilSamlingDto> =
        apiRespons.hentVilkår(behandlingId).expectOkWithBody<List<VilkårReiseTilSamlingDto>>()

    fun opprettVilkår(
        behandlingId: BehandlingId,
        dto: LagreVilkårReiseTilSamlingDto,
    ): VilkårReiseTilSamlingDto = apiRespons.opprettVilkår(behandlingId, dto).expectOkWithBody<VilkårReiseTilSamlingDto>()

    fun oppdaterVilkår(
        lagreVilkår: LagreVilkårReiseTilSamlingDto,
        vilkårId: VilkårId,
        behandlingId: BehandlingId,
    ): VilkårReiseTilSamlingDto = apiRespons.oppdaterVilkår(lagreVilkår, vilkårId, behandlingId).expectOkWithBody()

    fun slettVilkår(
        behandlingId: BehandlingId,
        vilkårId: VilkårId,
        dto: SlettVilkårRequestDto,
    ): SlettVilkårResultatDto = apiRespons.slettVilkår(behandlingId, vilkårId, dto).expectOkWithBody()

    fun regler(): RegelstrukturDto = apiRespons.regler().expectOkWithBody()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = VilkårReiseTilSamlingApi()

    inner class VilkårReiseTilSamlingApi {
        fun hentVilkår(behandlingId: BehandlingId) = testklient.get("/api/vilkar/reise-til-samling/$behandlingId")

        fun opprettVilkår(
            behandlingId: BehandlingId,
            dto: LagreVilkårReiseTilSamlingDto,
        ) = testklient.post("/api/vilkar/reise-til-samling/$behandlingId", dto)

        fun oppdaterVilkår(
            lagreVilkår: LagreVilkårReiseTilSamlingDto,
            vilkårId: VilkårId,
            behandlingId: BehandlingId,
        ) = testklient.put("/api/vilkar/reise-til-samling/$behandlingId/$vilkårId", lagreVilkår)

        fun slettVilkår(
            behandlingId: BehandlingId,
            vilkårId: VilkårId,
            dto: SlettVilkårRequestDto,
        ) = testklient.delete("/api/vilkar/reise-til-samling/$behandlingId/$vilkårId", dto)

        fun regler() = testklient.get("/api/vilkar/reise-til-samling/regler")
    }
}
