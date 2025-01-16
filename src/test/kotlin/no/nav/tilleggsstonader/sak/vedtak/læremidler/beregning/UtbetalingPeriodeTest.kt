package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.tilStønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class UtbetalingPeriodeTest {

    private val JAN_FØRSTE = LocalDate.of(2025, 1, 1)
    private val JAN_SISTE = LocalDate.of(2025, 1, 31)

    val grunnlagForUtbetalingPeriode = LøpendeMåned(
        fom = JAN_FØRSTE,
        tom = JAN_SISTE,
        utbetalingsdato = JAN_FØRSTE,
    )

    val stønadsperiode = stønadsperiode(
        behandlingId = BehandlingId.random(),
        fom = JAN_FØRSTE,
        tom = JAN_SISTE,
    ).tilStønadsperiodeBeregningsgrunnlag()

    val aktivitet = AktivitetLæremidlerBeregningGrunnlag(
        id = UUID.randomUUID(),
        type = AktivitetType.TILTAK,
        fom = JAN_FØRSTE,
        tom = JAN_SISTE,
        prosent = 100,
        studienivå = Studienivå.HØYERE_UTDANNING,
    )

    @Test
    fun `skal bruke tom fra siste vedtaksperiode for en utbetalingsperiode`() {
        val jan5 = LocalDate.of(2025, 1, 5)

        val vedtaksperiode = Vedtaksperiode(fom = JAN_FØRSTE, tom = jan5)

        val utbetalingPeriode = UtbetalingPeriode(
            løpendeMåned = grunnlagForUtbetalingPeriode.medVedtaksperiode(vedtaksperiode),
            stønadsperiode = stønadsperiode,
            aktivitet = aktivitet,
        )

        assertThat(utbetalingPeriode.fom).isEqualTo(JAN_FØRSTE)
        assertThat(utbetalingPeriode.tom).isEqualTo(jan5)
    }
}
