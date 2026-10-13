package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.vilkår

import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Delvilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.SvarOgBegrunnelse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vurdering
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId

object DagligReiseRegelTestUtil {
    fun oppfylteDelvilkårDagligReiseOffentligTransport() =
        listOf(
            Delvilkår(
                resultat = Vilkårsresultat.OPPFYLT,
                vurderinger =
                    listOf(
                        Vurdering(
                            regelId = RegelId.AVSTAND_OVER_SEKS_KM,
                            svar = SvarId.JA,
                            begrunnelse = "antall km",
                        ),
                        Vurdering(
                            regelId = RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT,
                            svar = SvarId.JA,
                            begrunnelse = "En begrunnelse på delvilkåret",
                        ),
                    ),
            ),
        )

    fun oppfylteDelvilkårDagligReisePrivatBil() =
        listOf(
            Delvilkår(
                resultat = Vilkårsresultat.OPPFYLT,
                vurderinger =
                    listOf(
                        Vurdering(
                            regelId = RegelId.AVSTAND_OVER_SEKS_KM,
                            svar = SvarId.JA,
                            begrunnelse = "antall km",
                        ),
                        Vurdering(
                            regelId = RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT,
                            svar = SvarId.NEI,
                            begrunnelse = "En begrunnelse på delvilkåret",
                        ),
                        Vurdering(
                            regelId = RegelId.KAN_KJØRE_MED_EGEN_BIL,
                            svar = SvarId.JA,
                            begrunnelse = "En begrunnelse på delvilkåret",
                        ),
                    ),
            ),
        )

    fun ikkeOppfylteDelvilkårDagligReise() =
        listOf(
            Delvilkår(
                resultat = Vilkårsresultat.IKKE_OPPFYLT,
                vurderinger =
                    listOf(
                        Vurdering(
                            regelId = RegelId.AVSTAND_OVER_SEKS_KM,
                            svar = SvarId.NEI,
                            begrunnelse =
                                "bare 3km. Med en gangske lang begrunnelse sånn at man kan se hvordan det ser ut " +
                                    "når saksbehandler skriver veldig masse.",
                        ),
                    ),
            ),
        )

    val oppfylteSvarOffentligtransport: Map<RegelId, SvarOgBegrunnelse> =
        mapOf(
            RegelId.AVSTAND_OVER_SEKS_KM to
                SvarOgBegrunnelse(
                    svar = SvarId.JA,
                    begrunnelse = "antall km",
                ),
            RegelId.KAN_REISE_MED_OFFENTLIG_TRANSPORT to
                SvarOgBegrunnelse(
                    svar = SvarId.JA,
                    begrunnelse = "En begrunnelse på delvilkåret",
                ),
        )
}
