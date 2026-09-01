package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelstrukturDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.AktivitetMedReiserDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.LagreVilkårReiseOppstartAvslutningHjemreiseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.SlettVilkårRequestDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.SlettVilkårResultatDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto.VilkårReiseOppstartAvslutningHjemreiseDto

class VilkårReiseOppstartAvslutningHjemreiseKall(
    private val testklient: Testklient,
) {
    fun hentVilkår(behandlingId: BehandlingId): List<AktivitetMedReiserDto> =
        apiRespons.hentVilkår(behandlingId).expectOkWithBody<List<AktivitetMedReiserDto>>()

    fun opprettVilkår(
        behandlingId: BehandlingId,
        dto: LagreVilkårReiseOppstartAvslutningHjemreiseDto,
    ): VilkårReiseOppstartAvslutningHjemreiseDto =
        apiRespons.opprettVilkår(behandlingId, dto).expectOkWithBody<VilkårReiseOppstartAvslutningHjemreiseDto>()

    fun oppdaterVilkår(
        lagreVilkår: LagreVilkårReiseOppstartAvslutningHjemreiseDto,
        vilkårId: VilkårId,
        behandlingId: BehandlingId,
    ): VilkårReiseOppstartAvslutningHjemreiseDto = apiRespons.oppdaterVilkår(lagreVilkår, vilkårId, behandlingId).expectOkWithBody()

    fun slettVilkår(
        behandlingId: BehandlingId,
        vilkårId: VilkårId,
        dto: SlettVilkårRequestDto,
    ): SlettVilkårResultatDto = apiRespons.slettVilkår(behandlingId, vilkårId, dto).expectOkWithBody()

    fun regler(): RegelstrukturDto = apiRespons.regler().expectOkWithBody()

    // Gir tilgang til "rå"-endepunktene slik at tester kan skrive egne assertions på responsen.
    val apiRespons = VilkårReiseOppstartAvslutningHjemreiseApi()

    inner class VilkårReiseOppstartAvslutningHjemreiseApi {
        fun hentVilkår(behandlingId: BehandlingId) = testklient.get("/api/vilkar/reise-oppstart-avslutning-hjemreise/$behandlingId")

        fun opprettVilkår(
            behandlingId: BehandlingId,
            dto: LagreVilkårReiseOppstartAvslutningHjemreiseDto,
        ) = testklient.post("/api/vilkar/reise-oppstart-avslutning-hjemreise/$behandlingId", dto)

        fun oppdaterVilkår(
            lagreVilkår: LagreVilkårReiseOppstartAvslutningHjemreiseDto,
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
