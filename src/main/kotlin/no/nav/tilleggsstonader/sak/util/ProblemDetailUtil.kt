package no.nav.tilleggsstonader.sak.util

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tilleggsstonader.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ProblemDetailException
import org.springframework.http.ProblemDetail
import org.springframework.web.client.RestClientResponseException

object ProblemDetailUtil {

    fun <T> execWithErrorHandler(fn: () -> T): T {
        try {
            return fn()
        } catch (e: RestClientResponseException) {
            readProblemDetail(e)?.let { throw ProblemDetailException(it, e) } ?: throw e
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
