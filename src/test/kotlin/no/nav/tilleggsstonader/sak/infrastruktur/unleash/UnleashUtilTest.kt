package no.nav.tilleggsstonader.sak.infrastruktur.unleash

import io.getunleash.Variant
import io.mockk.every
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.UnleashUtil.getVariantWithNameOrDefault
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class UnleashUtilTest {
    val unleashService = mockUnleashService()

    @Nested
    inner class GetVariantWithNameOrDefaultInt {
        @Test
        fun `skal returnere default value hvis enabled = false`() {
            every { unleashService.getVariant(any()) } returns Variant.DISABLED_VARIANT
            assertThat(getVariant()).isEqualTo(variantDefaultValue)
        }

        @Test
        fun `skal returnere default value er null`() {
            mockVariant(verdi = null)
            assertThat(getVariant()).isEqualTo(variantDefaultValue)
        }

        @Test
        fun `skal returnere verdi`() {
            mockVariant(verdi = "10")
            assertThat(getVariant()).isEqualTo(10)
        }

        @Test
        fun `skal kaste feil hvis varianten er enabled men navnet er feil`() {
            mockVariant(name = "feilNavn")
            val feil = "Fant variant med annet navn for ${toggle.name} forventet=antall faktisk=feilNavn"

            assertThatThrownBy {
                getVariant()
            }.hasMessageContaining(feil)
        }

        @Test
        fun `skal kaste feil hvis verdiet ikke er nummer`() {
            listOf("", "abc", "abc10").forEach {
                mockVariant(verdi = it)
                assertThatThrownBy {
                    getVariant()
                }.isInstanceOf(NumberFormatException::class.java)
            }
        }

        fun mockVariant(
            name: String = variantName,
            verdi: String? = null,
        ) {
            every { unleashService.getVariant(toggle) } returns Variant(name, verdi, true)
        }

        private val variantName = "antall"
        private val variantDefaultValue = 1
        private val toggle = Toggle.entries.first()

        private fun getVariant() = unleashService.getVariantWithNameOrDefault(toggle, variantName, variantDefaultValue)
    }
}
