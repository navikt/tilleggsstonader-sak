package no.nav.tilleggsstonader.sak.vilkår.regler.vilkår

import no.nav.tilleggsstonader.libs.utils.fnr.Fødselsnummer
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.vilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.regler.HovedregelMetadata
import no.nav.tilleggsstonader.sak.vilkår.regler.NesteRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.SvarId
import no.nav.tilleggsstonader.sak.vilkår.regler.Vilkårsregel
import no.nav.tilleggsstonader.sak.vilkår.regler.jaNeiSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.regler.regelIder
import java.time.LocalDate
import java.util.UUID

class PassBarnRegel : Vilkårsregel(
    vilkårType = VilkårType.PASS_BARN,
    regler = setOf(
        HAR_ALDER_LAVERE_ENN_GRENSEVERDI,
        UNNTAK_ALDER,
        DEKKES_UTGIFTER_ANNET_REGELVERK,
        ANNEN_FORELDER_MOTTAR_STØTTE,
        UTGIFTER_DOKUMENTERT,
    ),
    hovedregler = regelIder(
        HAR_ALDER_LAVERE_ENN_GRENSEVERDI,
        DEKKES_UTGIFTER_ANNET_REGELVERK,
        ANNEN_FORELDER_MOTTAR_STØTTE,
        UTGIFTER_DOKUMENTERT,
    ),
) {

    override fun initiereDelvilkår(
        metadata: HovedregelMetadata,
        resultat: Vilkårsresultat,
        barnId: UUID?,
    ): List<Delvilkår> {
        return hovedregler.map {
            if (it == RegelId.HAR_ALDER_LAVERE_ENN_GRENSEVERDI && !harFullførtFjerdetrinn(barnId, metadata)) {
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
                    SluttSvarRegel.OPPFYLT_MED_PÅKREVD_BEGRUNNELSE
                } + mapOf(SvarId.NEI to SluttSvarRegel.IKKE_OPPFYLT_MED_PÅKREVD_BEGRUNNELSE)

        private val UNNTAK_ALDER =
            RegelSteg(
                regelId = RegelId.UNNTAK_ALDER,
                svarMapping = unntakAlderMapping,
            )

        private val HAR_ALDER_LAVERE_ENN_GRENSEVERDI =
            RegelSteg(
                regelId = RegelId.HAR_ALDER_LAVERE_ENN_GRENSEVERDI,
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
                    hvisNei = SluttSvarRegel.IKKE_OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                ),
            )

        private val ANNEN_FORELDER_MOTTAR_STØTTE =
            RegelSteg(
                regelId = RegelId.ANNEN_FORELDER_MOTTAR_STØTTE,
                jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.IKKE_OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                    hvisNei = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                ),
            )

        private val DEKKES_UTGIFTER_ANNET_REGELVERK =
            RegelSteg(
                regelId = RegelId.DEKKES_UTGIFTER_ANNET_REGELVERK,
                jaNeiSvarRegel(
                    hvisJa = SluttSvarRegel.IKKE_OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                    hvisNei = SluttSvarRegel.OPPFYLT_MED_VALGFRI_BEGRUNNELSE,
                ),
            )
    }

    private fun automatiskVurderAlderLavereEnnGrenseverdi(): Delvilkår {
        val begrunnelse = "Automatisk vurdert: Ut ifra barnets alder er det ${
            LocalDate.now()
                .norskFormat()
        } automatisk vurdert at barnet ikke har fullført 4. skoleår."

        return Delvilkår(
            resultat = Vilkårsresultat.AUTOMATISK_OPPFYLT,
            listOf(
                Vurdering(
                    regelId = RegelId.HAR_ALDER_LAVERE_ENN_GRENSEVERDI,
                    svar = SvarId.NEI,
                    begrunnelse = begrunnelse,
                ),
            ),
        )
    }

    fun harFullførtFjerdetrinn(
        barnId: UUID?,
        metadata: HovedregelMetadata,
        datoForBeregning: LocalDate = LocalDate.now(),
    ): Boolean {
        val ident = metadata.barn.firstOrNull { it.id == barnId }?.ident
        feilHvis(ident == null) { "Fant ikke barn med id=$barnId i metadata" }

        return harFullførtFjerdetrinn(Fødselsnummer(ident).fødselsdato, datoForBeregning)
    }

    fun harFullførtFjerdetrinn(fødselsdato: LocalDate, datoForBeregning: LocalDate = LocalDate.now()): Boolean {
        val alder = datoForBeregning.year - fødselsdato.year
        var skoletrinn = alder - 5 // Begynner på skolen i det året de fyller 6

        if (datoForBeregning.month.plus(1).value < 6) { // Legger til en sikkerhetsmargin på 1 mnd tilfelle de fyller år mens saken behandles
            skoletrinn--
        }

        return skoletrinn > 4
    }
}
