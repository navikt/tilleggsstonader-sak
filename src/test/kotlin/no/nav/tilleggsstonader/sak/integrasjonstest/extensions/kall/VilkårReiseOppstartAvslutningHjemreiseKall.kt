package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.AktivitetMedReiserDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.LagreVilkårMidlertidigBostedDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.LagreVilkårROAHDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.LagreVilkårROAHReiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.RegelstrukturROAHDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.SlettVilkårRequestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.SlettVilkårResultatDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.VilkårMidlertidigBostedDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.VilkårROAHReiseDto

class VilkårReiseOppstartAvslutningHjemreiseKall(
    private val testklient: Testklient,
) {
    fun hentVilkår(behandlingId: BehandlingId): List<AktivitetMedReiserDto> =
        apiRespons.hentVilkår(behandlingId).expectOkWithBody<List<AktivitetMedReiserDto>>()

    fun opprettVilkår(
        behandlingId: BehandlingId,
        dto: LagreVilkårROAHReiseDto,
    ): VilkårROAHReiseDto = apiRespons.opprettVilkår(behandlingId, dto).expectOkWithBody<VilkårROAHReiseDto>()

    fun oppdaterVilkår(
        lagreVilkår: LagreVilkårROAHReiseDto,
        vilkårId: VilkårId,
        behandlingId: BehandlingId,
    ): VilkårROAHReiseDto = apiRespons.oppdaterVilkår(lagreVilkår, vilkårId, behandlingId).expectOkWithBody()

    fun opprettParaplyvilkår(
        behandlingId: BehandlingId,
        dto: LagreVilkårMidlertidigBostedDto,
    ): VilkårMidlertidigBostedDto = apiRespons.opprettVilkår(behandlingId, dto).expectOkWithBody()

    fun oppdaterParaplyvilkår(
        lagreVilkår: LagreVilkårMidlertidigBostedDto,
        vilkårId: VilkårId,
        behandlingId: BehandlingId,
    ): VilkårMidlertidigBostedDto = apiRespons.oppdaterVilkår(lagreVilkår, vilkårId, behandlingId).expectOkWithBody()

    fun slettVilkår(
        behandlingId: BehandlingId,
        vilkårId: VilkårId,
        dto: SlettVilkårRequestDto,
    ): SlettVilkårResultatDto = apiRespons.slettVilkår(behandlingId, vilkårId, dto).expectOkWithBody()

    fun regler(): RegelstrukturROAHDto = apiRespons.regler().expectOkWithBody()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = VilkårReiseOppstartAvslutningHjemreiseApi()

    inner class VilkårReiseOppstartAvslutningHjemreiseApi {
        fun hentVilkår(behandlingId: BehandlingId) = testklient.get("/api/vilkar/reise-oppstart-avslutning-hjemreise/$behandlingId")

        fun opprettVilkår(
            behandlingId: BehandlingId,
            dto: LagreVilkårROAHDto,
        ) = testklient.post("/api/vilkar/reise-oppstart-avslutning-hjemreise/$behandlingId", dto)

        fun oppdaterVilkår(
            lagreVilkår: LagreVilkårROAHDto,
            vilkårId: VilkårId,
            behandlingId: BehandlingId,
        ) = testklient.put("/api/vilkar/reise-oppstart-avslutning-hjemreise/$behandlingId/$vilkårId", lagreVilkår)

        fun slettVilkår(
            behandlingId: BehandlingId,
            vilkårId: VilkårId,
            dto: SlettVilkårRequestDto,
        ) = testklient.delete("/api/vilkar/reise-oppstart-avslutning-hjemreise/$behandlingId/$vilkårId", dto)

        fun regler() = testklient.get("/api/vilkar/reise-oppstart-avslutning-hjemreise/regler")
    }
}
