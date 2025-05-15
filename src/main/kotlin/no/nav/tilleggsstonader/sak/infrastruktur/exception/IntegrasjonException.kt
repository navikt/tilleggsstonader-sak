package no.nav.tilleggsstonader.sak.infrastruktur.exception

import java.net.URI

/**
 * @param hendelse blir med i feilmeldingen til bruker i [ApiExceptionHandler], bør være lowercase. Eks "henting av aktiviteter"
 */
class IntegrasjonException(
    val hendelse: String,
    val tjeneste: IntegrasjonsTjeneste,
    throwable: Throwable? = null,
    val uri: URI? = null,
) : RuntimeException("Feilet $hendelse fra $tjeneste", throwable)

enum class IntegrasjonsTjeneste(
    val visningsnavn: String,
) {
    AKTIVITETER("aktiviteter"),
}
