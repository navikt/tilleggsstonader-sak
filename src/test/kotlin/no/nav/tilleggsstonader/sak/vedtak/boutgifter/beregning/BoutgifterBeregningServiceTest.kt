package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.grunnlagsdataDomain
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class BoutgifterBeregningServiceTest {
    val boutgifterUtgiftService = mockk<BoutgifterUtgiftService>()
    val vedtakRepository = mockk<VedtakRepository>()
    val vilkårperiodeService = mockk<VilkårperiodeService>()
    val grunnlagsdataService = mockk<GrunnlagsdataService>()

    val vedtaksperiodeValideringService =
        VedtaksperiodeValideringService(
            vilkårperiodeService = vilkårperiodeService,
            vedtakRepository = vedtakRepository,
        )

    val boutgifterBeregningService =
        BoutgifterBeregningService(
            boutgifterUtgiftService = boutgifterUtgiftService,
            vedtaksperiodeValideringService = vedtaksperiodeValideringService,
//            vedtakRepository = vedtakRepository,
        )

    val behandling = saksbehandling()

    val vilkårperioder =
        Vilkårperioder(
            målgrupper =
                listOf(
                    målgruppe(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 10, 31),
                    ),
                ),
            aktiviteter =
                listOf(
                    aktivitet(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 10, 31),
                    ),
                ),
        )

    @Test
    fun `Kan ikke ha faste- og midlertidig utgifter i samme behandling`() {
        val utgift: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>> =
            mapOf(
                TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                    listOf(
                        UtgiftBeregningBoutgifter(
                            fom = LocalDate.of(2025, 1, 1),
                            tom = LocalDate.of(2025, 3, 31),
                            utgift = 3000,
                        ),
                    ),
                TypeBoutgift.UTGIFTER_OVERNATTING to
                    listOf(
                        UtgiftBeregningBoutgifter(
                            fom = LocalDate.of(2025, 1, 1),
                            tom = LocalDate.of(2025, 1, 3),
                            utgift = 3000,
                        ),
                    ),
            )

        val vedtaksperioder =
            listOf(
                Vedtaksperiode(
                    id = UUID.randomUUID(),
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 1, 31),
                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                    aktivitet = AktivitetType.TILTAK,
                ),
            )

        every { boutgifterUtgiftService.hentUtgifterTilBeregning(any()) } returns utgift

        assertThatThrownBy {
            boutgifterBeregningService.beregn(
                behandling,
                vedtaksperioder,
                typeVedtak = TypeVedtak.INNVILGELSE,
            )
        }.hasMessage("Foreløpig støtter vi ikke løpende og midlertidige utgifter i samme behandling")
    }

    @Nested
    inner class LøpendeUtgifter {
        val utgift: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>> =
            mapOf(
                TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                    listOf(
                        UtgiftBeregningBoutgifter(
                            fom = LocalDate.of(2025, 1, 1),
                            tom = LocalDate.of(2025, 3, 31),
                            utgift = 3000,
                        ),
                    ),
            )

        val vedtaksperioder =
            listOf(
                Vedtaksperiode(
                    id = UUID.randomUUID(),
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 3, 31),
                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                    aktivitet = AktivitetType.TILTAK,
                ),
            )

        @BeforeEach
        fun setup() {
            every { boutgifterUtgiftService.hentUtgifterTilBeregning(any()) } returns utgift
            every { vilkårperiodeService.hentVilkårperioder(any()) } returns vilkårperioder
            every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns grunnlagsdataDomain(behandlingId = behandling.id)
        }

        @Test
        fun `Kan beregne for løpende utgifter en bolig`() {
            val forventet =
                listOf(
                    BeregningsresultatForLøpendeMåned(
                        grunnlag =
                            Beregningsgrunnlag(
                                fom = LocalDate.of(2025, 1, 1),
                                tom = LocalDate.of(2025, 1, 31),
                                utbetalingsdato = LocalDate.of(2025, 1, 1),
                                utgifter = utgift,
                                makssats = 4953,
                                makssatsBekreftet = true,
                                målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                                aktivitet = AktivitetType.TILTAK,
                            ),
                        delAvTidligereUtbetaling = false,
                    ),
                    BeregningsresultatForLøpendeMåned(
                        grunnlag =
                            Beregningsgrunnlag(
                                fom = LocalDate.of(2025, 2, 1),
                                tom = LocalDate.of(2025, 2, 28),
                                utbetalingsdato = LocalDate.of(2025, 2, 1),
                                utgifter = utgift,
                                makssats = 4953,
                                makssatsBekreftet = true,
                                målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                                aktivitet = AktivitetType.TILTAK,
                            ),
                        delAvTidligereUtbetaling = false,
                    ),
                    BeregningsresultatForLøpendeMåned(
                        grunnlag =
                            Beregningsgrunnlag(
                                fom = LocalDate.of(2025, 3, 1),
                                tom = LocalDate.of(2025, 3, 31),
                                utbetalingsdato = LocalDate.of(2025, 3, 1),
                                utgifter = utgift,
                                makssats = 4953,
                                makssatsBekreftet = true,
                                målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                                aktivitet = AktivitetType.TILTAK,
                            ),
                        delAvTidligereUtbetaling = false,
                    ),
                )

            val res =
                boutgifterBeregningService
                    .beregn(
                        behandling = behandling,
                        vedtaksperioder = vedtaksperioder,
                        typeVedtak = TypeVedtak.INNVILGELSE,
                    ).perioder

            assertThat(res).isEqualTo(forventet)
        }

        @Test
        fun `Kan beregne for løpende utgifter to boliger`() {
            val utgift: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>> =
                mapOf(
                    TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER to
                        listOf(
                            UtgiftBeregningBoutgifter(
                                fom = LocalDate.of(2025, 1, 1),
                                tom = LocalDate.of(2025, 3, 31),
                                utgift = 3000,
                            ),
                        ),
                )

            every { boutgifterUtgiftService.hentUtgifterTilBeregning(any()) } returns utgift

            val forventet =
                listOf(
                    BeregningsresultatForLøpendeMåned(
                        grunnlag =
                            Beregningsgrunnlag(
                                fom = LocalDate.of(2025, 1, 1),
                                tom = LocalDate.of(2025, 1, 31),
                                utbetalingsdato = LocalDate.of(2025, 1, 1),
                                utgifter = utgift,
                                makssats = 4953,
                                makssatsBekreftet = true,
                                målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                                aktivitet = AktivitetType.TILTAK,
                            ),
                        delAvTidligereUtbetaling = false,
                    ),
                    BeregningsresultatForLøpendeMåned(
                        grunnlag =
                            Beregningsgrunnlag(
                                fom = LocalDate.of(2025, 2, 1),
                                tom = LocalDate.of(2025, 2, 28),
                                utbetalingsdato = LocalDate.of(2025, 2, 1),
                                utgifter = utgift,
                                makssats = 4953,
                                makssatsBekreftet = true,
                                målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                                aktivitet = AktivitetType.TILTAK,
                            ),
                        delAvTidligereUtbetaling = false,
                    ),
                    BeregningsresultatForLøpendeMåned(
                        grunnlag =
                            Beregningsgrunnlag(
                                fom = LocalDate.of(2025, 3, 1),
                                tom = LocalDate.of(2025, 3, 31),
                                utbetalingsdato = LocalDate.of(2025, 3, 1),
                                utgifter = utgift,
                                makssats = 4953,
                                makssatsBekreftet = true,
                                målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                                aktivitet = AktivitetType.TILTAK,
                            ),
                        delAvTidligereUtbetaling = false,
                    ),
                )

            val res =
                boutgifterBeregningService
                    .beregn(
                        behandling = behandling,
                        vedtaksperioder = vedtaksperioder,
                        typeVedtak = TypeVedtak.INNVILGELSE,
                    ).perioder

            assertThat(res).isEqualTo(forventet)
        }
    }

    @Nested
    inner class UtgifterOvernatting {
        val utgift: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>> =
            mapOf(
                TypeBoutgift.UTGIFTER_OVERNATTING to
                    listOf(
                        UtgiftBeregningBoutgifter(
                            fom = LocalDate.of(2025, 1, 1),
                            tom = LocalDate.of(2025, 1, 31),
                            utgift = 3000,
                        ),
                    ),
            )

        val vedtaksperioder =
            listOf(
                Vedtaksperiode(
                    id = UUID.randomUUID(),
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 1, 31),
                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                    aktivitet = AktivitetType.TILTAK,
                ),
            )

        @BeforeEach
        fun setup() {
            every { boutgifterUtgiftService.hentUtgifterTilBeregning(any()) } returns utgift
            every { vilkårperiodeService.hentVilkårperioder(any()) } returns vilkårperioder
            every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns grunnlagsdataDomain(behandlingId = behandling.id)
        }

        @Test
        fun `Kan beregne for utgift overnatting`() {
            val forventet =
                listOf(
                    BeregningsresultatForLøpendeMåned(
                        grunnlag =
                            Beregningsgrunnlag(
                                fom = LocalDate.of(2025, 1, 1),
                                tom = LocalDate.of(2025, 1, 31),
                                utbetalingsdato = LocalDate.of(2025, 1, 1),
                                utgifter = utgift,
                                makssats = 4953,
                                makssatsBekreftet = true,
                                målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                                aktivitet = AktivitetType.TILTAK,
                            ),
                        delAvTidligereUtbetaling = false,
                    ),
                )

            val res =
                boutgifterBeregningService
                    .beregn(
                        behandling = behandling,
                        vedtaksperioder = vedtaksperioder,
                        typeVedtak = TypeVedtak.INNVILGELSE,
                    ).perioder

            assertThat(res).isEqualTo(forventet)
        }

        @Test
        fun `Kaster feil hvis ingen utgift`() {
            every { boutgifterUtgiftService.hentUtgifterTilBeregning(any()) } returns emptyMap()

            assertThatThrownBy {
                boutgifterBeregningService.beregn(
                    behandling,
                    vedtaksperioder,
                    typeVedtak = TypeVedtak.INNVILGELSE,
                )
            }.hasMessage("Kan ikke innvilge når det ikke finnes noen utgiftsperioder")
        }

        @Test
        fun `Kaster feil hvis utgift delvis i vedtaksperiode`() {
            val utgift: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>> =
                mapOf(
                    TypeBoutgift.UTGIFTER_OVERNATTING to
                        listOf(
                            UtgiftBeregningBoutgifter(
                                fom = LocalDate.of(2025, 1, 15),
                                tom = LocalDate.of(2025, 2, 14),
                                utgift = 3000,
                            ),
                        ),
                )

            every { boutgifterUtgiftService.hentUtgifterTilBeregning(any()) } returns utgift

            assertThatThrownBy {
                boutgifterBeregningService.beregn(
                    behandling = behandling,
                    vedtaksperioder = vedtaksperioder,
                    typeVedtak = TypeVedtak.INNVILGELSE,
                )
            }.hasMessage("Kan ikke innvilge når det ikke finnes utgifter hele vedtaksperioden")
        }

        @Test
        fun `Kaster feil hvis utgift krysser utbetaling`() {
            val vedtaksperioder =
                listOf(
                    Vedtaksperiode(
                        id = UUID.randomUUID(),
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 10),
                        målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                        aktivitet = AktivitetType.TILTAK,
                    ),
                    Vedtaksperiode(
                        id = UUID.randomUUID(),
                        fom = LocalDate.of(2025, 1, 25),
                        tom = LocalDate.of(2025, 2, 5),
                        målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                        aktivitet = AktivitetType.TILTAK,
                    ),
                )

            val utgift: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>> =
                mapOf(
                    TypeBoutgift.UTGIFTER_OVERNATTING to
                        listOf(
                            UtgiftBeregningBoutgifter(
                                fom = LocalDate.of(2025, 1, 1),
                                tom = LocalDate.of(2025, 1, 10),
                                utgift = 3000,
                            ),
                            UtgiftBeregningBoutgifter(
                                fom = LocalDate.of(2025, 1, 25),
                                tom = LocalDate.of(2025, 2, 5),
                                utgift = 3000,
                            ),
                        ),
                )

            every { boutgifterUtgiftService.hentUtgifterTilBeregning(any()) } returns utgift

            assertThatThrownBy {
                boutgifterBeregningService.beregn(
                    behandling = behandling,
                    vedtaksperioder = vedtaksperioder,
                    typeVedtak = TypeVedtak.INNVILGELSE,
                )
            }.hasMessageContaining("Vi støtter foreløpig ikke at utgifter krysser ulike utbetalingsperioder")
        }
    }
}
