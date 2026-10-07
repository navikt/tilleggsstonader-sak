package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.LagreVilkårFlyttingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.SlettVilkårFlyttingRequestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.SlettVilkårFlyttingResultatDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.VilkårFlyttingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelstrukturDto

class VilkårFlyttingKall(
    private val testklient: Testklient,
) {
    fun hentVilkår(behandlingId: BehandlingId): List<VilkårFlyttingDto> = apiRespons.hentVilkår(behandlingId).expectOkWithBody()

    fun opprettVilkår(
        behandlingId: BehandlingId,
        dto: LagreVilkårFlyttingDto,
    ): VilkårFlyttingDto = apiRespons.opprettVilkår(behandlingId, dto).expectOkWithBody()

    fun oppdaterVilkår(
        lagreVilkår: LagreVilkårFlyttingDto,
        vilkårId: VilkårId,
        behandlingId: BehandlingId,
    ): VilkårFlyttingDto = apiRespons.oppdaterVilkår(lagreVilkår, vilkårId, behandlingId).expectOkWithBody()

    fun slettVilkår(
        behandlingId: BehandlingId,
        vilkårId: VilkårId,
        dto: SlettVilkårFlyttingRequestDto,
    ): SlettVilkårFlyttingResultatDto = apiRespons.slettVilkår(behandlingId, vilkårId, dto).expectOkWithBody()

    fun regler(): RegelstrukturDto = apiRespons.regler().expectOkWithBody()

    val apiRespons = VilkårFlyttingApi()

    inner class VilkårFlyttingApi {
        fun hentVilkår(behandlingId: BehandlingId) = testklient.get("/api/vilkar/flytting/$behandlingId")

        fun opprettVilkår(
            behandlingId: BehandlingId,
            dto: LagreVilkårFlyttingDto,
        ) = testklient.post("/api/vilkar/flytting/$behandlingId", dto)

        fun oppdaterVilkår(
            lagreVilkår: LagreVilkårFlyttingDto,
            vilkårId: VilkårId,
            behandlingId: BehandlingId,
        ) = testklient.put("/api/vilkar/flytting/$behandlingId/$vilkårId", lagreVilkår)

        fun slettVilkår(
            behandlingId: BehandlingId,
            vilkårId: VilkårId,
            dto: SlettVilkårFlyttingRequestDto,
        ) = testklient.delete("/api/vilkar/flytting/$behandlingId/$vilkårId", dto)

        fun regler() = testklient.get("/api/vilkar/flytting/regler")
    }
}
