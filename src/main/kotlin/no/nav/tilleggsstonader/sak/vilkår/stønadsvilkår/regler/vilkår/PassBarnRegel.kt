package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.libs.utils.fnr.Fødselsnummer
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.NesteRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel.Companion.OPPFYLT_MED_VALGFRI_BEGRUNNELSE
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.jaNeiSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelUtil.harFullførtFjerdetrinn
import java.time.LocalDate

class PassBarnRegel :
    Vilkårsregel(
        vilkårType = VilkårType.PASS_BARN,
        regler =
            setOf(
                UTGIFTER_DOKUMENTERT,
                ANNEN_FORELDER_MOTTAR_STØTTE,
                HAR_FULLFØRT_FJERDEKLASSE,
                UNNTAK_ALDER,
            ),
    ) {
    // TODO då man ikke lengre initierer delvilkår fra backend med periodisering av vilkår burde denne fjernes?
    override fun initiereDelvilkår(
        metadata: HovedregelMetadata,
        resultat: Vilkårsresultat,
        barnId: BarnId?,
    ): List<Delvilkår> {
        if (resultat != Vilkårsresultat.IKKE_TATT_STILLING_TIL) {
            return super.initiereDelvilkår(metadata, resultat, barnId)
        }

        return hovedregler.map {
            if (it == RegelId.HAR_FULLFØRT_FJERDEKLASSE && !harFullførtFjerdetrinn(barnId, metadata)) {
                automatiskVurderAlderLavereEnnGrenseverdi()
            } else {
                Delvilkår(resultat, vurderinger = listOf(Vurdering(it)))
            }
        }
    }

    companion object {
        private val UNNTAK_ALDER =
            RegelSteg(
                regelId = RegelId.UNNTAK_ALDER,
                erHovedregel = false,
                svarMapping =
                    mapOf(
                        SvarId.TRENGER_MER_TILSYN_ENN_JEVNALDRENDE to OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                        SvarId.FORSØRGER_HAR_LANGVARIG_ELLER_UREGELMESSIG_ARBEIDSTID to OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                        SvarId.NEI to IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    ),
            )

        private val HAR_FULLFØRT_FJERDEKLASSE =
            RegelSteg(
                regelId = RegelId.HAR_FULLFØRT_FJERDEKLASSE,
                erHovedregel = true,
                svarMapping =
                    jaNeiSvarRegel(
                        hvisJa = NesteRegel(UNNTAK_ALDER.regelId),
                        hvisNei = OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                    ),
            )

        private val UTGIFTER_DOKUMENTERT =
            RegelSteg(
                regelId = RegelId.UTGIFTER_DOKUMENTERT,
                erHovedregel = true,
                jaNeiSvarRegel(
                    hvisJa = OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                    hvisNei = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                ),
            )

        private val ANNEN_FORELDER_MOTTAR_STØTTE =
            RegelSteg(
                regelId = RegelId.ANNEN_FORELDER_MOTTAR_STØTTE,
                erHovedregel = true,
                jaNeiSvarRegel(
                    hvisJa = IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    hvisNei = OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                ),
            )
    }

    private fun automatiskVurderAlderLavereEnnGrenseverdi(): Delvilkår {
        val iDag = osloDateNow().norskFormat()
        val begrunnelse =
            "Automatisk vurdert: Ut ifra barnets alder er det $iDag automatisk vurdert at barnet ikke har fullført 4. skoleår."

        return Delvilkår(
            resultat = Vilkårsresultat.AUTOMATISK_OPPFYLT,
            listOf(
                Vurdering(
                    regelId = RegelId.HAR_FULLFØRT_FJERDEKLASSE,
                    svar = SvarId.NEI,
                    begrunnelse = begrunnelse,
                ),
            ),
        )
    }

    private fun harFullførtFjerdetrinn(
        barnId: BarnId?,
        metadata: HovedregelMetadata,
        datoForBeregning: LocalDate = osloDateNow(),
    ): Boolean {
        val ident = metadata.barn.firstOrNull { it.id == barnId }?.ident
        feilHvis(ident == null, sensitivFeilmelding = { "Fant ikke barn med id=$barnId i metadata" }) {
            "Fant ikke barn i metadata"
        }

        return harFullførtFjerdetrinn(Fødselsnummer(ident).fødselsdato, datoForBeregning)
    }
}
