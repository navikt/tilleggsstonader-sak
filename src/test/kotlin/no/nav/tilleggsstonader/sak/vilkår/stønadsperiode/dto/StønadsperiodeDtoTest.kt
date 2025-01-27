package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto

import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeStatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.util.UUID

class StønadsperiodeDtoTest {
    @Test
    fun `skal validere at periode er gyldig`() {
        assertThatThrownBy {
            StønadsperiodeDto(
                id = UUID.randomUUID(),
                fom = osloDateNow(),
                tom = osloDateNow().minusDays(1),
                målgruppe = MålgruppeType.AAP,
                aktivitet = AktivitetType.TILTAK,
                status = StønadsperiodeStatus.NY,
            )
        }.hasMessageContaining("Til-og-med før fra-og-med")
    }
}
