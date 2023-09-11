package no.nav.tilleggsstonader.sak.infrastruktur

import org.springframework.context.annotation.Profile
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.lang.Nullable
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

@Profile("!integrasjonstest")
@ControllerAdvice
class RessursAdvice : ResponseBodyAdvice<Any?> {
    override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>): Boolean {
        return true
    }

    override fun beforeBodyWrite(
        @Nullable body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
    ): Any? {
        return Ressurs.success(body)
    }
}

data class Ressurs<T>(
    val data: T?,
    val status: Status,
    val melding: String,
    val frontendFeilmelding: String? = null,
    val stacktrace: String?,
) {

    enum class Status {
        SUKSESS,
    }

    companion object {
        fun <T> success(data: T): Ressurs<T> = Ressurs(
            data = data,
            status = Status.SUKSESS,
            melding = "Innhenting av data var vellykket",
            stacktrace = null,
        )
    }

    override fun toString(): String {
        return "Ressurs(status=$status, melding='$melding')"
    }

    fun toSecureString(): String {
        return "Ressurs(status=$status, melding='$melding', frontendFeilmelding='$frontendFeilmelding')"
    }
}
