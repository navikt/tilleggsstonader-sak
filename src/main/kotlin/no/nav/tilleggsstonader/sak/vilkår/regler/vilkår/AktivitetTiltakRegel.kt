package no.nav.tilleggsstonader.sak.vilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.regelIder

class AktivitetTiltakRegel : Vilkårsregel(
    vilkårType = VilkårType.AKTIVITET_TILTAK,
    regler = setOf(LØNN_GJENNOM_TILTAK, MOTTAR_SYKEPENGER_GJENNOM_AKTIVITET),
    hovedregler = regelIder(LØNN_GJENNOM_TILTAK, MOTTAR_SYKEPENGER_GJENNOM_AKTIVITET),
) {
    companion object {
        private val LØNN_GJENNOM_TILTAK =
            RegelSteg(
                regelId = RegelId.LØNN_GJENNOM_TILTAK,
                jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.OPPFYLT,
                    hvisNei = SluttSvarRegel.IKKE_OPPFYLT,
                ),
            )

        private val MOTTAR_SYKEPENGER_GJENNOM_AKTIVITET =
            RegelSteg(
                regelId = RegelId.MOTTAR_SYKEPENGER_GJENNOM_AKTIVITET,
                jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.OPPFYLT,
                    hvisNei = SluttSvarRegel.IKKE_OPPFYLT,
                ),
            )
    }
}
