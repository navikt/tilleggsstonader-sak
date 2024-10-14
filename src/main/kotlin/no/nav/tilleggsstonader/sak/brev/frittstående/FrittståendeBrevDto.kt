package no.nav.tilleggsstonader.sak.brev.frittstående

data class FrittståendeBrevDto(
    val pdf: ByteArray,
    // val mottakere: BrevmottakereDto,
    val tittel: String,
)
