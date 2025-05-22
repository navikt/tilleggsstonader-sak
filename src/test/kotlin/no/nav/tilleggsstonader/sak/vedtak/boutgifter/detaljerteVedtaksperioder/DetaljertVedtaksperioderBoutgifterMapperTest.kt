package no.nav.tilleggsstonader.sak.vedtak.boutgifter.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.UtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.finnMakssats
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.detaljerteVedtaksperioder.DetaljertVedtaksperioderBoutgifterMapper.finnDetaljerteVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BoutgifterPerUtgiftstype
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

    @Nested
    inner class DetaljertVedtaksperioderOvernatting {
        @Test
        fun `skal ikke slå sammen utgifter i samme 30 dagersperiode og beløp som dekkes skal minkes for hver utgift`() {
            val beregningsresultat =
                BoutgifterTestUtil.lagBeregningsresultatMåned(
                    fom = førsteJan,
                    tom = sisteJan,
                    utgifter =
                        listOf(
                            UtgifterMedType(
                                fom = LocalDate.of(2024, 1, 3),
                                tom = LocalDate.of(2024, 1, 4),
                                utgift = 4000,
                                type = TypeBoutgift.UTGIFTER_OVERNATTING,
                            ),
                            UtgifterMedType(
                                fom = LocalDate.of(2024, 1, 7),
                                tom = LocalDate.of(2024, 1, 8),
                                utgift = 1000,
                                type = TypeBoutgift.UTGIFTER_OVERNATTING,
                            ),
                            UtgifterMedType(
                                fom = LocalDate.of(2024, 1, 10),
                                tom = LocalDate.of(2024, 1, 11),
                                utgift = 1000,
                                type = TypeBoutgift.UTGIFTER_OVERNATTING,
                            ),
                        ).tilUtgiftMap(),
                )

            val vedtak = innvilgelseBoutgifter(listOf(beregningsresultat))

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
        fun `skal ikke slå sammen 2 utgifter i ulike 30 dagersperioder`() {
            val beregningsresultatJan =
                BoutgifterTestUtil.lagBeregningsresultatMåned(
                    fom = førsteJan,
                    tom = sisteJan,
                    utgifter =
                        listOf(
                            UtgifterMedType(
                                fom = LocalDate.of(2024, 1, 3),
                                tom = LocalDate.of(2024, 1, 4),
                                utgift = 8000,
                                type = TypeBoutgift.UTGIFTER_OVERNATTING,
                            ),
                        ).tilUtgiftMap(),
                )

            val beregningsresultatFeb =
                BoutgifterTestUtil.lagBeregningsresultatMåned(
                    fom = førsteFeb,
                    tom = sisteFeb,
                    utgifter =
                        listOf(
                            UtgifterMedType(
                                fom = LocalDate.of(2024, 2, 10),
                                tom = LocalDate.of(2024, 2, 15),
                                utgift = 4000,
                                type = TypeBoutgift.UTGIFTER_OVERNATTING,
                            ),
                        ).tilUtgiftMap(),
                )

            val vedtak =
                InnvilgelseBoutgifter(
                    vedtaksperioder = emptyList(),
                    beregningsresultat =
                        BeregningsresultatBoutgifter(
                            perioder =
                                listOf(
                                    beregningsresultatJan,
                                    beregningsresultatFeb,
                                ),
                        ),
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

        @Test
        fun `skal sortere utgifter til overnatting kronologisk`() {
            val beregningsresultatJan =
                BoutgifterTestUtil.lagBeregningsresultatMåned(
                    fom = førsteJan,
                    tom = sisteJan,
                    utgifter =
                        listOf(
                            UtgifterMedType(
                                fom = LocalDate.of(2024, 1, 7),
                                tom = LocalDate.of(2024, 1, 8),
                                utgift = 8000,
                                type = TypeBoutgift.UTGIFTER_OVERNATTING,
                            ),
                            UtgifterMedType(
                                fom = LocalDate.of(2024, 1, 3),
                                tom = LocalDate.of(2024, 1, 4),
                                utgift = 1000,
                                type = TypeBoutgift.UTGIFTER_OVERNATTING,
                            ),
                        ).tilUtgiftMap(),
                )

            val vedtak = innvilgelseBoutgifter(listOf(beregningsresultatJan))

            val res = vedtak.finnDetaljerteVedtaksperioder()

            assertThat(res).hasSize(1)

            val utgifter = res.first().utgifterTilOvernatting!!

            assertThat(utgifter[0].fom).isEqualTo(LocalDate.of(2024, 1, 3))
            assertThat(utgifter[1].fom).isEqualTo(LocalDate.of(2024, 1, 7))
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
                            listOf(
                                UtgifterMedType(
                                    fom = førsteJan,
                                    tom = sisteJan,
                                    utgift = 4000,
                                    type = TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG,
                                ),
                            ).tilUtgiftMap(),
                    ),
                    BoutgifterTestUtil.lagBeregningsresultatMåned(
                        fom = førsteFeb,
                        tom = sisteFeb,
                        utgifter =
                            listOf(
                                UtgifterMedType(
                                    fom = førsteFeb,
                                    tom = sisteFeb,
                                    utgift = 4000,
                                    TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG,
                                ),
                            ).tilUtgiftMap(),
                    ),
                )

            val vedtak = innvilgelseBoutgifter(beregningsresultat)

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
        @Test
        fun `skal ikke slå sammen løpende og overnatting i samme periode eller utgifter for overnatting`() {
            val beregningsresultat =
                BoutgifterTestUtil.lagBeregningsresultatMåned(
                    fom = førsteJan,
                    tom = sisteJan,
                    utgifter =
                        listOf(
                            UtgifterMedType(
                                fom = LocalDate.of(2024, 1, 3),
                                tom = LocalDate.of(2024, 1, 4),
                                utgift = 8000,
                                type = TypeBoutgift.UTGIFTER_OVERNATTING,
                            ),
                            UtgifterMedType(
                                fom = LocalDate.of(2024, 1, 7),
                                tom = LocalDate.of(2024, 1, 8),
                                utgift = 1000,
                                type = TypeBoutgift.UTGIFTER_OVERNATTING,
                            ),
                            UtgifterMedType(
                                fom = førsteJan,
                                tom = sisteJan,
                                utgift = 4000,
                                type = TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG,
                            ),
                        ).tilUtgiftMap(),
                )

            val vedtak = innvilgelseBoutgifter(listOf(beregningsresultat))

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
                        listOf(
                            UtgifterMedType(
                                fom = LocalDate.of(2024, 1, 3),
                                tom = LocalDate.of(2024, 1, 4),
                                utgift = 8000,
                                type = TypeBoutgift.UTGIFTER_OVERNATTING,
                            ),
                            UtgifterMedType(
                                fom = LocalDate.of(2024, 1, 7),
                                tom = LocalDate.of(2024, 1, 8),
                                utgift = 1000,
                                type = TypeBoutgift.UTGIFTER_OVERNATTING,
                            ),
                            UtgifterMedType(
                                fom = førsteJan,
                                tom = sisteJan,
                                utgift = 4000,
                                type = TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG,
                            ),
                        ).tilUtgiftMap(),
                )

            val beregningsresultatForFebruar =
                BoutgifterTestUtil.lagBeregningsresultatMåned(
                    fom = førsteFeb,
                    tom = sisteFeb,
                    utgifter =
                        listOf(
                            UtgifterMedType(
                                fom = førsteFeb,
                                tom = sisteFeb,
                                utgift = 4000,
                                type = TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG,
                            ),
                        ).tilUtgiftMap(),
                )

            val vedtak =
                innvilgelseBoutgifter(
                    listOf(
                        beregningsresultat,
                        beregningsresultatForFebruar,
                    ),
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
            assertThat(løpende.tom).isEqualTo(sisteFeb)
            assertThat(løpende.erLøpendeUtgift).isTrue()
            assertThat(løpende.antallMåneder).isEqualTo(2)
            assertThat(løpende.utgifterTilOvernatting).isNullOrEmpty()
            assertThat(løpende.stønadsbeløpMnd).isEqualTo(4000)
            assertThat(løpende.totalUtgiftMåned).isEqualTo(4000)
        }
    }

    data class UtgifterMedType(
        val fom: LocalDate,
        val tom: LocalDate,
        val utgift: Int,
        val type: TypeBoutgift,
    )

    private fun List<UtgifterMedType>.tilUtgiftMap(): BoutgifterPerUtgiftstype =
        this
            .groupBy { it.type }
            .mapValues { entry ->
                entry.value.map {
                    UtgiftBeregningBoutgifter(
                        fom = it.fom,
                        tom = it.tom,
                        utgift = it.utgift,
                    )
                }
            }

    private fun innvilgelseBoutgifter(beregningsperioder: List<BeregningsresultatForLøpendeMåned>) =
        InnvilgelseBoutgifter(
            vedtaksperioder = emptyList(),
            beregningsresultat = BeregningsresultatBoutgifter(perioder = beregningsperioder),
        )
}
