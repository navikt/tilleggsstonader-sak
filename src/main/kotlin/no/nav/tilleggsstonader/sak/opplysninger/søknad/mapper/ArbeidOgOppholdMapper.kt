package no.nav.tilleggsstonader.sak.opplysninger.søknad.mapper

import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.ArbeidOgOpphold
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.OppholdUtenforNorge
import no.nav.tilleggsstonader.kontrakter.søknad.felles.ArbeidOgOpphold as ArbeidOgOppholdKontrakt
import no.nav.tilleggsstonader.kontrakter.søknad.felles.OppholdUtenforNorge as OppholdUtenforNorgeKontrakter

object ArbeidOgOppholdMapper {
    fun mapArbeidOgOpphold(kontrakt: ArbeidOgOppholdKontrakt?): ArbeidOgOpphold? {
        if (kontrakt == null) return null
        return ArbeidOgOpphold(
            jobberIAnnetLand = kontrakt.jobberIAnnetLand?.verdi,
            jobbAnnetLand = kontrakt.jobbAnnetLand?.verdi,
            harPengestøtteAnnetLand = kontrakt.harPengestøtteAnnetLand?.verdier?.map { it.verdi },
            pengestøtteAnnetLand = kontrakt.pengestøtteAnnetLand?.verdi,
            harOppholdUtenforNorgeSiste12mnd = kontrakt.harOppholdUtenforNorgeSiste12mnd?.verdi,
            oppholdUtenforNorgeSiste12mnd = kontrakt.oppholdUtenforNorgeSiste12mnd.map(::mapOpphold),
            harOppholdUtenforNorgeNeste12mnd = kontrakt.harOppholdUtenforNorgeNeste12mnd?.verdi,
            oppholdUtenforNorgeNeste12mnd = kontrakt.oppholdUtenforNorgeNeste12mnd.map(::mapOpphold),
        )
    }

    private fun mapOpphold(opphold: OppholdUtenforNorgeKontrakter) = OppholdUtenforNorge(
        land = opphold.land.verdi,
        årsak = opphold.årsak.verdier.map { it.verdi },
        fom = opphold.fom.verdi,
        tom = opphold.tom.verdi,
    )
}
