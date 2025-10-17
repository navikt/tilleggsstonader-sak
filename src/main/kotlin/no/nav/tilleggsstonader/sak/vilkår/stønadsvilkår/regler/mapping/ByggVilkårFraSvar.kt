package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.mapping

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.SvarOgBegrunnelse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.BegrunnelseType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.NesteRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelSteg
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SluttSvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarRegel
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.Vilkårsregel
import kotlin.collections.get

object ByggVilkårFraSvar {
    fun byggDelvilkårsettFraSvarOgVilkårsregel(
        vilkårsregel: Vilkårsregel,
        svar: Map<RegelId, SvarOgBegrunnelse?>,
    ): List<Delvilkår> = vilkårsregel.hovedregler.map {
        byggDelvilkårFraSvar(
            vilkårsregel = vilkårsregel,
            hovedregelId = it,
            svar = svar
        )
    }

    private fun byggDelvilkårFraSvar(
        vilkårsregel: Vilkårsregel,
        hovedregelId: RegelId,
        svar: Map<RegelId, SvarOgBegrunnelse?>,
    ): Delvilkår {
        val vurderinger = mutableListOf<Vurdering>()
        var regelId: RegelId? = hovedregelId
        var sluttResultat: Vilkårsresultat? = null

        while (regelId != null) {
            val gjeldendeSvar = svar[regelId]
            val gjeldendeRegel = vilkårsregel.regel(regelId)

            val svarRegel = validerSvarOgFinnSvarregel(regel = gjeldendeRegel, svar = gjeldendeSvar)

            vurderinger.add(
                Vurdering(
                    regelId = regelId,
                    svar = gjeldendeSvar?.svarId,
                    begrunnelse = gjeldendeSvar?.begrunnelse,
                ),
            )

            regelId = finnNesteRegelId(svarRegel)
            sluttResultat = finnResultat(svarRegel)
        }

        return Delvilkår(
            resultat = sluttResultat ?: Vilkårsresultat.IKKE_TATT_STILLING_TIL,
            vurderinger = vurderinger,
        )
    }

    private fun validerSvarOgFinnSvarregel(
        regel: RegelSteg,
        svar: SvarOgBegrunnelse?,
    ): SvarRegel? {
        val svarRegel = regel.svarMapping[svar?.svarId]

        feilHvis(svarRegel == null && svar != null) {
            "SvarId=${svar!!.svarId} er ikke et gyldig svar for regelId=${regel.regelId}"
        }

        when (svarRegel?.begrunnelseType) {
            BegrunnelseType.PÅKREVD ->
                feilHvis(svar?.begrunnelse.isNullOrBlank()) {
                    "Påkrevd begrunnelse for regelId=${regel.regelId}"
                }

            BegrunnelseType.UTEN ->
                feilHvisIkke(svar?.begrunnelse.isNullOrBlank()) {
                    "Begrunnelse skal ikke settes for regelId=${regel.regelId}"
                }

            else -> {}
        }

        return svarRegel
    }

    private fun finnNesteRegelId(svarRegel: SvarRegel?): RegelId? = (svarRegel as? NesteRegel)?.regelId

    private fun finnResultat(svarRegel: SvarRegel?): Vilkårsresultat? =
        (svarRegel as? SluttSvarRegel)?.resultat?.vilkårsresultat
}
