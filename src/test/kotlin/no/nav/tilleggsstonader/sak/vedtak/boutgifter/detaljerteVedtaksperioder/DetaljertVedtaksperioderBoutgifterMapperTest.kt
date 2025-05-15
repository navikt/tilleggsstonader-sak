package no.nav.tilleggsstonader.sak.vedtak.boutgifter.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.UtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.finnMakssats
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.detaljerteVedtaksperioder.DetaljertVedtaksperioderBoutgifterMapper.finnDetaljerteVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DetaljertVedtaksperioderBoutgifterMapperTest {
    val førsteJan = LocalDate.of(2024, 1, 1)
    val sisteJan = LocalDate.of(2024, 1, 31)
    val førsteFeb = LocalDate.of(2024, 2, 1)
    val sisteFeb = LocalDate.of(2024, 2, 29)
    /*
     * Overnatting:
     * To utgifter i hver sin 30 dagers periode skal ikke slås sammen (en over makssats og en under)
     *
     * 3 utgifter i samme 30 dagersperiode -> en ender med beløp dekket = 0
     * Sjekke utregning av "beløp som dekkes"
     *
     * Løpende utgifter:
     * Test 1: to mnd med løpende utgifter som skal slås sammen
     *
     * Felles:
     * Sjekke at de kommer som to ulike perioder og ikke slås sammen hvis det finnes to utgifter i samme beregningsmånedresultat
     * assertThat(res.overnatting.utgifter).isEqual(overnattingsutgifter) OBS: skal ikke summere inn løpende utgifter
     * */

    @Test
    fun `test`() {
        val beregningsresultat =
            BoutgifterTestUtil.lagBeregningsresultatMåned(
                fom = førsteJan,
                tom = sisteJan,
                utgifter =
                    mapOf(
                        TypeBoutgift.UTGIFTER_OVERNATTING to
                            listOf(
                                UtgiftBeregningBoutgifter(
                                    fom = LocalDate.of(2024, 1, 3),
                                    tom = LocalDate.of(2024, 1, 4),
                                    utgift = 4000,
                                ),
                                UtgiftBeregningBoutgifter(
                                    fom = LocalDate.of(2024, 1, 7),
                                    tom = LocalDate.of(2024, 1, 8),
                                    utgift = 1000,
                                ),
                                UtgiftBeregningBoutgifter(
                                    fom = LocalDate.of(2024, 1, 10),
                                    tom = LocalDate.of(2024, 1, 11),
                                    utgift = 1000,
                                ),
                            ),
                    ),
            )

        val vedtak =
            InnvilgelseBoutgifter(
                vedtaksperioder = emptyList(),
                beregningsresultat = BeregningsresultatBoutgifter(perioder = listOf(beregningsresultat)),
            )

        val res = vedtak.finnDetaljerteVedtaksperioder()

        assertThat(res).hasSize(1)

        val resJan = res.first()

        assertThat(resJan.fom).isEqualTo(førsteJan)
        assertThat(resJan.tom).isEqualTo(sisteJan)
        assertThat(resJan.erLøpendeUtgift).isFalse
        assertThat(resJan.utgifterTilOvernatting).hasSize(3)
        assertThat(resJan.stønadsbeløpMnd).isEqualTo(finnMakssats(førsteJan).beløp)

        val forventetUtgiftRes =
            listOf(
                UtgiftTilOvernatting(
                    fom = LocalDate.of(2024, 1, 3),
                    tom = LocalDate.of(2024, 1, 4),
                    utgift = 4000,
                    beløpSomDekkes = 4000,
                ),
                UtgiftTilOvernatting(
                    fom = LocalDate.of(2024, 1, 7),
                    tom = LocalDate.of(2024, 1, 8),
                    utgift = 1000,
                    beløpSomDekkes = 809,
                ),
                UtgiftTilOvernatting(
                    fom = LocalDate.of(2024, 1, 10),
                    tom = LocalDate.of(2024, 1, 11),
                    utgift = 1000,
                    beløpSomDekkes = 0,
                ),
            )

        assertThat(resJan.utgifterTilOvernatting).isEqualTo(forventetUtgiftRes)
    }
}
