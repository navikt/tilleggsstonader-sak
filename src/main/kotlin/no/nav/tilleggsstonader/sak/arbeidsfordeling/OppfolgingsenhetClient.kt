package no.nav.tilleggsstonader.sak.arbeidsfordeling

import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import java.net.URI

@Component
class OppfolgingsenhetClient(
    @Value("\${clients.veilarboppfolging.uri}")
    private val veilarboppfolgingUri: URI,
    @Value("\${NAIS_APP_NAME}")
    private val navConsumerId: String,
    @Qualifier("restClientAzure") private val restClient: RestClient,
) {
    fun hentOppfølgingsenhet(fnr: String): String? {
        val request =
            OppfolgingsenhetRequest(
                variables = OppfolgingsenhetRequestVariables(fnr = fnr),
                query = OppfolgingsenhetConfig.oppfolgingsenhetQuery,
            )

        val response =
            restClient
                .post()
                .uri("$veilarboppfolgingUri/veilarboppfolging/api/graphql")
                .body(request)
                .headers { it["Nav-Consumer-Id"] = navConsumerId }
                .retrieve()
                .body<OppfolgingsenhetResponse>()

        if (response == null) {
            error("Fikk tom respons ved oppslag av oppfølgingsenhet")
        }

        if (!response.errors.isNullOrEmpty()) {
            secureLogger.error("Feil ved henting av oppfolgingsenhet: ${response.errors.joinToString { it.message }}")
            error("Feil ved henting av oppfolgingsenhet")
        }

        val data = response.data ?: error("Data er null fra oppfolgingsenhet")
        return data.oppfolgingsEnhet?.enhet?.id
    }
}

private object OppfolgingsenhetConfig {
    val oppfolgingsenhetQuery = graphqlQuery()

    private fun graphqlQuery() =
        OppfolgingsenhetConfig::class.java
            .getResource("/arbeidsfordeling/oppfolgingsenhet.graphql")!!
            .readText()
            .graphqlCompatible()

    private fun String.graphqlCompatible(): String = StringUtils.normalizeSpace(this.replace("\n", ""))
}

data class OppfolgingsenhetRequest(
    val variables: OppfolgingsenhetRequestVariables,
    val query: String,
)

data class OppfolgingsenhetRequestVariables(
    val fnr: String,
)

data class OppfolgingsenhetResponse(
    val data: OppfolgingsenhetData?,
    val errors: List<OppfolgingsenhetError>?,
)

data class OppfolgingsenhetError(
    val message: String,
)

data class OppfolgingsenhetData(
    val oppfolgingsEnhet: OppfolgingsenhetResultat?,
)

data class OppfolgingsenhetResultat(
    val enhet: Oppfolgingsenhet?,
)

data class Oppfolgingsenhet(
    val id: String?,
    val navn: String,
    val kilde: String,
)
