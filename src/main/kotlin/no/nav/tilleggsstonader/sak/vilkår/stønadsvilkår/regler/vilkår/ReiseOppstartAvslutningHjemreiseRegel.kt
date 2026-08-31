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

/**
 * Hvilken type reise (oppstart/avslutning/hjemreise) saken gjelder lagres som eget fakta-felt
 * ([no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål]) direkte
 * fra frontend, og er ikke en del av selve regelverket her – den påvirker ikke resultatet. Regelverket er ellers likt
 * som for [ReiseTilSamlingRegel] (uten spørsmålene knyttet til stipend).
 */
class ReiseOppstartAvslutningHjemreiseRegel :
    Vilkårsregel(
        vilkårType = VilkårType.REISE_OPPSTART_AVSLUTNING_HJEMREISE,
        regler =
            setOf(
                KAN_REISE_MED_OFFENTLIG_TRANSPORT,
                KAN_REISE_MED_EGEN_BIL,
            ),
    ) {
    companion object {
        private val KAN_REISE_MED_EGEN_BIL =
            RegelSteg(
                regelId = RegelId.KAN_REISE_MED_EGEN_BIL,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa =
                            SluttSvarRegel(
                                resultat = Resultat.OPPFYLT,
                                begrunnelseType = BegrunnelseType.UTEN,
                                tilhørendeFaktaType = TypeVilkårFakta.REISE_OPPSTART_AVSLUTNING_HJEMREISE_PRIVAT_BIL,
                            ),
                        hvisNei =
                            SluttSvarRegel(
                                resultat = Resultat.IKKE_OPPFYLT,
                                begrunnelseType = BegrunnelseType.PÅKREVD,
                            ),
                    ),
            )
        private val KAN_REISE_MED_OFFENTLIG_TRANSPORT =
            RegelSteg(
                regelId = RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT,
                erHovedregel = true,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa =
                            SluttSvarRegel(
                                resultat = Resultat.OPPFYLT,
                                begrunnelseType = BegrunnelseType.UTEN,
                                tilhørendeFaktaType = TypeVilkårFakta.REISE_OPPSTART_AVSLUTNING_HJEMREISE_OFFENTLIG_TRANSPORT,
                            ),
                        hvisNei = NesteRegel(KAN_REISE_MED_EGEN_BIL.regelId, BegrunnelseType.PÅKREVD),
                    ),
            )
    }
}
