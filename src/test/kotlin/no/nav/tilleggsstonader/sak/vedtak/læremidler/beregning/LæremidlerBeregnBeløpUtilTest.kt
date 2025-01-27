package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregnBeløpUtil.beregnBeløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class LæremidlerBeregnBeløpUtilTest {
    @Nested
    inner class BeregnBeløp {
        @Test
        fun `finn beløp for stuieprosent 100 prosent`() {
            val beløp = beregnBeløp(875, 100)
            assertThat(beløp).isEqualTo(875)
        }

        @Test
        fun `finn beløp for stuieprosent 50 prosent`() {
            val beløp = beregnBeløp(874, 50)
            assertThat(beløp).isEqualTo(437)
        }

        @Test
        fun `finn beløp for stuieprosent 50 runder opp prosent`() {
            val beløp = beregnBeløp(875, 50)
            assertThat(beløp).isEqualTo(438)
        }
    }
}
