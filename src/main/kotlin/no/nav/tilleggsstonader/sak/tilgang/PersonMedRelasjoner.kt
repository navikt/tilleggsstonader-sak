package no.nav.tilleggsstonader.sak.tilgang

import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering.FORTROLIG
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlPersonKort
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.gradering

data class PersonMedRelasjoner(
    val søker: PersonMedAdresseBeskyttelse,
    val barn: List<PersonMedAdresseBeskyttelse>,
    val andreForeldre: List<PersonMedAdresseBeskyttelse>,
) {
    fun identerForEgenAnsattKontroll(): Set<String> = (andreForeldre + søker).map { it.personIdent }.toSet()

    fun adressebeskyttelser(): List<AdressebeskyttelseGradering> = (barn + andreForeldre + søker).map { it.adressebeskyttelse }
}

data class PersonMedAdresseBeskyttelse(
    val personIdent: String,
    val adressebeskyttelse: AdressebeskyttelseGradering,
)

fun List<PersonMedAdresseBeskyttelse>.personIdentMedKode6(): String? =
    this
        .find { it.adressebeskyttelse == STRENGT_FORTROLIG || it.adressebeskyttelse == STRENGT_FORTROLIG_UTLAND }
        ?.personIdent

fun List<PersonMedAdresseBeskyttelse>.personMedKode7(): String? = this.find { it.adressebeskyttelse == FORTROLIG }?.personIdent

@JvmName("pdlBarnKortTilPersonMedAdresseBeskyttelse")
fun Map<String, PdlBarn>.tilPersonMedAdresseBeskyttelse() =
    this.map {
        PersonMedAdresseBeskyttelse(
            personIdent = it.key,
            adressebeskyttelse = it.value.adressebeskyttelse.gradering(),
        )
    }

@JvmName("pdlPersonKortTilPersonMedAdresseBeskyttelse")
fun Map<String, PdlPersonKort>.tilPersonMedAdresseBeskyttelse() =
    this.map {
        PersonMedAdresseBeskyttelse(
            personIdent = it.key,
            adressebeskyttelse = it.value.adressebeskyttelse.gradering(),
        )
    }
