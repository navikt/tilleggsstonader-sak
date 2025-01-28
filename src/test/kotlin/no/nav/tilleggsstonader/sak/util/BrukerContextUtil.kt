package no.nav.tilleggsstonader.sak.util

import com.nimbusds.jwt.JWTClaimsSet
import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

object BrukerContextUtil {
    fun clearBrukerContext() {
        RequestContextHolder.resetRequestAttributes()
    }

    fun mockBrukerContext(
        preferredUsername: String = "A",
        groups: List<String> = emptyList(),
        servletRequest: HttpServletRequest = MockHttpServletRequest(),
        azp_name: String? = null,
    ) {
        val tokenValidationContext = mockk<TokenValidationContext>()
        val jwtTokenClaims =
            JwtTokenClaims(
                JWTClaimsSet
                    .Builder()
                    .subject(preferredUsername)
                    .claim("preferred_username", preferredUsername)
                    .claim("NAVident", preferredUsername)
                    .claim("name", preferredUsername)
                    .claim("groups", groups)
                    .claim("azp_name", azp_name)
                    .build(),
            )
        val requestAttributes = ServletRequestAttributes(servletRequest)

        RequestContextHolder.setRequestAttributes(requestAttributes)
        requestAttributes.setAttribute(
            SpringTokenValidationContextHolder::class.java.name,
            tokenValidationContext,
            RequestAttributes.SCOPE_REQUEST,
        )
        val jwtToken = mockk<JwtToken>()
        every { jwtToken.subject } returns preferredUsername
        every { jwtToken.jwtTokenClaims } returns jwtTokenClaims
        every { tokenValidationContext.getClaims("azuread") } returns jwtTokenClaims
        every { tokenValidationContext.getJwtToken("azuread") } returns jwtToken
    }

    fun <T> testWithBrukerContext(
        preferredUsername: String = "A",
        groups: List<String> = emptyList(),
        fn: () -> T,
    ): T {
        try {
            mockBrukerContext(preferredUsername, groups)
            return fn()
        } finally {
            clearBrukerContext()
        }
    }
}
