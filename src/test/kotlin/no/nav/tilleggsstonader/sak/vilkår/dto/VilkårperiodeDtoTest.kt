package no.nav.tilleggsstonader.sak.vilkår.dto

import io.mockk.mockk
import no.nav.tilleggsstonader.sak.vilkår.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårperiodeDtoTest {

    @Test
    fun `skal validere at periode er gyldig`() {
        assertThatThrownBy {
            VilkårperiodeDto(
                fom = LocalDate.now(),
                tom = LocalDate.now().minusDays(1),
                type = MålgruppeType.AAP,
                vilkår = mockk(),
            )
        }.hasMessageContaining("Til-og-med før fra-og-med")
    }
}
