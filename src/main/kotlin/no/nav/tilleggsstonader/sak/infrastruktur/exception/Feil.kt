package no.nav.tilleggsstonader.sak.infrastruktur.exception

import org.springframework.http.HttpStatus
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Brukes primært som feil som er årsaket av en saksbehandler, som logges som info, og feil blir logge i vanlig logg
 */
open class ApiFeil(val feil: String, val httpStatus: HttpStatus) : RuntimeException(feil)

/**
 * Generelle feil. Logger som Error.
 * @param frontendFeilmelding logges kun i securelog
 */
class Feil(
    message: String,
    val frontendFeilmelding: String = message,
    val httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    throwable: Throwable? = null,
) : RuntimeException(message, throwable)

@OptIn(ExperimentalContracts::class)
inline fun feilHvis(boolean: Boolean, httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR, lazyMessage: () -> String) {
    contract {
        returns() implies !boolean
    }
    if (boolean) {
        throw Feil(message = lazyMessage(), frontendFeilmelding = lazyMessage(), httpStatus)
    }
}

@OptIn(ExperimentalContracts::class)
inline fun brukerfeilHvis(boolean: Boolean, httpStatus: HttpStatus = HttpStatus.BAD_REQUEST, lazyMessage: () -> String) {
    contract {
        returns() implies !boolean
    }
    if (boolean) {
        throw ApiFeil(feil = lazyMessage(), httpStatus = httpStatus)
    }
}

inline fun feilHvisIkke(boolean: Boolean, httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR, lazyMessage: () -> String) {
    feilHvis(!boolean, httpStatus) { lazyMessage() }
}

inline fun brukerfeilHvisIkke(boolean: Boolean, httpStatus: HttpStatus = HttpStatus.BAD_REQUEST, lazyMessage: () -> String) {
    brukerfeilHvis(!boolean, httpStatus) { lazyMessage() }
}

class ManglerTilgang(val melding: String, val frontendFeilmelding: String) : RuntimeException(melding)
