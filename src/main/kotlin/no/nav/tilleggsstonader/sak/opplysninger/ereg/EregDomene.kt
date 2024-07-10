package no.nav.tilleggsstonader.sak.opplysninger.ereg

data class OrganisasjonDto(
    val organisasjonsnummer: String,
    val navn: Navn,
)

data class Navn(
    val navnelinje1: String?,
)
