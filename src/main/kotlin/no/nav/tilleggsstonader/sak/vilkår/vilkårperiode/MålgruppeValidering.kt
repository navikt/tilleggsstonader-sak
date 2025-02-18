package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlagsdata
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.LagreVilkårperiode
import java.time.LocalDate

object MålgruppeValidering {
    fun validerKanLeggeTilMålgruppeManuelt(
        stønadstype: Stønadstype,
        målgruppeType: MålgruppeType,
    ) {
        val gyldig =
            when (stønadstype) {
                Stønadstype.BARNETILSYN, Stønadstype.LÆREMIDLER ->
                    when (målgruppeType) {
                        MålgruppeType.AAP -> true
                        MålgruppeType.DAGPENGER -> false
                        MålgruppeType.NEDSATT_ARBEIDSEVNE -> true
                        MålgruppeType.OMSTILLINGSSTØNAD -> true
                        MålgruppeType.OVERGANGSSTØNAD -> true
                        MålgruppeType.UFØRETRYGD -> true
                        MålgruppeType.SYKEPENGER_100_PROSENT -> true
                        MålgruppeType.INGEN_MÅLGRUPPE -> true
                    }
            }
        feilHvisIkke(gyldig) {
            "målgruppe=$målgruppeType er ikke gyldig for $stønadstype"
        }
    }

    fun vurderAldersvilkår(
        vilkårperiode: LagreVilkårperiode,
        stønadstype: Stønadstype,
        grunnlagsdata: Grunnlagsdata,
    ) {
        val fødselsdato = grunnlagsdata.grunnlag.fødsel?.fødselsdato

        val gyldig: SvarJaNei? =
            when (stønadstype) {
                Stønadstype.BARNETILSYN, Stønadstype.LÆREMIDLER ->
                    when (vilkårperiode.type as MålgruppeType) {
                        MålgruppeType.AAP, MålgruppeType.NEDSATT_ARBEIDSEVNE, MålgruppeType.UFØRETRYGD ->
                            brukerErEldreEnn18ogYngreEnn67IgjennomHelePerioden(fødselsdato, vilkårperiode.fom, vilkårperiode.tom)
                        MålgruppeType.OMSTILLINGSSTØNAD ->
                            if (brukerErYngreEnn67ÅrIgjennomHelePerioden(
                                    fødselsdato,
                                    vilkårperiode.fom,
                                )
                            ) {
                                SvarJaNei.JA
                            } else {
                                SvarJaNei.NEI
                            }
                        MålgruppeType.OVERGANGSSTØNAD -> SvarJaNei.JA
                        MålgruppeType.DAGPENGER -> null
                        MålgruppeType.SYKEPENGER_100_PROSENT -> null
                        MålgruppeType.INGEN_MÅLGRUPPE -> null
                    }
            }
        feilHvis(gyldig == SvarJaNei.NEI) {
            "Aldersvilkår er ikke oppfylt ved opprettelse av målgruppe=${vilkårperiode.type} for behandling=${grunnlagsdata.behandlingId}"
        }
    }

    private fun brukerErYngreEnn67ÅrIgjennomHelePerioden(
        fødselsdato: LocalDate?,
        vilkårsperiodeFom: LocalDate,
    ): Boolean {
        val sekstisyvÅrsDagenTilBruker = fødselsdato?.plusYears(67)
        if (sekstisyvÅrsDagenTilBruker != null) {
            return (sekstisyvÅrsDagenTilBruker < vilkårsperiodeFom)
        }
        return false
    }

    private fun brukerErEldreEnn18ÅrIgjennomHelePerioden(
        fødselsdato: LocalDate?,
        vilkårsperiodeTom: LocalDate,
    ): Boolean {
        val attenårsdagenTilBruker = fødselsdato?.plusYears(18)
        if (attenårsdagenTilBruker != null) {
            return (vilkårsperiodeTom < attenårsdagenTilBruker)
        }
        return false
    }

    private fun brukerErEldreEnn18ogYngreEnn67IgjennomHelePerioden(
        fødselsdato: LocalDate?,
        vilkårsperiodeFom: LocalDate,
        vilkårsperiodeTom: LocalDate,
    ): SvarJaNei? {
        if (brukerErYngreEnn67ÅrIgjennomHelePerioden(fødselsdato, vilkårsperiodeFom) &&
            brukerErEldreEnn18ÅrIgjennomHelePerioden(fødselsdato, vilkårsperiodeTom)
        ) {
            return SvarJaNei.JA
        }
        return SvarJaNei.NEI
    }
}
