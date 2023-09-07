package no.nav.tilleggsstonader.sak.tilgang

data class Tilgang(
    val harTilgang: Boolean,
    val begrunnelse: String? = null,
)
