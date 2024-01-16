package no.nav.tilleggsstonader.sak.vilkår.dto

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class StønadsperiodeDtoTest {

    @Test
    fun `skal validere at periode er gyldig`() {
        assertThatThrownBy {
            StønadsperiodeDto(
                id = UUID.randomUUID(),
                fom = LocalDate.now(),
                tom = LocalDate.now().minusDays(1),
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.TILTAK,
            )
        }.hasMessageContaining("Til-og-med før fra-og-med")
    }
}
