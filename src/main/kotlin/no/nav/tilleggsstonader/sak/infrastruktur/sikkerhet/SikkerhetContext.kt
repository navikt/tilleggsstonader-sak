package no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet

import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.exceptions.JwtTokenMissingException
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.slf4j.LoggerFactory

object SikkerhetContext {

    private val logger = LoggerFactory.getLogger(javaClass)

    private const val SYSTEM_NAVN = "System"
    const val SYSTEM_FORKORTELSE = "VL" // Vedtaksløsning

    val NAVIDENT_REGEX = """^[a-zA-Z]\d{6}$""".toRegex()

    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun hentToken() =
        SpringTokenValidationContextHolder().getTokenValidationContext().getJwtToken("azuread")
            ?: throw JwtTokenMissingException()

    fun erMaskinTilMaskinToken(): Boolean {
        val claims = SpringTokenValidationContextHolder().getTokenValidationContext().getClaims("azuread")
        return claims.get("oid") != null &&
            claims.get("oid") == claims.get("sub") &&
            claims.getAsList("roles").contains("access_as_application")
    }

    fun kallKommerFra(eksternApplikasjon: EksternApplikasjon): Boolean {
        val claims = SpringTokenValidationContextHolder().getTokenValidationContext().getClaims("azuread")
        val applikasjonsnavn = claims.get("azp_name")?.toString() ?: "" // e.g. dev-gcp:some-team:application-name
        secureLogger.info("Applikasjonsnavn: $applikasjonsnavn")
        return applikasjonsnavn.endsWith(eksternApplikasjon.namespaceAppNavn)
    }

    fun hentSaksbehandler(): String {
        val result = hentSaksbehandlerEllerSystembruker()

        if (result == SYSTEM_FORKORTELSE) {
            error("Finner ikke NAVident i token")
        }
        return result
    }

    fun erSaksbehandler(): Boolean = hentSaksbehandlerEllerSystembruker() != SYSTEM_FORKORTELSE

    fun hentSaksbehandlerEllerSystembruker() =
        Result.runCatching { SpringTokenValidationContextHolder().getTokenValidationContext() }
            .fold(
                onSuccess = {
                    it.getClaim("NAVident")?.toString() ?: SYSTEM_FORKORTELSE
                },
                onFailure = { SYSTEM_FORKORTELSE },
            )

    fun hentSaksbehandlerNavn(strict: Boolean = false): String {
        return Result.runCatching { SpringTokenValidationContextHolder().getTokenValidationContext() }
            .fold(
                onSuccess = {
                    it.getClaim("name")?.toString()
                        ?: if (strict) error("Finner ikke navn i azuread token") else SYSTEM_NAVN
                },
                onFailure = { if (strict) error("Finner ikke navn på innlogget bruker") else SYSTEM_NAVN },
            )
    }

    fun hentGrupperFraToken(): Set<String> {
        return Result.runCatching { SpringTokenValidationContextHolder().getTokenValidationContext() }
            .fold(
                onSuccess = {
                    @Suppress("UNCHECKED_CAST")
                    val groups = it.getClaim("groups") as List<String>?
                    groups?.toSet() ?: emptySet()
                },
                onFailure = { emptySet() },
            )
    }

    private fun TokenValidationContext.getClaim(name: String) =
        this.getJwtToken("azuread")?.jwtTokenClaims?.get(name)

    /**
     * Denne sjekker at contexten har tilgang til påkrevd rolle
     * Hvis man gjør et kall uten token på en controller med @Unprotected så får man automatiskt systemrolle
     * Av den grunnen burde ikke vanlige controllers ha @Unprotected, men faktiskt validere kall
     */
    fun harTilgangTilGittRolle(rolleConfig: RolleConfig, minimumsrolle: BehandlerRolle): Boolean {
        val rollerFraToken = hentGrupperFraToken()
        val rollerForBruker = when {
            hentSaksbehandlerEllerSystembruker() == SYSTEM_FORKORTELSE -> listOf(
                BehandlerRolle.SYSTEM,
                BehandlerRolle.BESLUTTER,
                BehandlerRolle.SAKSBEHANDLER,
                BehandlerRolle.VEILEDER,
            )

            rollerFraToken.contains(rolleConfig.beslutterRolle) -> listOf(
                BehandlerRolle.BESLUTTER,
                BehandlerRolle.SAKSBEHANDLER,
                BehandlerRolle.VEILEDER,
            )

            rollerFraToken.contains(rolleConfig.saksbehandlerRolle) -> listOf(
                BehandlerRolle.SAKSBEHANDLER,
                BehandlerRolle.VEILEDER,
            )

            rollerFraToken.contains(rolleConfig.veilederRolle) -> listOf(BehandlerRolle.VEILEDER)
            else -> listOf(BehandlerRolle.UKJENT)
        }
        if (logger.isDebugEnabled) {
            logger.debug("Roller for kall=$rollerForBruker")
        }

        return rollerForBruker.contains(minimumsrolle)
    }
}
