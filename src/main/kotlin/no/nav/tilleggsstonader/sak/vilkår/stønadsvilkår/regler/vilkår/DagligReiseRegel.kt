package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.TypeVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.BegrunnelseType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.NesteRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Resultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.OPPFYLT_MED_VALGFRI_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.jaNeiSvarRegel

class DagligReiseRegel :
    Vilkårsregel(
        vilkårType = VilkårType.DAGLIG_REISE,
        regler =
            setOf(
                AVSTAND_OVER_SEKS_KM,
                UNNTAK_SEKS_KM,
                KAN_REISE_MED_OFFENTLIG_TRANSPORT,
                KAN_KJØRE_MED_EGEN_BIL,
            ),
    ) {
    companion object {
        private val KAN_KJØRE_MED_EGEN_BIL =
            RegelSteg(
                regelId = RegelId.KAN_KJØRE_MED_EGEN_BIL,
                erHovedregel = false,
                svarMapping =
                    mapOf(
                        SvarId.JA to
                            SluttSvarRegel(
                                resultat = Resultat.OPPFYLT,
                                begrunnelseType = BegrunnelseType.VALGFRI,
                                tilhørendeFaktaType = TypeVilkårFakta.DAGLIG_REISE_PRIVAT_BIL,
                            ),
                        SvarId.NEI to OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                    ),
            )

        private val KAN_REISE_MED_OFFENTLIG_TRANSPORT =
            RegelSteg(
                regelId = RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa =
                            SluttSvarRegel(
                                resultat = Resultat.OPPFYLT,
                                begrunnelseType = BegrunnelseType.VALGFRI,
                                tilhørendeFaktaType = TypeVilkårFakta.DAGLIG_REISE_OFFENTLIG_TRANSPORT,
                            ),
                        hvisNei = NesteRegel(KAN_KJØRE_MED_EGEN_BIL.regelId, BegrunnelseType.PÅKREVD),
                    ),
            )

        private val UNNTAK_SEKS_KM =
            RegelSteg(
                regelId = RegelId.UNNTAK_SEKS_KM,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(KAN_REISE_MED_OFFENTLIG_TRANSPORT.regelId, BegrunnelseType.PÅKREVD),
                        hvisNei = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )

        private val AVSTAND_OVER_SEKS_KM =
            RegelSteg(
                regelId = RegelId.AVSTAND_OVER_SEKS_KM,
                erHovedregel = true,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(KAN_REISE_MED_OFFENTLIG_TRANSPORT.regelId, BegrunnelseType.VALGFRI),
                        hvisNei = NesteRegel(UNNTAK_SEKS_KM.regelId),
                    ),
            )
    }
}
