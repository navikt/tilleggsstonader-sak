package no.nav.tilleggsstonader.sak.infrastruktur.exception

import no.nav.security.token.support.core.exceptions.JwtTokenMissingException
import no.nav.tilleggsstonader.sak.infrastruktur.config.SecureLogger
import org.slf4j.LoggerFactory
import org.springframework.core.NestedExceptionUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

@ControllerAdvice
class ApiExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val secureLogger = SecureLogger.secureLogger

    @ExceptionHandler(Throwable::class)
    fun handleThrowable(throwable: Throwable): ProblemDetail {
        val responseStatus = throwable::class.annotations.find { it is ResponseStatus }
            ?.let { it as ResponseStatus }
            ?.value
            ?: HttpStatus.INTERNAL_SERVER_ERROR

        val metodeSomFeiler = finnMetodeSomFeiler(throwable)

        val mostSpecificCause = throwable.getMostSpecificCause()
        if (mostSpecificCause is SocketTimeoutException || mostSpecificCause is TimeoutException) {
            secureLogger.warn(
                "Timeout feil: ${mostSpecificCause.message}, $metodeSomFeiler ${rootCause(throwable)}",
                throwable,
            )
            logger.warn("Timeout feil: $metodeSomFeiler ${rootCause(throwable)} ")
            return lagTimeoutfeilRessurs()
        }

        secureLogger.error("Uventet feil: $metodeSomFeiler ${rootCause(throwable)}", throwable)
        logger.error("Uventet feil: $metodeSomFeiler ${rootCause(throwable)} ")

        logger.error("Ukjent feil status=${responseStatus.value()}")
        // TODO securelogger når vi har tilgang
        secureLogger.error("Ukjent feil status=${responseStatus.value()}", throwable)
        return ProblemDetail.forStatusAndDetail(responseStatus, "Ukjent feil")
    }

    @ExceptionHandler(JwtTokenMissingException::class)
    fun handleJwtTokenMissingException(jwtTokenMissingException: JwtTokenMissingException): ProblemDetail {
        return ProblemDetail.forStatusAndDetail(
            HttpStatus.UNAUTHORIZED,
            "En uventet feil oppstod: Kall ikke autorisert",
        )
    }

    @ExceptionHandler(ApiFeil::class)
    fun handleThrowable(feil: ApiFeil): ProblemDetail {
        val metodeSomFeiler = finnMetodeSomFeiler(feil)
        secureLogger.info("En håndtert feil har oppstått(${feil.httpStatus}): ${feil.feil}", feil)
        logger.info(
            "En håndtert feil har oppstått(${feil.httpStatus}) " +
                "metode=$metodeSomFeiler exception=${rootCause(feil)}: ${feil.message} ",
        )
        return ProblemDetail.forStatusAndDetail(feil.httpStatus, feil.feil)
    }

    @ExceptionHandler(Feil::class)
    fun handleThrowable(feil: Feil): ProblemDetail {
        val metodeSomFeiler = finnMetodeSomFeiler(feil)
        secureLogger.error("En håndtert feil har oppstått(${feil.httpStatus}): ${feil.frontendFeilmelding}", feil)
        logger.error(
            "En håndtert feil har oppstått(${feil.httpStatus}) " +
                "metode=$metodeSomFeiler exception=${rootCause(feil)}: ${feil.message} ",
        )
        return ProblemDetail.forStatusAndDetail(feil.httpStatus, feil.frontendFeilmelding)
    }

    @ExceptionHandler(PdlNotFoundException::class)
    fun handleThrowable(feil: PdlNotFoundException): ProblemDetail {
        logger.warn("Finner ikke personen i PDL")
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Finner ingen personer for valgt personident")
    }

    @ExceptionHandler(ManglerTilgang::class)
    fun handleThrowable(manglerTilgang: ManglerTilgang): ProblemDetail {
        secureLogger.warn("En håndtert tilgangsfeil har oppstått - ${manglerTilgang.melding}", manglerTilgang)
        logger.warn("En håndtert tilgangsfeil har oppstått")
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, manglerTilgang.frontendFeilmelding)
    }

    @ExceptionHandler(IntegrasjonException::class)
    fun handleThrowable(feil: IntegrasjonException): ProblemDetail {
        secureLogger.error("Feil mot integrasjonsclienten har oppstått: uri={} data={}", feil.uri, feil.data, feil)
        logger.error("Feil mot integrasjonsclienten har oppstått exception=${rootCause(feil)}")
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, feil.message)
    }

    private fun lagTimeoutfeilRessurs(): ProblemDetail = ProblemDetail.forStatusAndDetail(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Kommunikasjonsproblemer med andre systemer - prøv igjen",
    )

    fun finnMetodeSomFeiler(e: Throwable): String {
        val firstElement = e.stackTrace.firstOrNull {
            it.className.startsWith("no.nav.tilleggsstonader.sak") &&
                !it.className.contains("$") &&
                !it.className.contains("InsertUpdateRepositoryImpl")
        }
        if (firstElement != null) {
            val className = firstElement.className.split(".").lastOrNull()
            return "$className::${firstElement.methodName}(${firstElement.lineNumber})"
        }
        return e.cause?.let { finnMetodeSomFeiler(it) } ?: "(Ukjent metode som feiler)"
    }

    private fun rootCause(throwable: Throwable): String {
        return throwable.getMostSpecificCause().javaClass.simpleName
    }

    private fun Throwable.getMostSpecificCause(): Throwable {
        return NestedExceptionUtils.getMostSpecificCause(this)
    }
}
