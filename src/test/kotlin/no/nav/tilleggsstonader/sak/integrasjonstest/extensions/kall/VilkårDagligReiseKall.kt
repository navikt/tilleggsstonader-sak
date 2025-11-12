package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.LagreDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.SlettVilkårRequestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.SlettVilkårResultatDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.VilkårDagligReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelstrukturDto

class VilkårDagligReiseKall(
    private val testklient: Testklient,
) {
    fun hentVilkår(behandlingId: BehandlingId): List<VilkårDagligReiseDto> = apiRespons.hentVilkår(behandlingId).expectOkWithBody()

    fun opprettVilkår(
        behandlingId: BehandlingId,
        dto: LagreDagligReiseDto,
    ): VilkårDagligReiseDto = apiRespons.opprettVilkår(behandlingId, dto).expectOkWithBody()

    fun oppdaterVilkår(
        lagreVilkår: LagreDagligReiseDto,
        vilkårId: VilkårId,
        behandlingId: BehandlingId,
    ): VilkårDagligReiseDto = apiRespons.oppdaterVilkår(lagreVilkår, vilkårId, behandlingId).expectOkWithBody()

    fun slettVilkår(
        behandlingId: BehandlingId,
        vilkårId: VilkårId,
        dto: SlettVilkårRequestDto,
    ): SlettVilkårResultatDto = apiRespons.slettVilkår(behandlingId, vilkårId, dto).expectOkWithBody()

    fun regler(): RegelstrukturDto = apiRespons.regler().expectOkWithBody()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = VilkårDagligReiseApi()

    inner class VilkårDagligReiseApi {
        fun hentVilkår(behandlingId: BehandlingId) = testklient.get("/api/vilkar/daglig-reise/$behandlingId")

        fun opprettVilkår(
            behandlingId: BehandlingId,
            dto: LagreDagligReiseDto,
        ) = testklient.post("/api/vilkar/daglig-reise/$behandlingId", dto)

        fun oppdaterVilkår(
            lagreVilkår: LagreDagligReiseDto,
            vilkårId: VilkårId,
            behandlingId: BehandlingId,
        ) = testklient.put("/api/vilkar/daglig-reise/$behandlingId/$vilkårId", lagreVilkår)

        fun slettVilkår(
            behandlingId: BehandlingId,
            vilkårId: VilkårId,
            dto: SlettVilkårRequestDto,
        ) = testklient.delete("/api/vilkar/daglig-reise/$behandlingId/$vilkårId", dto)

        fun regler() = testklient.get("/api/vilkar/daglig-reise/regler")
    }
}
