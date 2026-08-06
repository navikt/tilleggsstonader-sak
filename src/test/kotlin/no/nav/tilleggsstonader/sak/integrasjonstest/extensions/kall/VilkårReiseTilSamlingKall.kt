package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelstrukturDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.LagreVilkårReiseTilSamlingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto.VilkårReiseTilSamlingDto

class VilkårReiseTilSamlingKall(
    private val testklient: Testklient,
) {
    fun hentVilkår(behandlingId: BehandlingId): List<VilkårReiseTilSamlingDto> = apiRespons.hentVilkår(behandlingId).expectOkWithBody()

    fun opprettVilkår(
        behandlingId: BehandlingId,
        dto: LagreVilkårReiseTilSamlingDto,
    ): VilkårReiseTilSamlingDto = apiRespons.opprettVilkår(behandlingId, dto).expectOkWithBody()

    fun regler(): RegelstrukturDto = apiRespons.regler().expectOkWithBody()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = VilkårReiseTilSamlingApi()

    inner class VilkårReiseTilSamlingApi {
        fun hentVilkår(behandlingId: BehandlingId) = testklient.get("/api/vilkar/reise-til-samling/$behandlingId")

        fun opprettVilkår(
            behandlingId: BehandlingId,
            dto: LagreVilkårReiseTilSamlingDto,
        ) = testklient.post("/api/vilkar/reise-til-samling/$behandlingId", dto)

        fun regler() = testklient.get("/api/vilkar/reise-til-samling/regler")
    }
}
