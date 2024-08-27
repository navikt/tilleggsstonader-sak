package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.evalutation

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.BegrunnelseType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.NesteRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.jaNeiSvarRegel

class VilkårsregelEnHovedregel :
    Vilkårsregel(
        VilkårType.EKSEMPEL,
        setOf(
            RegelSteg(
                regelId = RegelId.HAR_ET_NAVN,
                erHovedregel = true,
                svarMapping = jaNeiSvarRegel(
                    hvisNei = NesteRegel(
                        RegelId.HAR_ET_NAVN2,
                        BegrunnelseType.PÅKREVD,
                    ),
                ),
            ),
            RegelSteg(
                regelId = RegelId.HAR_ET_NAVN2,
                erHovedregel = false,
                svarMapping = jaNeiSvarRegel(),
            ),
        ),
    )

class VilkårsregelToHovedregler :
    Vilkårsregel(
        VilkårType.EKSEMPEL,
        setOf(
            RegelSteg(
                regelId = RegelId.HAR_ET_NAVN,
                erHovedregel = true,
                svarMapping = jaNeiSvarRegel(hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE),
            ),
            RegelSteg(
                regelId = RegelId.HAR_ET_NAVN2,
                erHovedregel = true,
                svarMapping = jaNeiSvarRegel(),
            ),
        ),
    )
