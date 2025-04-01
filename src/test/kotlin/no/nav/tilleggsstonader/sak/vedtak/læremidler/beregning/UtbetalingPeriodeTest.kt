package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.tilStønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class UtbetalingPeriodeTest {
    private val førsteJanuar = LocalDate.of(2025, 1, 1)
    private val sisteJanuar = LocalDate.of(2025, 1, 31)

    val grunnlagForUtbetalingPeriode =
        LøpendeMåned(
            fom = førsteJanuar,
            tom = sisteJanuar,
            utbetalingsdato = førsteJanuar,
        )

    val stønadsperiode =
        stønadsperiode(
            behandlingId = BehandlingId.random(),
            fom = førsteJanuar,
            tom = sisteJanuar,
        ).tilStønadsperiodeBeregningsgrunnlag()

    val aktivitet =
        AktivitetLæremidlerBeregningGrunnlag(
            id = UUID.randomUUID(),
            type = AktivitetType.TILTAK,
            fom = førsteJanuar,
            tom = sisteJanuar,
            prosent = 100,
            studienivå = Studienivå.HØYERE_UTDANNING,
        )

    @Test
    fun `skal bruke tom fra siste vedtaksperiode for en utbetalingsperiode`() {
        val jan5 = LocalDate.of(2025, 1, 5)

        val vedtaksperiode = VedtaksperiodeInnenforLøpendeMåned(fom = førsteJanuar, tom = jan5)

        val utbetalingPeriode =
            UtbetalingPeriode(
                løpendeMåned = grunnlagForUtbetalingPeriode.medVedtaksperiode(vedtaksperiode),
                målgruppeOgAktivitet = MålgruppeOgAktivitet(stønadsperiode.målgruppe.faktiskMålgruppe(), aktivitet),
            )

        assertThat(utbetalingPeriode.fom).isEqualTo(førsteJanuar)
        assertThat(utbetalingPeriode.tom).isEqualTo(jan5)
    }
}
