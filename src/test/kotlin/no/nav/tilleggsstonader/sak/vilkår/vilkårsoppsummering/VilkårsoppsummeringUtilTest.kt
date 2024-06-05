package no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering

import no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering.VilkårsoppsummeringUtil.utledAlderNårStønadsperiodeBegynner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate.now

class VilkårsoppsummeringUtilTest {
    @Nested
    inner class UtledAlderNårStønadsperiodeBegynner {

        @Test
        fun `skal svare med null hvis man mangler dato for stønadsperiode`() {
            assertThat(utledAlderNårStønadsperiodeBegynner(null, null)).isNull()
            assertThat(utledAlderNårStønadsperiodeBegynner(now(), null)).isNull()
            assertThat(utledAlderNårStønadsperiodeBegynner(now().minusYears(3), null)).isNull()
            assertThat(utledAlderNårStønadsperiodeBegynner(now().minusDays(3), null)).isNull()
        }

        @Test
        fun `skal svare med 0 hvis barnet mangler fødselsdato då vi ikke vet alderen til barnet`() {
            assertThat(utledAlderNårStønadsperiodeBegynner(null, now())).isEqualTo(0)

            assertThat(utledAlderNårStønadsperiodeBegynner(null, now().minusYears(3))).isEqualTo(0)
            assertThat(utledAlderNårStønadsperiodeBegynner(null, now().minusDays(3))).isEqualTo(0)

            assertThat(utledAlderNårStønadsperiodeBegynner(null, now().plusYears(3))).isEqualTo(0)
            assertThat(utledAlderNårStønadsperiodeBegynner(null, now().plusDays(3))).isEqualTo(0)
        }

        @Test
        fun `skal returnere barnets alder vid tidspunktet for første stønadsperioden`() {
            assertThat(utledAlderNårStønadsperiodeBegynner(now(), now())).isEqualTo(0)

            val toÅrSiden = now().minusYears(2)
            assertThat(utledAlderNårStønadsperiodeBegynner(toÅrSiden, now())).isEqualTo(2)
            assertThat(utledAlderNårStønadsperiodeBegynner(toÅrSiden.minusDays(1), now())).isEqualTo(2)
            assertThat(utledAlderNårStønadsperiodeBegynner(toÅrSiden.plusDays(1), now())).isEqualTo(1)
        }
    }
}
