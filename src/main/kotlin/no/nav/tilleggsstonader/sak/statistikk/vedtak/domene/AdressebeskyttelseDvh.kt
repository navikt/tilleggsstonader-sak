package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.AdressebeskyttelseGradering

enum class AdressebeskyttelseDvh {
    STRENGT_FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
    FORTROLIG,
    UGRADERT,
    ;

    companion object {
        fun fraDomene(adressebeskyttelse: AdressebeskyttelseGradering) =
            when (adressebeskyttelse) {
                AdressebeskyttelseGradering.STRENGT_FORTROLIG -> STRENGT_FORTROLIG
                AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND -> STRENGT_FORTROLIG_UTLAND
                AdressebeskyttelseGradering.FORTROLIG -> FORTROLIG
                AdressebeskyttelseGradering.UGRADERT -> UGRADERT
            }
    }
}