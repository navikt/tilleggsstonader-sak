package no.nav.tilleggsstonader.sak.util

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.security.mock.oauth2.http.objectMapper
import no.nav.tilleggsstonader.libs.http.client.ProblemDetailException
import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import org.springframework.http.ProblemDetail
import org.springframework.web.client.RestClientResponseException

object ProblemDetailUtil {

    fun catchProblemDetailException(fn: () -> Unit): ProblemDetailException {
        return catchThrowableOfType<ProblemDetailException> {
            execWithErrorHandler(fn)
        }
    }

    fun catchHttpException(fn: () -> Unit): RestClientResponseException {
        return catchThrowableOfType<RestClientResponseException> {
            execWithErrorHandler(fn)
        }
    }

    fun <T> execWithErrorHandler(fn: () -> T): T {
        try {
            return fn()
        } catch (e: RestClientResponseException) {
            readProblemDetail(e)?.let { throw ProblemDetailException(it, e) }
                ?: throw e
        }
    }

    private fun readProblemDetail(e: RestClientResponseException): ProblemDetail? {
        val responseBody = e.responseBodyAsString // "detail"
        return if (responseBody.contains("\"detail\"")) {
            objectMapper.readValue<ProblemDetail>(responseBody)
        } else {
            null
        }
    }
}
