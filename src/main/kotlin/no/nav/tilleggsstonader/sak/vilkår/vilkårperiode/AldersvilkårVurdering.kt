package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlagsdata
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import java.time.LocalDate

object AldersvilkårVurdering {
    fun vurderAldersvilkår(
        vilkårperiode: LagreVilkårperiode,
        grunnlagsData: Grunnlagsdata,
    ): SvarJaNei {
        val fødselsdato = grunnlagsData.grunnlag.fødsel?.fødselsdato

        feilHvis(fødselsdato == null) { "Kan ikke vurdere aldersvilkår uten å vite fødselsdato til bruker" }

        val gyldig: SvarJaNei =
            when (vilkårperiode.type as MålgruppeType) {
                MålgruppeType.AAP, MålgruppeType.NEDSATT_ARBEIDSEVNE, MålgruppeType.UFØRETRYGD ->
                    vurderAldersvilkårForNedsattArbeidsevne(fødselsdato, vilkårperiode)
                MålgruppeType.OMSTILLINGSSTØNAD -> vurderAldersvilkårForOmstillingsstønad(fødselsdato, vilkårperiode)
                MålgruppeType.OVERGANGSSTØNAD -> SvarJaNei.JA_IMPLISITT
                else -> throw Feil("Aldersvilkår vurderes ikke for målgruppe: ${vilkårperiode.type}")
            }
        return gyldig
    }

    private fun heleVilkårsperiodenErEtterBrukerFyller18År(
        fødselsdato: LocalDate,
        vilkårperiode: LagreVilkårperiode,
    ): Boolean {
        val attenårsdagenTilBruker = fødselsdato.plusYears(18)
        feilHvis((vilkårperiode.fom <= attenårsdagenTilBruker) && (attenårsdagenTilBruker <= vilkårperiode.tom)) {
            "Brukeren fyller 18 år i løpet av vilkårsperioden"
        }
        return attenårsdagenTilBruker < vilkårperiode.fom
    }

    private fun heleVilkårsperiodenErFørBrukerFyller67År(
        fødselsdato: LocalDate,
        vilkårperiode: LagreVilkårperiode,
    ): Boolean {
        val sekstisyvÅrsDagenTilBruker = fødselsdato.plusYears(67)
        feilHvis((vilkårperiode.fom <= sekstisyvÅrsDagenTilBruker) && (sekstisyvÅrsDagenTilBruker <= vilkårperiode.tom)) {
            "Brukeren fyller 67 år i løpet av vilkårsperioden"
        }
        return vilkårperiode.tom < sekstisyvÅrsDagenTilBruker
    }

    private fun vurderAldersvilkårForNedsattArbeidsevne(
        fødselsdato: LocalDate,
        vilkårperiode: LagreVilkårperiode,
    ): SvarJaNei {
        if (heleVilkårsperiodenErEtterBrukerFyller18År(fødselsdato, vilkårperiode) &&
            heleVilkårsperiodenErFørBrukerFyller67År(fødselsdato, vilkårperiode)
        ) {
            return SvarJaNei.JA
        }
        return SvarJaNei.NEI
    }

    private fun vurderAldersvilkårForOmstillingsstønad(
        fødselsdato: LocalDate,
        vilkårperiode: LagreVilkårperiode,
    ): SvarJaNei =
        if (heleVilkårsperiodenErFørBrukerFyller67År(fødselsdato, vilkårperiode)
        ) {
            SvarJaNei.JA
        } else {
            SvarJaNei.NEI
        }

    data class VurderingFaktaEtterlevelseAldersvilkår(
        val vilkårperiodeFom: LocalDate,
        val vilkårperiodeTom: LocalDate,
        val fødselsdato: LocalDate?,
    ) {
        init {
            require(fødselsdato != null) { "Fødselsdato må være kjent for å vurdere aldersvilkår" }
        }
    }
}
