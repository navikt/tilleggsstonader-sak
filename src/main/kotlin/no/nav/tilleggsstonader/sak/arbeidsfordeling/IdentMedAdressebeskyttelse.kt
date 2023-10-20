package no.nav.tilleggsstonader.sak.arbeidsfordeling

import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering


data class IdentMedAdressebeskyttelse(
    val ident: String,
    val adressebeskyttelsegradering: AdressebeskyttelseGradering?,
)
