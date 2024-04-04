package no.nav.tilleggsstonader.sak.behandling.fakta

import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.ArbeidOgOpphold
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.OppholdUtenforNorge

object FaktaArbeidOgOppholdMapper {

    fun mapArbeidOgOpphold(arbeidOgOpphold: ArbeidOgOpphold?): FaktaArbeidOgOpphold? {
        if (arbeidOgOpphold == null) return null

        return FaktaArbeidOgOpphold(
            jobberIAnnetLand = arbeidOgOpphold.jobberIAnnetLand,
            jobbAnnetLand = arbeidOgOpphold.jobbAnnetLand,
            harPengestøtteAnnetLand = arbeidOgOpphold.harPengestøtteAnnetLand,
            pengestøtteAnnetLand = arbeidOgOpphold.pengestøtteAnnetLand,
            harOppholdUtenforNorgeSiste12mnd = arbeidOgOpphold.harOppholdUtenforNorgeSiste12mnd,
            oppholdUtenforNorgeSiste12mnd = arbeidOgOpphold.oppholdUtenforNorgeSiste12mnd.map(::mapOpphold),
            harOppholdUtenforNorgeNeste12mnd = arbeidOgOpphold.harOppholdUtenforNorgeNeste12mnd,
            oppholdUtenforNorgeNeste12mnd = arbeidOgOpphold.oppholdUtenforNorgeNeste12mnd.map(::mapOpphold),
        )
    }

    private fun mapOpphold(opphold: OppholdUtenforNorge): FaktaOppholdUtenforNorge {
        return FaktaOppholdUtenforNorge(
            land = opphold.land,
            årsak = opphold.årsak,
            fom = opphold.fom,
            tom = opphold.tom,
        )
    }
}
