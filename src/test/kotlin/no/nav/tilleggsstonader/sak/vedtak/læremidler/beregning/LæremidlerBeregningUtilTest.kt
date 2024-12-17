package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerBeregningUtil.beregnBeløp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LæremidlerBeregningUtilTest {
    @Test
    fun `finn beløp for stuieprosent 100%`() {
        val beløp = beregnBeløp(875, 100)
        assertThat(beløp).isEqualTo(875)
    }

    @Test
    fun `finn beløp for stuieprosent 50%`() {
        val beløp = beregnBeløp(874, 50)
        assertThat(beløp).isEqualTo(437)
    }

    @Test
    fun `finn beløp for stuieprosent 50 runder opp%`() {
        val beløp = beregnBeløp(875, 50)
        assertThat(beløp).isEqualTo(438)
    }
}
