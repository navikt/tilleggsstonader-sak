package no.nav.tilleggsstonader.sak.googlemaps.dto

import no.nav.tilleggsstonader.sak.googlemaps.autocompleteApi.AutocompleteResponse

data class AdresseforslagDto(
    val forslag: List<String>?,
)

fun AutocompleteResponse.tilDto(): AdresseforslagDto =
    AdresseforslagDto(
        forslag = this.suggestions?.map { it.placePrediction.text.text },
    )
