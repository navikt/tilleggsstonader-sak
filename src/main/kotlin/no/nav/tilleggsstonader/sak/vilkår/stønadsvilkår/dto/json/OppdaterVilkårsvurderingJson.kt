package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.json

import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.DelvilkårDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.finnOppfølgingsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.hovedreglerPassBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import java.util.UUID

data class OppdaterVilkårsvurderingJson(
    val id: UUID,
    val behandlingId: UUID,
    val vurdering: List<VurderingJson>,
)

data class VurderingJson(
    val regel: RegelId,
    val svar: SvarId?,
    val begrunnelse: String?,
)

fun VurderingJson.tilDomene(): Vurdering = Vurdering(
    regelId = this.regel,
    svar = this.svar,
    begrunnelse = this.begrunnelse,
)

fun List<VurderingJson>.tilDelvilkårDtoer(): List<DelvilkårDto> {
    return hovedreglerPassBarn().flatMap { regelId ->
        val hovedregelsvar = this.find { it.regel == regelId }!!.tilDomene()

        val svar = hovedregelsvar.svar
        brukerfeilHvis(svar == null) { "Alle hovedregler må ha et svar." }

        val oppfølgingsregel = finnOppfølgingsregel(regelId, svar)

        if (oppfølgingsregel == RegelId.SLUTT_NODE) {
            listOf(Delvilkår(vurderinger = listOf(hovedregelsvar)).tilDto())
        } else {
            val oppfølgingssvar = this.find { it.regel == oppfølgingsregel }?.tilDomene()

            brukerfeilHvis(oppfølgingssvar?.svar == null) { "$oppfølgingsregel skal ha et svar." }

            listOf(Delvilkår(vurderinger = listOf(hovedregelsvar, oppfølgingssvar!!)).tilDto())
        }
    }
}