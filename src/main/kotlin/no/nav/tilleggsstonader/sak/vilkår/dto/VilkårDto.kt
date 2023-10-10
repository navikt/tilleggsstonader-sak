package no.nav.tilleggsstonader.sak.vilkår.dto

import no.nav.tilleggsstonader.sak.vilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.regler.SvarId
import java.time.LocalDateTime
import java.util.UUID

data class VilkårDto(
    val id: UUID,
    val behandlingId: UUID,
    val resultat: Vilkårsresultat,
    val vilkårType: VilkårType,
    val barnId: UUID? = null,
    val endretAv: String,
    val endretTid: LocalDateTime,
    val delvilkårsett: List<DelvilkårDto> = emptyList(),
    val opphavsvilkår: OpphavsvilkårDto?,
)

data class OpphavsvilkårDto(
    val behandlingId: UUID,
    val endretTid: LocalDateTime,
)

data class OppdaterVilkårDto(val id: UUID, val behandlingId: UUID)

data class SvarPåVilkårDto(
    val id: UUID,
    val behandlingId: UUID,
    val delvilkårsett: List<DelvilkårDto>,
)

data class GjenbrukVilkårDto(val behandlingId: UUID, val kopierBehandlingId: UUID)

data class DelvilkårDto(
    val resultat: Vilkårsresultat,
    val vurderinger: List<VurderingDto>,
) {

    /**
     * @return regelId for første svaret som er hovedregeln på delvilkåret
     */
    fun hovedregel() = this.vurderinger.first().regelId
}

data class VurderingDto(
    val regelId: RegelId,
    val svar: SvarId? = null,
    val begrunnelse: String? = null,
)

fun Vurdering.tilDto() = VurderingDto(this.regelId, this.svar, this.begrunnelse)

fun Delvilkår.tilDto() = DelvilkårDto(this.resultat, this.vurderinger.map { it.tilDto() })

fun Vilkår.tilDto() =
    VilkårDto(
        id = this.id,
        behandlingId = this.behandlingId,
        resultat = this.resultat,
        vilkårType = this.type,
        barnId = this.barnId,
        endretAv = this.sporbar.endret.endretAv,
        endretTid = this.sporbar.endret.endretTid,
        delvilkårsett = this.delvilkårsett
            .filter { it.resultat != Vilkårsresultat.IKKE_AKTUELL }
            .map { it.tilDto() },
        opphavsvilkår = this.opphavsvilkår?.let { OpphavsvilkårDto(it.behandlingId, it.vurderingstidspunkt) },
    )

fun DelvilkårDto.svarTilDomene() = this.vurderinger.map { it.tilDomene() }
fun VurderingDto.tilDomene() = Vurdering(this.regelId, this.svar, this.begrunnelse)
