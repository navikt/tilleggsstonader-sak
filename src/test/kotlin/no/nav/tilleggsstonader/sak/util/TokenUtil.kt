package no.nav.tilleggsstonader.sak.util

import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import java.util.UUID

object TokenUtil {
    /**
     * client token
     * oid = unik id på applikasjon A i Azure AD
     * sub = unik id på applikasjon A i Azure AD, alltid lik oid
     */
    fun clientToken(
        mockOAuth2Server: MockOAuth2Server,
        clientId: String,
        accessAsApplication: Boolean,
    ): String {
        val thisId = UUID.randomUUID().toString()

        val claims =
            mapOf(
                "oid" to thisId,
                "azp" to clientId,
                "azp_name" to clientId,
                "roles" to if (accessAsApplication) listOf("access_as_application") else emptyList(),
            )

        return mockOAuth2Server
            .issueToken(
                issuerId = "azuread",
                subject = thisId,
                audience = "aud-localhost",
                claims = claims,
            ).serialize()
    }

    /**
     * On behalf
     * oid = unik id på brukeren i Azure AD
     * sub = unik id på brukeren i kombinasjon med applikasjon det ble logget inn i
     */
    fun onBehalfOfToken(
        mockOAuth2Server: MockOAuth2Server,
        roles: List<String>,
        saksbehandler: String,
    ): String {
        val clientId = UUID.randomUUID().toString()
        val brukerId = UUID.randomUUID().toString()

        val claims =
            mapOf(
                "oid" to brukerId,
                "azp" to clientId,
                "name" to saksbehandler,
                "NAVident" to saksbehandler,
                "groups" to roles,
            )

        return mockOAuth2Server
            .issueToken(
                issuerId = "azuread",
                subject = UUID.randomUUID().toString(),
                audience = "aud-localhost",
                claims = claims,
            ).serialize()
    }

    fun tokenXToken(
        mockOAuth2Server: MockOAuth2Server,
        subject: String,
        issuerId: String = "tokenx",
        clientId: String = UUID.randomUUID().toString(),
        audience: String = "aud-localhost",
        claims: Map<String, Any> =
            mapOf(
                "acr" to "Level4",
                "pid" to subject,
            ),
    ): String =
        mockOAuth2Server
            .issueToken(
                issuerId,
                clientId,
                DefaultOAuth2TokenCallback(
                    issuerId = issuerId,
                    subject = subject,
                    audience = listOf(audience),
                    claims = claims,
                    expiry = 3600,
                ),
            ).serialize()
}
