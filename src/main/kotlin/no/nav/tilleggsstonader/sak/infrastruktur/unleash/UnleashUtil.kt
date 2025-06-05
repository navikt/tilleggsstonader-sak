package no.nav.tilleggsstonader.sak.infrastruktur.unleash

import no.nav.tilleggsstonader.libs.unleash.ToggleId
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import kotlin.jvm.optionals.getOrNull

object UnleashUtil {
    fun UnleashService.getVariantWithNameOrDefault(
        toggle: ToggleId,
        name: String,
        defaultValue: Int,
    ): Int {
        val variant = getVariant(toggle)
        return if (variant.isEnabled) {
            feilHvis(variant.name != name) {
                "Fant variant med annet navn for $toggle forventet=$name faktisk=${variant.name}"
            }
            variant.payload
                .getOrNull()
                ?.value
                ?.toInt() ?: defaultValue
        } else {
            defaultValue
        }
    }
}
