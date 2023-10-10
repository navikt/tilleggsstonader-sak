package no.nav.tilleggsstonader.sak.vilkår.regler

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.domain.Vurdering
import java.time.LocalDate
import java.util.UUID

/**
 * Brukes for å utlede hvilke delvilkår som må besvares
 */
data class HovedregelMetadata(
    val barn: List<BehandlingBarn>,
    val behandling: Behandling,
)

abstract class Vilkårsregel(
    val vilkårType: VilkårType,
    val regler: Map<RegelId, RegelSteg>,
    @JsonIgnore
    val hovedregler: Set<RegelId>,
) {

    open fun initiereDelvilkårsvurdering(
        metadata: HovedregelMetadata,
        resultat: Vilkårsresultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
        barnId: UUID? = null,
    ): List<Delvilkår> {
        return hovedregler.map {
            Delvilkår(
                resultat,
                vurderinger = listOf(Vurdering(it)),
            )
        }
    }

    constructor(vilkårType: VilkårType, regler: Set<RegelSteg>, hovedregler: Set<RegelId>) :
        this(vilkårType, regler.associateBy { it.regelId }, hovedregler)

    fun regel(regelId: RegelId): RegelSteg {
        return regler[regelId] ?: throw Feil("Finner ikke regelId=$regelId for vilkårType=$vilkårType")
    }

    protected fun automatiskVurdertDelvilkår(
        regelId: RegelId,
        svarId: SvarId,
        begrunnelse: String,
    ): Delvilkår {
        return Delvilkår(
            resultat = Vilkårsresultat.AUTOMATISK_OPPFYLT,
            listOf(
                Vurdering(
                    regelId = regelId,
                    svar = svarId,
                    begrunnelse = "Automatisk vurdert (${LocalDate.now().norskFormat()}): $begrunnelse",
                ),
            ),
        )
    }

    protected fun ubesvartDelvilkårsvurdering(regelId: RegelId) = Delvilkår(
        resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
        vurderinger = listOf(
            Vurdering(
                regelId = regelId,
            ),
        ),
    )
}
