package no.nav.tilleggsstonader.sak.infrastruktur.exception

import org.springframework.http.HttpStatus
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Brukes primært som feil som er årsaket av en saksbehandler, som logges som info, og feil blir logge i vanlig logg
 */
open class ApiFeil(
    val feil: String,
    val frontendFeilmelding: String = feil,
    val httpStatus: HttpStatus,
) : RuntimeException(feil) {
    constructor(feil: String, httpStatus: HttpStatus) :
        this(feil = feil, frontendFeilmelding = feil, httpStatus = httpStatus)
}

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
inline fun feilHvis(
    boolean: Boolean,
    httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    noinline sensitivFeilmelding: (() -> String)? = null,
    lazyMessage: () -> String,
) {
    contract {
        returns() implies !boolean
    }
    if (boolean) {
        throw Feil(
            message = lazyMessage(),
            frontendFeilmelding = sensitivFeilmelding?.invoke() ?: lazyMessage(),
            httpStatus = httpStatus,
        )
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun brukerfeil(
    feil: String,
    httpStatus: HttpStatus = HttpStatus.BAD_REQUEST,
): Nothing = throw ApiFeil(feil = feil, httpStatus = httpStatus)

@OptIn(ExperimentalContracts::class)
inline fun brukerfeilHvis(
    boolean: Boolean,
    httpStatus: HttpStatus = HttpStatus.BAD_REQUEST,
    noinline sensitivFeilmelding: (() -> String)? = null,
    lazyMessage: () -> String,
) {
    contract {
        returns() implies !boolean
    }
    if (boolean) {
        throw ApiFeil(
            feil = lazyMessage(),
            frontendFeilmelding = sensitivFeilmelding?.invoke() ?: lazyMessage(),
            httpStatus = httpStatus,
        )
    }
}

inline fun feilHvisIkke(
    boolean: Boolean,
    httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    noinline sensitivFeilmelding: (() -> String)? = null,
    lazyMessage: () -> String,
) {
    feilHvis(!boolean, httpStatus, sensitivFeilmelding) { lazyMessage() }
}

inline fun brukerfeilHvisIkke(
    boolean: Boolean,
    httpStatus: HttpStatus = HttpStatus.BAD_REQUEST,
    noinline sensitivFeilmelding: (() -> String)? = null,
    lazyMessage: () -> String,
) {
    brukerfeilHvis(!boolean, httpStatus, sensitivFeilmelding) { lazyMessage() }
}

class ManglerTilgang(val melding: String, val frontendFeilmelding: String) : RuntimeException(melding)
