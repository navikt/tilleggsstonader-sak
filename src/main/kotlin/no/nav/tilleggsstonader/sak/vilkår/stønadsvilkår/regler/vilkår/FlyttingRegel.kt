package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.TypeVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.BegrunnelseType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.NesteRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Resultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.jaNeiSvarRegel

class FlyttingRegel :
    Vilkårsregel(
        vilkårType = VilkårType.FLYTTING,
        regler =
            setOf(
                SKAL_BRUKE_FLYTTEBYRÅ,
                SKAL_KJØRE_SELV,
            ),
    ) {
    companion object {
        private val SKAL_BRUKE_FLYTTEBYRÅ =
            RegelSteg(
                regelId = RegelId.SKAL_BRUKE_FLYTTEBYRÅ,
                erHovedregel = true,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa =
                            SluttSvarRegel(
                                resultat = Resultat.OPPFYLT,
                                begrunnelseType = BegrunnelseType.VALGFRI,
                                tilhørendeFaktaType = TypeVilkårFakta.FLYTTING_FLYTTEBYRÅ,
                            ),
                        hvisNei =
                            NesteRegel(
                                regelId = RegelId.SKAL_KJØRE_SELV,
                                begrunnelseType = BegrunnelseType.VALGFRI,
                            ),
                    ),
            )

        private val SKAL_KJØRE_SELV =
            RegelSteg(
                regelId = RegelId.SKAL_KJØRE_SELV,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa =
                            SluttSvarRegel(
                                resultat = Resultat.OPPFYLT,
                                begrunnelseType = BegrunnelseType.VALGFRI,
                                tilhørendeFaktaType = TypeVilkårFakta.FLYTTING_KJØRE_SELV,
                            ),
                        hvisNei =
                            SluttSvarRegel(
                                resultat = Resultat.IKKE_OPPFYLT,
                                begrunnelseType = BegrunnelseType.PÅKREVD,
                                tilhørendeFaktaType = TypeVilkårFakta.FLYTTING_UBESTEMT,
                            ),
                    ),
            )
    }
}
