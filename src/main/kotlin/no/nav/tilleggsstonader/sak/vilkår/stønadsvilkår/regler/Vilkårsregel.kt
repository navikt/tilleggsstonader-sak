package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vurdering
import java.time.LocalDate

/**
 * Brukes for å utlede hvilke delvilkår som må besvares
 */
data class HovedregelMetadata(
    val barn: List<BehandlingBarn>,
    val behandling: Saksbehandling,
)

abstract class Vilkårsregel(
    val vilkårType: VilkårType,
    val regler: Map<RegelId, RegelSteg>,
) {
    @get:JsonIgnore
    val hovedregler: Set<RegelId> = regler.filter { it.value.erHovedregel }.keys.toSet()

    open fun initiereDelvilkår(
        metadata: HovedregelMetadata,
        resultat: Vilkårsresultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
        barnId: BarnId? = null,
    ): List<Delvilkår> =
        hovedregler.map {
            Delvilkår(
                resultat,
                vurderinger = listOf(Vurdering(it)),
            )
        }

    constructor(vilkårType: VilkårType, regler: Set<RegelSteg>) :
        this(vilkårType, regler.associateBy { it.regelId })

    fun regel(regelId: RegelId): RegelSteg = regler[regelId] ?: throw Feil("Finner ikke regelId=$regelId for vilkårType=$vilkårType")

    protected fun automatiskVurdertDelvilkår(
        regelId: RegelId,
        svarId: SvarId,
        begrunnelse: String,
    ): Delvilkår =
        Delvilkår(
            resultat = Vilkårsresultat.AUTOMATISK_OPPFYLT,
            listOf(
                Vurdering(
                    regelId = regelId,
                    svar = svarId,
                    begrunnelse = "Automatisk vurdert (${LocalDate.now().norskFormat()}): $begrunnelse",
                ),
            ),
        )

    protected fun ubesvartDelvilkår(regelId: RegelId) =
        Delvilkår(
            resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
            vurderinger =
                listOf(
                    Vurdering(
                        regelId = regelId,
                    ),
                ),
        )
}
