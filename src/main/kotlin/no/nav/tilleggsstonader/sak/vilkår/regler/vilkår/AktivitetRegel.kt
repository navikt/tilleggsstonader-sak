package no.nav.tilleggsstonader.sak.vilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.regelIder

class AktivitetRegel : Vilkårsregel(
    vilkårType = VilkårType.AKTIVITET,
    regler = setOf(ER_AKTIVITET_REGISTRERT),
    hovedregler = regelIder(ER_AKTIVITET_REGISTRERT),
) {
    companion object {

        private val ER_AKTIVITET_REGISTRERT =
            RegelSteg(
                regelId = RegelId.ER_AKTIVITET_REGISTRERT,
                jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                ),
            )
    }
}
