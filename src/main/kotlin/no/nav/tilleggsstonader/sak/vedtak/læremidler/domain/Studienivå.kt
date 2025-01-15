package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

/**
 * @param prioritet lavest er den som har høyest prioritet
 */
enum class Studienivå(val prioritet: Int) {
    HØYERE_UTDANNING(0),
    VIDEREGÅENDE(1),
}
