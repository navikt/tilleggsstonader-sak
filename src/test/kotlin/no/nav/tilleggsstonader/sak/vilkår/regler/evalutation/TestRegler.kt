package no.nav.tilleggsstonader.sak.vilkår.regler.evalutation

import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.regler.BegrunnelseType
import no.nav.tilleggsstonader.sak.vilkår.regler.NesteRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.regler.jaNeiSvarRegel

class VilkårsregelEnHovedregel :
    Vilkårsregel(
        VilkårType.EKSEMPEL,
        setOf(
            RegelSteg(
                RegelId.HAR_ET_NAVN,
                jaNeiSvarRegel(
                    hvisNei = NesteRegel(
                        RegelId.HAR_ET_NAVN2,
                        BegrunnelseType.PÅKREVD,
                    ),
                ),
            ),
            RegelSteg(
                regelId = RegelId.HAR_ET_NAVN2,
                svarMapping = jaNeiSvarRegel(),
            ),
        ),
        hovedregler = setOf(RegelId.HAR_ET_NAVN),
    )

class VilkårsregelToHovedregler :
    Vilkårsregel(
        VilkårType.EKSEMPEL,
        setOf(
            RegelSteg(
                regelId = RegelId.HAR_ET_NAVN,
                svarMapping = jaNeiSvarRegel(hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE),
            ),
            RegelSteg(
                regelId = RegelId.HAR_ET_NAVN2,
                svarMapping = jaNeiSvarRegel(),
            ),
        ),
        hovedregler = setOf(
            RegelId.HAR_ET_NAVN,
            RegelId.HAR_ET_NAVN2,
        ),
    )
