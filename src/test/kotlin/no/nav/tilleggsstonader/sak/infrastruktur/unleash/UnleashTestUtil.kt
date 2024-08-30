package no.nav.tilleggsstonader.sak.infrastruktur.unleash

import io.getunleash.Variant
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.libs.unleash.UnleashService

fun mockUnleashService(isEnabled: Boolean = true): UnleashService {
    val mockk = mockk<UnleashService>()
    resetMock(mockk, isEnabled)

    // Variants må konfigureres en og en då de kan ha ulike navn som er relevant å konfigurere
    // eks
    // mockk.mockGetVariant(Toggle.SØKNAD_ROUTING_TILSYN_BARN, søknadRoutingVariant())
    return mockk
}

fun resetMock(mockk: UnleashService, isEnabled: Boolean = true) {
    clearMocks(mockk)
    every { mockk.isEnabled(any()) } returns isEnabled
    every { mockk.isEnabled(Toggle.VILKÅR_PERIODISERING) } returns true
    every { mockk.isEnabled(any(), any<Boolean>()) } returns isEnabled
}

fun UnleashService.mockIsEnabled(toggle: Toggle, value: Boolean = false): UnleashService {
    val service = this
    every { service.isEnabled(toggle) } returns value
    every { service.isEnabled(toggle, any<Boolean>()) } returns value
    return this
}

fun UnleashService.mockGetVariant(toggle: Toggle, variant: Variant): UnleashService {
    val service = this
    every { service.getVariant(toggle) } returns variant
    every { service.getVariant(toggle, any()) } returns variant
    return this
}
