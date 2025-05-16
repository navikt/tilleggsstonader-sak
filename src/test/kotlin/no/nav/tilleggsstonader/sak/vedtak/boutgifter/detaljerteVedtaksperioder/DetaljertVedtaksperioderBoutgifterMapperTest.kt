package no.nav.tilleggsstonader.sak.vedtak.boutgifter.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.UtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.finnMakssats
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.detaljerteVedtaksperioder.DetaljertVedtaksperioderBoutgifterMapper.finnDetaljerteVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DetaljertVedtaksperioderBoutgifterMapperTest {
    val førsteJan = LocalDate.of(2024, 1, 1)
    val sisteJan = LocalDate.of(2024, 1, 31)
    val førsteFeb = LocalDate.of(2024, 2, 1)
    val sisteFeb = LocalDate.of(2024, 2, 29)
    /*
     * Overnatting:
     * [X] To utgifter i hver sin 30 dagers periode skal ikke slås sammen (en over makssats og en under)
     *
     * [X] 3 utgifter i samme 30 dagersperiode -> en ender med beløp dekket = 0
     * Sjekke utregning av "beløp som dekkes"
     *
     * Løpende utgifter:
     * [X] Test 1: to mnd med løpende utgifter som skal slås sammen
     *
     * Felles:
     * [x] Sjekke at de kommer som to ulike perioder og ikke slås sammen hvis det finnes to utgifter i samme beregningsmånedresultat
     * assertThat(res.overnatting.utgifter).isEqual(overnattingsutgifter) OBS: skal ikke summere inn løpende utgifter
     * */

    @Nested
    inner class DetaljertVedtaksperioderOvernatting {
        @Test
        fun `skal ikke slå sammen 3 utgifter i samme 30 dagersperiode - sjekker utregning av beløpet som dekkes`() {
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

        @Test
        fun `skal ikke slå sammen 2 utgifter i samme 30 dagersperiode`() {
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
                                        utgift = 8000,
                                    ),
                                    UtgiftBeregningBoutgifter(
                                        fom = LocalDate.of(2024, 1, 7),
                                        tom = LocalDate.of(2024, 1, 8),
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
            assertThat(resJan.utgifterTilOvernatting).hasSize(2)
            assertThat(resJan.stønadsbeløpMnd).isEqualTo(finnMakssats(førsteJan).beløp)

            val forventetUtgiftRes =
                listOf(
                    UtgiftTilOvernatting(
                        fom = LocalDate.of(2024, 1, 3),
                        tom = LocalDate.of(2024, 1, 4),
                        utgift = 8000,
                        beløpSomDekkes = 4809,
                    ),
                    UtgiftTilOvernatting(
                        fom = LocalDate.of(2024, 1, 7),
                        tom = LocalDate.of(2024, 1, 8),
                        utgift = 1000,
                        beløpSomDekkes = 0,
                    ),
                )

            assertThat(resJan.utgifterTilOvernatting).isEqualTo(forventetUtgiftRes)
        }

        @Test
        fun `skal ikke slå sammen 2 utgifter i ulike 30 dagersperioder`() {
            val beregningsresultatJan =
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
                                        utgift = 8000,
                                    ),
                                ),
                        ),
                )

            val beregningsresultatFeb =
                BoutgifterTestUtil.lagBeregningsresultatMåned(
                    fom = førsteFeb,
                    tom = sisteFeb,
                    utgifter =
                        mapOf(
                            TypeBoutgift.UTGIFTER_OVERNATTING to
                                listOf(
                                    UtgiftBeregningBoutgifter(
                                        fom = LocalDate.of(2024, 2, 10),
                                        tom = LocalDate.of(2024, 2, 15),
                                        utgift = 4000,
                                    ),
                                ),
                        ),
                )

            val vedtak =
                InnvilgelseBoutgifter(
                    vedtaksperioder = emptyList(),
                    beregningsresultat = BeregningsresultatBoutgifter(perioder = listOf(beregningsresultatJan, beregningsresultatFeb)),
                )

            val res = vedtak.finnDetaljerteVedtaksperioder()

            assertThat(res).hasSize(2)

            val resJan = res.first()
            val resFeb = res.last()

            assertThat(resJan.fom).isEqualTo(førsteJan)
            assertThat(resJan.tom).isEqualTo(sisteJan)
            assertThat(resJan.erLøpendeUtgift).isFalse
            assertThat(resJan.totalUtgiftMåned).isEqualTo(8000)
            assertThat(resJan.stønadsbeløpMnd).isEqualTo(finnMakssats(førsteJan).beløp)

            assertThat(resFeb.fom).isEqualTo(førsteFeb)
            assertThat(resFeb.tom).isEqualTo(sisteFeb)
            assertThat(resFeb.erLøpendeUtgift).isFalse
            assertThat(resFeb.totalUtgiftMåned).isEqualTo(4000)
            assertThat(resFeb.stønadsbeløpMnd).isEqualTo(4000)
        }
    }

    @Nested
    inner class DetaljertVedtaksperioderLøpende {
        @Test
        fun `skal slå sammen to måneder med løpende utgifter`() {
            val beregningsresultat =
                listOf(
                    BoutgifterTestUtil.lagBeregningsresultatMåned(
                        fom = førsteJan,
                        tom = sisteJan,
                        utgifter =
                            mapOf(
                                TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                                    listOf(
                                        UtgiftBeregningBoutgifter(
                                            fom = førsteJan,
                                            tom = sisteJan,
                                            utgift = 4000,
                                        ),
                                    ),
                            ),
                    ),
                    BoutgifterTestUtil.lagBeregningsresultatMåned(
                        fom = førsteFeb,
                        tom = sisteFeb,
                        utgifter =
                            mapOf(
                                TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                                    listOf(
                                        UtgiftBeregningBoutgifter(
                                            fom = førsteFeb,
                                            tom = sisteFeb,
                                            utgift = 4000,
                                        ),
                                    ),
                            ),
                    ),
                )

            val vedtak =
                InnvilgelseBoutgifter(
                    vedtaksperioder = emptyList(),
                    beregningsresultat = BeregningsresultatBoutgifter(perioder = beregningsresultat),
                )

            val res = vedtak.finnDetaljerteVedtaksperioder()

            assertThat(res.size).isEqualTo(1)

            val sammenslåttRes = res.first()

            assertThat(sammenslåttRes.fom).isEqualTo(førsteJan)
            assertThat(sammenslåttRes.tom).isEqualTo(sisteFeb)
            assertThat(sammenslåttRes.antallMåneder).isEqualTo(2)
            assertThat(sammenslåttRes.totalUtgiftMåned).isEqualTo(4000)
            assertThat(sammenslåttRes.stønadsbeløpMnd).isEqualTo(4000)
            assertThat(sammenslåttRes.erLøpendeUtgift).isTrue()
        }
    }

    @Nested
    inner class DetaljertVedtaksperioderBegge {
        // Tror dette blir feil - stønadsbeløpet blir brukt opp og beløpet som dekkes på overnattingene burde være 0
        @Test
        fun `skal ikke slå sammen løpende og overnatting i samme periode eller utgifter for overnatting`() {
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
                                        utgift = 8000,
                                    ),
                                    UtgiftBeregningBoutgifter(
                                        fom = LocalDate.of(2024, 1, 7),
                                        tom = LocalDate.of(2024, 1, 8),
                                        utgift = 1000,
                                    ),
                                ),
                            TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                                listOf(
                                    UtgiftBeregningBoutgifter(
                                        fom = førsteJan,
                                        tom = sisteJan,
                                        utgift = 4000,
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

            assertThat(res).hasSize(2)

            val overnatting = res.first()
            assertThat(overnatting.fom).isEqualTo(førsteJan)
            assertThat(overnatting.tom).isEqualTo(sisteJan)
            assertThat(overnatting.erLøpendeUtgift).isFalse()
            assertThat(overnatting.utgifterTilOvernatting).hasSize(2)
            assertThat(overnatting.totalUtgiftMåned).isEqualTo(9000)
            assertThat(overnatting.stønadsbeløpMnd).isEqualTo(809)

            val løpende = res.last()
            assertThat(løpende.fom).isEqualTo(førsteJan)
            assertThat(løpende.tom).isEqualTo(sisteJan)
            assertThat(løpende.erLøpendeUtgift).isTrue()
            assertThat(løpende.utgifterTilOvernatting).isNullOrEmpty()
            assertThat(løpende.stønadsbeløpMnd).isEqualTo(4000)
            assertThat(løpende.totalUtgiftMåned).isEqualTo(4000)
        }

        @Test
        fun `skal slå sammen løpende men ikke overnattinger i samme periode`() {
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
                                        utgift = 8000,
                                    ),
                                    UtgiftBeregningBoutgifter(
                                        fom = LocalDate.of(2024, 1, 7),
                                        tom = LocalDate.of(2024, 1, 8),
                                        utgift = 1000,
                                    ),
                                ),
                            TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                                listOf(
                                    UtgiftBeregningBoutgifter(
                                        fom = førsteJan,
                                        tom = sisteJan,
                                        utgift = 4000,
                                    ),
                                ),
                        ),
                )

            val beregningsresultatForFebruar =
                BoutgifterTestUtil.lagBeregningsresultatMåned(
                    fom = førsteFeb,
                    tom = sisteFeb,
                    utgifter =
                        mapOf(
                            TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                                listOf(
                                    UtgiftBeregningBoutgifter(
                                        fom = førsteFeb,
                                        tom = sisteFeb,
                                        utgift = 4000,
                                    ),
                                ),
                        ),
                )

            val vedtak =
                InnvilgelseBoutgifter(
                    vedtaksperioder = emptyList(),
                    beregningsresultat = BeregningsresultatBoutgifter(perioder = listOf(beregningsresultat, beregningsresultatForFebruar)),
                )

            val res = vedtak.finnDetaljerteVedtaksperioder()

            assertThat(res).hasSize(2)

            val overnatting = res.first()
            assertThat(overnatting.fom).isEqualTo(førsteJan)
            assertThat(overnatting.tom).isEqualTo(sisteJan)
            assertThat(overnatting.erLøpendeUtgift).isFalse()
            assertThat(overnatting.utgifterTilOvernatting).hasSize(2)
            assertThat(overnatting.totalUtgiftMåned).isEqualTo(9000)
            assertThat(overnatting.stønadsbeløpMnd).isEqualTo(809)

            val løpende = res.last()
            assertThat(løpende.fom).isEqualTo(førsteJan)
            assertThat(løpende.tom).isEqualTo(sisteJan)
            assertThat(løpende.erLøpendeUtgift).isTrue()
            assertThat(løpende.utgifterTilOvernatting).isNullOrEmpty()
            assertThat(løpende.stønadsbeløpMnd).isEqualTo(4000)
            assertThat(løpende.totalUtgiftMåned).isEqualTo(4000)
        }
    }
}
