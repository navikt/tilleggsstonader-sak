package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.evaluering

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.DelvilkårVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårAktivitetDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårMålgruppeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.DelvilkårVilkårperiodeDto

data class ResultatEvaluering(
    val delvilkår: DelvilkårVilkårperiode,
    val resultat: ResultatVilkårperiode,
)

object EvalueringVilkårperiode {
    fun evaulerVilkårperiode(type: VilkårperiodeType, delvilkår: DelvilkårVilkårperiodeDto): ResultatEvaluering {
        return when {
            type is MålgruppeType && delvilkår is DelvilkårMålgruppeDto ->
                EvalueringMålgruppe.utledResultat(type, delvilkår)

            type is AktivitetType && delvilkår is DelvilkårAktivitetDto ->
                EvalueringAktivitet.utledResultat(type, delvilkår)

            else -> error("Ugyldig kombinasjon type=$type delvilkår=${delvilkår.javaClass.simpleName}")
        }
    }
}
