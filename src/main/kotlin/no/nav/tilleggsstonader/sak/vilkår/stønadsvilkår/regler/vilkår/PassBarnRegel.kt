package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.libs.utils.fnr.Fødselsnummer
import no.nav.tilleggsstonader.libs.utils.osloDateNow
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
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.jaNeiSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.regelIder
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår.PassBarnRegelUtil.harFullførtFjerdetrinn
import java.time.LocalDate
import java.util.UUID

class PassBarnRegel : Vilkårsregel(
    vilkårType = VilkårType.PASS_BARN,
    regler = setOf(
        UTGIFTER_DOKUMENTERT,
        ANNEN_FORELDER_MOTTAR_STØTTE,
        HAR_FULLFØRT_FJERDEKLASSE,
        UNNTAK_ALDER,
    ),
    hovedregler = regelIder(
        UTGIFTER_DOKUMENTERT,
        ANNEN_FORELDER_MOTTAR_STØTTE,
        HAR_FULLFØRT_FJERDEKLASSE,
    ),
) {

    override fun initiereDelvilkår(
        metadata: HovedregelMetadata,
        resultat: Vilkårsresultat,
        barnId: UUID?,
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

        private val unntakAlderMapping =
            setOf(
                SvarId.TRENGER_MER_TILSYN_ENN_JEVNALDRENDE,
                SvarId.FORSØRGER_HAR_LANGVARIG_ELLER_UREGELMESSIG_ARBEIDSTID,
            )
                .associateWith {
                    SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE
                } + mapOf(SvarId.NEI to SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE)

        private val UNNTAK_ALDER =
            RegelSteg(
                regelId = RegelId.UNNTAK_ALDER,
                svarMapping = unntakAlderMapping,
            )

        private val HAR_FULLFØRT_FJERDEKLASSE =
            RegelSteg(
                regelId = RegelId.HAR_FULLFØRT_FJERDEKLASSE,
                svarMapping = jaNeiSvarRegel(
                    hvisJa = NesteRegel(UNNTAK_ALDER.regelId),
                    hvisNei = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                ),
            )

        private val UTGIFTER_DOKUMENTERT =
            RegelSteg(
                regelId = RegelId.UTGIFTER_DOKUMENTERT,
                jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                    hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                ),
            )

        private val ANNEN_FORELDER_MOTTAR_STØTTE =
            RegelSteg(
                regelId = RegelId.ANNEN_FORELDER_MOTTAR_STØTTE,
                jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE,
                    hvisNei = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
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
        barnId: UUID?,
        metadata: HovedregelMetadata,
        datoForBeregning: LocalDate = osloDateNow(),
    ): Boolean {
        val ident = metadata.barn.firstOrNull { it.id == barnId }?.ident
        feilHvis(ident == null) { "Fant ikke barn med id=$barnId i metadata" }

        return harFullførtFjerdetrinn(Fødselsnummer(ident).fødselsdato, datoForBeregning)
    }
}
