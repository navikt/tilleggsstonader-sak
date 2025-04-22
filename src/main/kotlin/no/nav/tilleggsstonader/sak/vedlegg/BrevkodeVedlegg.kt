package no.nav.tilleggsstonader.sak.vedlegg

enum class BrevkodeVedlegg(
    val kode: String,
) {
    /**
     * Innsendingskvittering sendes inn med søknader fra FyllUt/SendInn men er ikke interessante å vise i vår saksbehandling
     */
    INNSENDINGSKVITTERING("L7"),
}
