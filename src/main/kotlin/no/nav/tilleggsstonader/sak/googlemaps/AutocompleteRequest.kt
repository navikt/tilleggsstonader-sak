package no.nav.tilleggsstonader.sak.googlemaps

data class AutocompleteRequest(
    val input: String,
    val includedRegionCodes: List<String>,
    val languageCode: String,
    val regionCode: String,
    val includedPrimaryTypes: List<String>,
)
