package no.nav.tilleggsstonader.sak.infrastruktur.unleash

import io.getunleash.Variant
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.libs.unleash.UnleashService

fun mockUnleashService(enabled: Boolean = true): UnleashService {
    val mockk = mockk<UnleashService>()
    every { mockk.isEnabled(any(), any<Boolean>()) } returns enabled
    every { mockk.getVariant(any(), any()) } returns Variant.DISABLED_VARIANT
    every { this }
    return mockk
}

fun UnleashService.mockSøknadRouting(toggle: Toggle, antall: Int = 10, enabled: Boolean = true) {
    val service = this
    val variant = Variant("antall", antall.toString(), enabled)
    every { service.getVariant(toggle, any()) } returns variant
}
