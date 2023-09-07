package no.nav.tilleggsstonader.sak.tilgang

import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering.FORTROLIG
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND

data class PersonMedRelasjoner(
    val personIdent: String,
    val adressebeskyttelse: AdressebeskyttelseGradering,
    val barn: List<PersonMedAdresseBeskyttelse>,
)

data class PersonMedAdresseBeskyttelse(
    val personIdent: String,
    val adressebeskyttelse: AdressebeskyttelseGradering,
)

fun List<PersonMedAdresseBeskyttelse>.personIdentMedKode6(): String? =
    this.find { it.adressebeskyttelse == STRENGT_FORTROLIG || it.adressebeskyttelse == STRENGT_FORTROLIG_UTLAND }
        ?.personIdent

fun List<PersonMedAdresseBeskyttelse>.personMedKode7(): String? =
    this.find { it.adressebeskyttelse == FORTROLIG }?.personIdent
