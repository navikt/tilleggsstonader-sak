package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil.rammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class RammeForReiseMedPrivatBilTest {
    @Test
    fun `finnMålgruppeForReiseperiode returnerer målgruppe når beregningsuke spenner over to sammenhengende vedtaksperioder med lik målgruppe og aktivitet`() {
        val ukeFom = 6 januar 2025
        val ukeTom = 12 januar 2025

        val reise =
            rammeForReiseMedPrivatBil(
                fom = ukeFom,
                tom = ukeTom,
                vedtaksperioder =
                    listOf(
                        // Grensen mellom periodene (9. og 10. jan) faller midt i beregningsuken
                        vedtaksperiode(fom = 1 januar 2025, tom = 9 januar 2025, målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE),
                        vedtaksperiode(fom = 10 januar 2025, tom = 19 januar 2025, målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE),
                    ),
            )

        val reiseperiode =
            BeregningsresultatForReisePrivatBilPeriode(
                fom = ukeFom,
                tom = ukeTom,
                grunnlag = BeregningsresultatForReisePrivatBilGrunnlag(dager = emptyList()),
                stønadsbeløp = BigDecimal.ZERO,
                brukersNavKontor = null,
                fraTidligereVedtak = false,
            )

        val målgruppe = reise.finnMålgruppeForReiseperiode(reiseperiode)

        assertThat(målgruppe).isEqualTo(FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE)
    }
}
