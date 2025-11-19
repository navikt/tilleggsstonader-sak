package no.nav.tilleggsstonader.sak.googlemaps

data class FinnReiseAvstandDto(
    val fraAdresse: Adresse,
    val tilAdresse: Adresse,
)

data class Adresse(
    val gate: String,
    val postnummer: String,
    val poststed: String,
) {
    fun tilSøkeString() = "$gate, $postnummer, $poststed"
}
