package no.nav.tilleggsstonader.sak.infrastruktur.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus

@ControllerAdvice
class ApiExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(Throwable::class)
    fun handleThrowable(throwable: Throwable): ProblemDetail {
        val responseStatus = throwable::class.annotations.find { it is ResponseStatus }
            ?.let { it as ResponseStatus }
            ?.value
            ?: HttpStatus.INTERNAL_SERVER_ERROR
        logger.error("Ukjent feil status=${responseStatus.value()}")
        // TODO securelogger når vi har tilgang
        logger.error("Ukjent feil status=${responseStatus.value()}", throwable)
        return ProblemDetail.forStatusAndDetail(responseStatus, "Ukjent feil")
    }
}
