package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.NesteRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.OPPFYLT_MED_VALGFRI_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.jaNeiSvarRegel

class DagligReiseOffentiligTransportRegel :
    Vilkårsregel(
        vilkårType = VilkårType.DAGLIG_REISE_OFFENTLIG_TRANSPORT,
        regler =
            setOf(
                AVSTAND_OVER_SEKS_KM,
                KAN_BRUKER_REISE_MED_OFFENTLIG_TRANSPORT,
                KAN_BRUKER_KJØRE_SELV,
            ),
    ) {
    companion object {
        private val KAN_BRUKER_KJØRE_SELV =
            RegelSteg(
                regelId = RegelId.KAN_BRUKER_KJØRE_SELV,
                erHovedregel = false,
                svarMapping =
                    mapOf(
                        SvarId.JA to OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                        SvarId.NEI to OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                    ),
            )

        private val KAN_BRUKER_REISE_MED_OFFENTLIG_TRANSPORT =
            RegelSteg(
                regelId = RegelId.KAN_BRUKER_REISE_MED_OFFENTLIG_TRANSPORT,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                        hvisNei = NesteRegel(KAN_BRUKER_KJØRE_SELV.regelId),
                    ),
            )

        private val AVSTAND_OVER_SEKS_KM =
            RegelSteg(
                regelId = RegelId.AVSTAND_OVER_SEKS_KM,
                erHovedregel = true,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(KAN_BRUKER_REISE_MED_OFFENTLIG_TRANSPORT.regelId),
                        hvisNei = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )
    }
}
