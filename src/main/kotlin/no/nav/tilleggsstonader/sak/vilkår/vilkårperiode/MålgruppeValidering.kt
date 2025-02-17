package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlagsdata
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
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

    fun aldersvilkårErOppfylt(
        målgruppeType: MålgruppeType,
        stønadstype: Stønadstype,
        grunnlagsdata: Grunnlagsdata,
    ) {
        val fødselsdato = grunnlagsdata.grunnlag.fødsel?.fødselsdato

        val gyldig: Boolean? =
            when (stønadstype) {
                Stønadstype.BARNETILSYN, Stønadstype.LÆREMIDLER ->
                    when (målgruppeType) {
                        MålgruppeType.AAP, MålgruppeType.NEDSATT_ARBEIDSEVNE, MålgruppeType.UFØRETRYGD ->
                            fødselsdatomellom18og67år(
                                fødselsdato,
                            )

                        MålgruppeType.OMSTILLINGSSTØNAD -> fødselsdatounder67år(fødselsdato)
                        MålgruppeType.OVERGANGSSTØNAD -> true
                        MålgruppeType.DAGPENGER -> null
                        MålgruppeType.SYKEPENGER_100_PROSENT -> null
                        MålgruppeType.INGEN_MÅLGRUPPE -> null
                    }
            }
        feilHvis(gyldig == false) {
            "Aldersvilkår er ikke oppfylt ved opprettelse av målgruppe=$målgruppeType for behandling=${grunnlagsdata.behandlingId}"
        }
    }

    private fun fødselsdatomellom18og67år(fødselsdato: LocalDate?): Boolean {
        val over18år: Boolean = fødselsdato?.plusYears(18)?.isBefore(LocalDate.now()) == true
        val under67år: Boolean = fødselsdato?.plusYears(67)?.isAfter(LocalDate.now()) == true

        return over18år && under67år
    }

    private fun fødselsdatounder67år(fødselsdato: LocalDate?): Boolean = fødselsdato?.plusYears(67)?.isAfter(LocalDate.now()) == true
}
