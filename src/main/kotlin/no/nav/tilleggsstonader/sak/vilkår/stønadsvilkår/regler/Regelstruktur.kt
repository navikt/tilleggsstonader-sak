package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.TypeVilkårFakta

typealias RegelstrukturDto = Map<RegelId, RegelInfo>

data class RegelInfo(
    val erHovedregel: Boolean,
    // Liste med alle regler som må nullstilles dersom regelen endrer svar.
    val reglerSomMåNullstilles: List<RegelId>,
    val svaralternativer: List<SvarAlternativ>,
)

/**
 * plasseringIHierarki: Beskriver maks antall regler som kan komme før dette spørsmålet
 * hovedregel: RegelId-en til tilhørende hovedregel, slik at vi ikke nullstiller svar som ikek henger sammen.
 */
data class Nullstillingsinfo(
    val plasseringIHierarki: Int,
    val hovedregel: RegelId,
)

data class SvarAlternativ(
    val svarId: SvarId,
    val nesteRegelId: RegelId? = null,
    val begrunnelseType: BegrunnelseType,
    val tilhørendeFaktaType: TypeVilkårFakta? = null,
)
