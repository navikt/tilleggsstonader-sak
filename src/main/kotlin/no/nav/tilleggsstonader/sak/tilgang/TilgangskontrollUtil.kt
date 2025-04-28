package no.nav.tilleggsstonader.sak.tilgang

import no.nav.tilleggsstonader.sak.opplysninger.pdl.domain.AdressebeskyttelseForPerson
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering.FORTROLIG
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering.UGRADERT

object TilgangskontrollUtil {
    fun høyesteGraderingen(personUtvidet: AdressebeskyttelseForPerson): AdressebeskyttelseGradering {
        val adressebeskyttelser = personUtvidet.adressebeskyttelser()
        return when {
            adressebeskyttelser.contains(STRENGT_FORTROLIG_UTLAND) -> STRENGT_FORTROLIG_UTLAND
            adressebeskyttelser.contains(STRENGT_FORTROLIG) -> STRENGT_FORTROLIG
            adressebeskyttelser.contains(FORTROLIG) -> FORTROLIG
            adressebeskyttelser.contains(UGRADERT) -> UGRADERT
            else -> UGRADERT
        }
    }
}
