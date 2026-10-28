package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.roah

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.BegrunnelseType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.NesteRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.OPPFYLT_MED_VALGFRI_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.jaNeiSvarRegel

class MidlertidigBostedPeriodeRegel :
    Vilkårsregel(
        vilkårType = VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE,
        regler =
            setOf(
                RegelSteg(
                    regelId = RegelId.ROAH_MÅ_BO_MIDLERTIDIG_BORTE,
                    erHovedregel = true,
                    svarMapping =
                        jaNeiSvarRegel(
                            hvisJa = NesteRegel(RegelId.ROAH_HAR_BARN_UNDER_18_ÅR_SOM_FLYTTER_MED, BegrunnelseType.VALGFRI),
                            hvisNei = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                        ),
                ),
                RegelSteg(
                    regelId = RegelId.ROAH_HAR_BARN_UNDER_18_ÅR_SOM_FLYTTER_MED,
                    erHovedregel = false,
                    svarMapping =
                        jaNeiSvarRegel(
                            hvisJa = NesteRegel(RegelId.ROAH_HAR_BARN_IKKE_FULLFØRT_FJERDE_SKOLEÅR_HJEMME, BegrunnelseType.VALGFRI),
                            hvisNei = NesteRegel(RegelId.ROAH_HAR_BARN_IKKE_FULLFØRT_FJERDE_SKOLEÅR_HJEMME, BegrunnelseType.VALGFRI),
                        ),
                ),
                RegelSteg(
                    regelId = RegelId.ROAH_HAR_BARN_IKKE_FULLFØRT_FJERDE_SKOLEÅR_HJEMME,
                    erHovedregel = false,
                    svarMapping =
                        jaNeiSvarRegel(
                            hvisJa = NesteRegel(RegelId.ROAH_HAR_DOKUMENTERTE_SÆRSKILTE_BEHOV_FLERE_HJEMREISER, BegrunnelseType.VALGFRI),
                            hvisNei = NesteRegel(RegelId.ROAH_HAR_DOKUMENTERTE_SÆRSKILTE_BEHOV_FLERE_HJEMREISER, BegrunnelseType.VALGFRI),
                        ),
                ),
                RegelSteg(
                    regelId = RegelId.ROAH_HAR_DOKUMENTERTE_SÆRSKILTE_BEHOV_FLERE_HJEMREISER,
                    erHovedregel = false,
                    svarMapping =
                        jaNeiSvarRegel(
                            hvisJa = OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                            hvisNei = OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                        ),
                ),
            ),
    )
