package no.nav.tilleggsstonader.sak.infrastruktur.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.client.RestClientResponseException

class ProblemDetailException(
    val detail: ProblemDetail,
    val responseException: RestClientResponseException,
    val httpStatus: HttpStatus = HttpStatus.valueOf(responseException.rawStatusCode)
) : RuntimeException(responseException)