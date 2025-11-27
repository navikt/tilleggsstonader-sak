package no.nav.tilleggsstonader.sak.googlemaps

data class AutocompleteResponse(
    val suggestions: List<Suggestion>?,
)

data class Suggestion(
    val placePrediction: PlacePrediction,
)

data class PlacePrediction(
    val text: Text,
)

data class Text(
    val text: String,
)
