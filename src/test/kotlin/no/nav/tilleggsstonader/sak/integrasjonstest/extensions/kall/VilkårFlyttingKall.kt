package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.integrasjonstest.Testklient
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.LagreVilkårFlyttingDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.dto.VilkårFlyttingDto

class VilkårFlyttingKall(
    private val testklient: Testklient,
) {
    fun hentVilkår(behandlingId: BehandlingId): List<VilkårFlyttingDto> =
        testklient.get("/api/vilkar/flytting/$behandlingId").expectOkWithBody()

    fun opprettVilkår(
        behandlingId: BehandlingId,
        dto: LagreVilkårFlyttingDto,
    ): VilkårFlyttingDto = testklient.post("/api/vilkar/flytting/$behandlingId", dto).expectOkWithBody()
}
