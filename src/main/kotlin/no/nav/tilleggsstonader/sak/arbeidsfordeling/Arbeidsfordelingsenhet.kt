package no.nav.tilleggsstonader.sak.arbeidsfordeling

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

data class Arbeidsfordelingsenhet(
    val enhetNr: String,
    val navn: String,
)

// Ignorerer ukjente felter, se https://norg2.intern.dev.nav.no/norg2/swagger-ui/index.html#/enhet/getEnhetByGeografiskOmraade
@JsonIgnoreProperties(ignoreUnknown = true)
data class NavKontor(
    val enhetNr: String,
)
