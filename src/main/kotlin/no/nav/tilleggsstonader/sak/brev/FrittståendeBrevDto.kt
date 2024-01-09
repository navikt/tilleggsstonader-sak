package no.nav.tilleggsstonader.sak.brev

data class FrittståendeBrevDto(
    val pdf: ByteArray,
    // val mottakere: BrevmottakereDto,
    val tittel: String,
)
