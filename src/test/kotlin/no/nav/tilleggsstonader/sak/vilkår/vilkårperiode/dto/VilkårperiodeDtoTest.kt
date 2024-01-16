package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeDomainUtil.målgruppe
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårperiodeDtoTest {

    @Test
    fun `skal validere at periode er gyldig`() {
        assertThatThrownBy {
            målgruppe(
                fom = LocalDate.now(),
                tom = LocalDate.now().minusDays(1),
            ).tilDto()
        }.hasMessageContaining("Til-og-med før fra-og-med")
    }
}
