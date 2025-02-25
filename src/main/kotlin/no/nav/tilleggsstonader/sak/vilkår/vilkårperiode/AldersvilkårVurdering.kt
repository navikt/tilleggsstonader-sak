package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlagsdata
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.VurderingAldersVilkår
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import java.time.LocalDate

object AldersvilkårVurdering {
    fun vurderAldersvilkår(
        vilkårperiode: LagreVilkårperiode,
        grunnlagsdata: Grunnlagsdata,
    ): VurderingAldersVilkår {
        val fødselsdato = grunnlagsdata.grunnlag.fødsel?.fødselsdato

        feilHvis(fødselsdato == null) { "Kan ikke vurdere aldersvilkår uten å vite fødselsdato til bruker" }

        val gyldig: SvarJaNei =
            when (vilkårperiode.type as MålgruppeType) {
                MålgruppeType.AAP, MålgruppeType.NEDSATT_ARBEIDSEVNE, MålgruppeType.UFØRETRYGD ->
                    vurderAldersvilkårForNedsattArbeidsevne(fødselsdato, vilkårperiode)
                MålgruppeType.OMSTILLINGSSTØNAD -> vurderAldersvilkårForOmstillingsstønad(fødselsdato, vilkårperiode)
                MålgruppeType.OVERGANGSSTØNAD -> SvarJaNei.JA_IMPLISITT
                MålgruppeType.DAGPENGER -> SvarJaNei.NEI
                MålgruppeType.SYKEPENGER_100_PROSENT -> SvarJaNei.NEI
                MålgruppeType.INGEN_MÅLGRUPPE -> SvarJaNei.NEI
            }

        return VurderingAldersVilkår(
            gyldig,
            genererFaktaSomJson(fødselsdato = fødselsdato),
        )
    }

    private fun genererFaktaSomJson(fødselsdato: LocalDate): String = "{ \"fødselsdato\": \"$fødselsdato\"}"

    private fun heleVilkårsperiodenErFørBrukerFyller67År(
        fødselsdato: LocalDate,
        vilkårsperiodeFom: LocalDate,
        vilkårsperiodeTom: LocalDate,
    ): Boolean {
        val sekstisyvÅrsDagenTilBruker = fødselsdato.plusYears(67)
        feilHvis((vilkårsperiodeFom < sekstisyvÅrsDagenTilBruker) && (sekstisyvÅrsDagenTilBruker <= vilkårsperiodeTom)) {
            "Brukeren fyller 67 år i løpet av vilkårsperioden"
        }
        return vilkårsperiodeTom < sekstisyvÅrsDagenTilBruker
    }

    private fun heleVilkårsperiodenErEtterBrukerFyller18År(
        fødselsdato: LocalDate,
        vilkårsperiodeFom: LocalDate,
        vilkårsperiodeTom: LocalDate,
    ): Boolean {
        val attenårsdagenTilBruker = fødselsdato.plusYears(18)
        feilHvis((vilkårsperiodeFom <= attenårsdagenTilBruker) && (attenårsdagenTilBruker <= vilkårsperiodeTom)) {
            "Brukeren fyller 18 år i løpet av vilkårsperioden"
        }
        return attenårsdagenTilBruker < vilkårsperiodeFom
    }

    private fun vurderAldersvilkårForNedsattArbeidsevne(
        fødselsdato: LocalDate,
        vilkårperiode: LagreVilkårperiode,
    ): SvarJaNei {
        if (heleVilkårsperiodenErFørBrukerFyller67År(fødselsdato, vilkårperiode.fom, vilkårperiode.tom) &&
            heleVilkårsperiodenErEtterBrukerFyller18År(fødselsdato, vilkårperiode.fom, vilkårperiode.tom)
        ) {
            return SvarJaNei.JA
        }
        return SvarJaNei.NEI
    }

    private fun vurderAldersvilkårForOmstillingsstønad(
        fødselsdato: LocalDate,
        vilkårperiode: LagreVilkårperiode,
    ): SvarJaNei =
        if (heleVilkårsperiodenErFørBrukerFyller67År(
                fødselsdato,
                vilkårperiode.fom,
                vilkårperiode.tom,
            )
        ) {
            SvarJaNei.JA
        } else {
            SvarJaNei.NEI
        }
}
