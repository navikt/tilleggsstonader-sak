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
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.jaNeiSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål

class ReiseOppstartAvslutningHjemreiseFlyttePeriodeRegel :
    Vilkårsregel(
        vilkårType = VilkårType.PERIODE_MED_REISE_OPPSTART_AVSLUTNING_HJEMREISE,
        regler =
            setOf(
                ROAH_HAR_UTGIFTER_TIL_REISEN,
                ROAH_KAN_REISE_MED_OFFENTLIG_TRANSPORT,
                ROAH_HAR_DOKUMENTERTE_UTGIFTER_TIL_REISEN,
                ROAH_KAN_BENYTTE_PRIVAT_BIL,
                ROAH_BENYTTET_TAXI,
                ROAH_ER_HELE_UTGIFTEN_NØDVENDIG,
                ROAH_HJEMREISE_INNENFOR_NORMALT_ANTALL,
                ROAH_HJEMREISE_BEHOV_FLERE_REISER,
            ),
    ) {
    companion object {
        private val standardRegelsett =
            setOf(
                RegelId.ROAH_HAR_UTGIFTER_TIL_REISEN,
                RegelId.ROAH_KAN_REISE_MED_OFFENTLIG_TRANSPORT,
                RegelId.ROAH_HAR_DOKUMENTERTE_UTGIFTER_TIL_REISEN,
                RegelId.ROAH_KAN_BENYTTE_PRIVAT_BIL,
                RegelId.ROAH_BENYTTET_TAXI,
                RegelId.ROAH_ER_HELE_UTGIFTEN_NØDVENDIG,
            )

        private val reglerPerTypeReiseformål: Map<TypeReiseformål, Set<RegelId>> =
            mapOf(
                TypeReiseformål.OPPSTART to standardRegelsett,
                TypeReiseformål.AVSLUTNING to standardRegelsett,
                TypeReiseformål.HJEMREISE to
                    setOf(
                        RegelId.ROAH_HJEMREISE_INNENFOR_NORMALT_ANTALL,
                        RegelId.ROAH_HJEMREISE_BEHOV_FLERE_REISER,
                    ) + standardRegelsett,
            )

        fun forType(typeReiseformål: TypeReiseformål): Vilkårsregel {
            val alleRegler = ReiseOppstartAvslutningHjemreiseFlyttePeriodeRegel().regler
            val regelIderForType = reglerPerTypeReiseformål.getValue(typeReiseformål)
            val reglerForType = alleRegler.filterKeys { it in regelIderForType }
            return object : Vilkårsregel(VilkårType.REISE_OPPSTART_AVSLUTNING_HJEMREISE, reglerForType) {}
        }

        private val ROAH_HJEMREISE_INNENFOR_NORMALT_ANTALL =
            RegelSteg(
                regelId = RegelId.ROAH_HJEMREISE_INNENFOR_NORMALT_ANTALL,
                erHovedregel = true,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(RegelId.ROAH_HAR_UTGIFTER_TIL_REISEN, BegrunnelseType.VALGFRI),
                        hvisNei = NesteRegel(RegelId.ROAH_HJEMREISE_BEHOV_FLERE_REISER, BegrunnelseType.VALGFRI),
                    ),
            )

        private val ROAH_HJEMREISE_BEHOV_FLERE_REISER =
            RegelSteg(
                regelId = RegelId.ROAH_HJEMREISE_BEHOV_FLERE_REISER,
                erHovedregel = false,
                svarMapping =
                    mapOf(
                        SvarId.ROAH_HAR_BARN_IKKE_FERDIG_MED_FJERDE_SKOLEÅR to
                            NesteRegel(
                                regelId = RegelId.ROAH_HAR_UTGIFTER_TIL_REISEN,
                                begrunnelseType = BegrunnelseType.VALGFRI,
                            ),
                        SvarId.ROAH_HAR_DOKUMENTERTE_ANDRE_SÆRSKILTE_BEHOV to
                            NesteRegel(
                                regelId = RegelId.ROAH_HAR_UTGIFTER_TIL_REISEN,
                                begrunnelseType = BegrunnelseType.PÅKREVD,
                            ),
                        SvarId.NEI to IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )

        private val ROAH_HAR_UTGIFTER_TIL_REISEN =
            RegelSteg(
                regelId = RegelId.ROAH_HAR_UTGIFTER_TIL_REISEN,
                erHovedregel = true,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(RegelId.ROAH_KAN_REISE_MED_OFFENTLIG_TRANSPORT, BegrunnelseType.VALGFRI),
                        hvisNei = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )

        private val ROAH_KAN_REISE_MED_OFFENTLIG_TRANSPORT =
            RegelSteg(
                regelId = RegelId.ROAH_KAN_REISE_MED_OFFENTLIG_TRANSPORT,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(RegelId.ROAH_HAR_DOKUMENTERTE_UTGIFTER_TIL_REISEN, BegrunnelseType.VALGFRI),
                        hvisNei = NesteRegel(RegelId.ROAH_KAN_BENYTTE_PRIVAT_BIL, BegrunnelseType.PÅKREVD),
                    ),
            )

        private val ROAH_HAR_DOKUMENTERTE_UTGIFTER_TIL_REISEN =
            RegelSteg(
                regelId = RegelId.ROAH_HAR_DOKUMENTERTE_UTGIFTER_TIL_REISEN,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa =
                            NesteRegel(
                                regelId = RegelId.ROAH_ER_HELE_UTGIFTEN_NØDVENDIG,
                                begrunnelseType = BegrunnelseType.VALGFRI,
                                tilhørendeFaktaType = TypeVilkårFakta.REISE_OPPSTART_AVSLUTNING_HJEMREISE_OFFENTLIG_TRANSPORT,
                            ),
                        hvisNei = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )

        private val ROAH_KAN_BENYTTE_PRIVAT_BIL =
            RegelSteg(
                regelId = RegelId.ROAH_KAN_BENYTTE_PRIVAT_BIL,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa =
                            NesteRegel(
                                regelId = RegelId.ROAH_ER_HELE_UTGIFTEN_NØDVENDIG,
                                begrunnelseType = BegrunnelseType.VALGFRI,
                                tilhørendeFaktaType = TypeVilkårFakta.REISE_OPPSTART_AVSLUTNING_HJEMREISE_PRIVAT_BIL,
                            ),
                        hvisNei = NesteRegel(RegelId.ROAH_BENYTTET_TAXI, BegrunnelseType.PÅKREVD),
                    ),
            )

        private val ROAH_BENYTTET_TAXI =
            RegelSteg(
                regelId = RegelId.ROAH_BENYTTET_TAXI,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(RegelId.ROAH_ER_HELE_UTGIFTEN_NØDVENDIG, BegrunnelseType.VALGFRI),
                        hvisNei = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )

        private val ROAH_ER_HELE_UTGIFTEN_NØDVENDIG =
            RegelSteg(
                regelId = RegelId.ROAH_ER_HELE_UTGIFTEN_NØDVENDIG,
                erHovedregel = false,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa =
                            SluttSvarRegel(
                                resultat = Resultat.OPPFYLT,
                                begrunnelseType = BegrunnelseType.VALGFRI,
                            ),
                        hvisNei =
                            SluttSvarRegel(
                                resultat = Resultat.OPPFYLT,
                                begrunnelseType = BegrunnelseType.PÅKREVD,
                            ),
                    ),
            )
    }
}
