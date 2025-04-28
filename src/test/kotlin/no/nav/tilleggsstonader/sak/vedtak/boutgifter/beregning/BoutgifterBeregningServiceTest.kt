package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.beregningsresultatFørstegangsbehandlingLøpendeUtgifterEnBolig
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.beregningsresultatFørstegangsbehandlingLøpendeUtgifterToBoliger
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.beregningsresultatFørstegangsbehandlingMidlertidigOvernatting
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.innvilgelseBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.lagBeregningsresultatMåned
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.løpendeUtgifterEnBolig
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.løpendeUtgifterToBoliger
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.utgiftMidlertidigOvernatting
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
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

    val vedtaksperiodeValideringService =
        VedtaksperiodeValideringService(
            vilkårperiodeService = vilkårperiodeService,
            vedtakRepository = vedtakRepository,
        )

    val boutgifterBeregningService =
        BoutgifterBeregningService(
            boutgifterUtgiftService = boutgifterUtgiftService,
            vedtaksperiodeValideringService = vedtaksperiodeValideringService,
            vedtakRepository = vedtakRepository,
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
        val vedtaksperioderFørstegangsbehandling =
            listOf(vedtaksperiode(fom = LocalDate.of(2025, 1, 1), tom = LocalDate.of(2025, 3, 31)))

        @BeforeEach
        fun setup() {
            every { vilkårperiodeService.hentVilkårperioder(any()) } returns vilkårperioder
        }

        @Test
        fun `Kan beregne for løpende utgifter en bolig`() {
            every { boutgifterUtgiftService.hentUtgifterTilBeregning(any()) } returns løpendeUtgifterEnBolig

            val beregningsresultat =
                boutgifterBeregningService
                    .beregn(
                        behandling = behandling,
                        vedtaksperioder = vedtaksperioderFørstegangsbehandling,
                        typeVedtak = TypeVedtak.INNVILGELSE,
                    ).perioder

            assertThat(beregningsresultat).isEqualTo(beregningsresultatFørstegangsbehandlingLøpendeUtgifterEnBolig)
        }

        @Test
        fun `Kan beregne revurderinog for løpende utgifter en bolig`() {
            val utgifterRevurdering: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>> =
                mapOf(
                    TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                        listOf(
                            UtgiftBeregningBoutgifter(
                                fom = LocalDate.of(2025, 1, 1),
                                tom = LocalDate.of(2025, 3, 31),
                                utgift = 3000,
                            ),
                            UtgiftBeregningBoutgifter(
                                fom = LocalDate.of(2025, 4, 1),
                                tom = LocalDate.of(2025, 4, 30),
                                utgift = 6000,
                            ),
                        ),
                )

            val vedtaksperioderRevurdering =
                listOf(
                    vedtaksperiode(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 3, 31),
                    ),
                    vedtaksperiode(
                        fom = LocalDate.of(2025, 4, 1),
                        tom = LocalDate.of(2025, 4, 30),
                    ),
                )

            val innvilgelseBoutgifter =
                innvilgelseBoutgifter(
                    beregningsresultat =
                        BeregningsresultatBoutgifter(
                            beregningsresultatFørstegangsbehandlingLøpendeUtgifterEnBolig,
                        ),
                    vedtaksperioder = vedtaksperioderFørstegangsbehandling,
                )

            val utgifterEtterRevuderFra: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>> =
                mapOf(
                    TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                        listOf(
                            UtgiftBeregningBoutgifter(
                                fom = LocalDate.of(2025, 4, 1),
                                tom = LocalDate.of(2025, 4, 30),
                                utgift = 6000,
                            ),
                        ),
                )

            val forventet =
                listOf(
                    lagBeregningsresultatMåned(
                        fom = LocalDate.of(2025, 1, 1),
                        utgifter = løpendeUtgifterEnBolig,
                        delAvTidligere = true,
                    ),
                    lagBeregningsresultatMåned(
                        fom = LocalDate.of(2025, 2, 1),
                        utgifter = løpendeUtgifterEnBolig,
                        delAvTidligere = true,
                    ),
                    lagBeregningsresultatMåned(
                        fom = LocalDate.of(2025, 3, 1),
                        utgifter = løpendeUtgifterEnBolig,
                        delAvTidligere = true,
                    ),
                    lagBeregningsresultatMåned(
                        fom = LocalDate.of(2025, 4, 1),
                        utgifter = utgifterEtterRevuderFra,
                        delAvTidligere = false,
                    ),
                )

            every { boutgifterUtgiftService.hentUtgifterTilBeregning(any()) } returns utgifterRevurdering
            every { vedtakRepository.findByIdOrThrow(any()) } returns innvilgelseBoutgifter

            val res =
                boutgifterBeregningService
                    .beregn(
                        behandling =
                            saksbehandling(
                                revurderFra = LocalDate.of(2025, 4, 1),
                                forrigeIverksatteBehandlingId = BehandlingId.random(),
                                type = BehandlingType.REVURDERING,
                            ),
                        vedtaksperioder = vedtaksperioderRevurdering,
                        typeVedtak = TypeVedtak.INNVILGELSE,
                    ).perioder

            assertThat(res.size).isEqualTo(4)
            assertThat(res).isEqualTo(forventet)
        }

        @Test
        fun `Kan beregne for løpende utgifter to boliger`() {
            every { boutgifterUtgiftService.hentUtgifterTilBeregning(any()) } returns løpendeUtgifterToBoliger

            val res =
                boutgifterBeregningService
                    .beregn(
                        behandling = behandling,
                        vedtaksperioder = vedtaksperioderFørstegangsbehandling,
                        typeVedtak = TypeVedtak.INNVILGELSE,
                    ).perioder

            assertThat(res).isEqualTo(beregningsresultatFørstegangsbehandlingLøpendeUtgifterToBoliger)
        }

        @Test
        fun `Kan beregne revurderinog for løpende utgifter to boliger`() {
            val utgifterRevurdering: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>> =
                mapOf(
                    TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER to
                        listOf(
                            UtgiftBeregningBoutgifter(
                                fom = LocalDate.of(2025, 1, 1),
                                tom = LocalDate.of(2025, 3, 31),
                                utgift = 3000,
                            ),
                            UtgiftBeregningBoutgifter(
                                fom = LocalDate.of(2025, 4, 1),
                                tom = LocalDate.of(2025, 4, 30),
                                utgift = 6000,
                            ),
                        ),
                )
            val innvilgelseBoutgifter =
                innvilgelseBoutgifter(
                    beregningsresultat =
                        BeregningsresultatBoutgifter(
                            beregningsresultatFørstegangsbehandlingLøpendeUtgifterToBoliger,
                        ),
                    vedtaksperioder = vedtaksperioderFørstegangsbehandling,
                )

            val vedtaksperioderRevurdering =
                listOf(
                    vedtaksperiode(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 3, 31),
                    ),
                    vedtaksperiode(
                        fom = LocalDate.of(2025, 4, 1),
                        tom = LocalDate.of(2025, 4, 30),
                    ),
                )

            val utgifterEtterRevuderFra: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>> =
                mapOf(
                    TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER to
                        listOf(
                            UtgiftBeregningBoutgifter(
                                fom = LocalDate.of(2025, 4, 1),
                                tom = LocalDate.of(2025, 4, 30),
                                utgift = 6000,
                            ),
                        ),
                )
            val forventet =
                listOf(
                    lagBeregningsresultatMåned(
                        fom = LocalDate.of(2025, 1, 1),
                        utgifter = løpendeUtgifterToBoliger,
                        delAvTidligere = true,
                    ),
                    lagBeregningsresultatMåned(
                        fom = LocalDate.of(2025, 2, 1),
                        utgifter = løpendeUtgifterToBoliger,
                        delAvTidligere = true,
                    ),
                    lagBeregningsresultatMåned(
                        fom = LocalDate.of(2025, 3, 1),
                        utgifter = løpendeUtgifterToBoliger,
                        delAvTidligere = true,
                    ),
                    lagBeregningsresultatMåned(
                        fom = LocalDate.of(2025, 4, 1),
                        utgifter = utgifterEtterRevuderFra,
                        delAvTidligere = false,
                    ),
                )

            every { boutgifterUtgiftService.hentUtgifterTilBeregning(any()) } returns utgifterRevurdering
            every { vedtakRepository.findByIdOrThrow(any()) } returns innvilgelseBoutgifter

            val res =
                boutgifterBeregningService
                    .beregn(
                        behandling =
                            saksbehandling(
                                revurderFra = LocalDate.of(2025, 4, 1),
                                forrigeIverksatteBehandlingId = BehandlingId.random(),
                                type = BehandlingType.REVURDERING,
                            ),
                        vedtaksperioder = vedtaksperioderRevurdering,
                        typeVedtak = TypeVedtak.INNVILGELSE,
                    ).perioder

            assertThat(res.size).isEqualTo(4)
            assertThat(res).isEqualTo(forventet)
        }
    }

    @Nested
    inner class UtgifterOvernatting {
        val vedtaksperioder =
            listOf(
                vedtaksperiode(
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 1, 31),
                ),
            )

        @BeforeEach
        fun setup() {
            every { vilkårperiodeService.hentVilkårperioder(any()) } returns vilkårperioder
        }

        @Test
        fun `Kan beregne for utgift overnatting`() {
            every { boutgifterUtgiftService.hentUtgifterTilBeregning(any()) } returns utgiftMidlertidigOvernatting

            val res =
                boutgifterBeregningService
                    .beregn(
                        behandling = behandling,
                        vedtaksperioder = vedtaksperioder,
                        typeVedtak = TypeVedtak.INNVILGELSE,
                    ).perioder

            assertThat(res).isEqualTo(beregningsresultatFørstegangsbehandlingMidlertidigOvernatting)
        }

        @Test
        fun `Kan revurdere med tidligere vedtaksperioder for utgift overnatting`() {
            val utgifterRevurdering: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>> =
                mapOf(
                    TypeBoutgift.UTGIFTER_OVERNATTING to
                        listOf(
                            UtgiftBeregningBoutgifter(
                                fom = LocalDate.of(2025, 1, 1),
                                tom = LocalDate.of(2025, 1, 31),
                                utgift = 3000,
                            ),
                            UtgiftBeregningBoutgifter(
                                fom = LocalDate.of(2025, 3, 10),
                                tom = LocalDate.of(2025, 3, 12),
                                utgift = 6000,
                            ),
                        ),
                )

            val vedtaksperioderRevurdering =
                listOf(
                    vedtaksperiode(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 31),
                    ),
                    vedtaksperiode(
                        fom = LocalDate.of(2025, 3, 10),
                        tom = LocalDate.of(2025, 3, 12),
                    ),
                )

            val utgiftEtterRevurderFra: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>> =
                mapOf(
                    TypeBoutgift.UTGIFTER_OVERNATTING to
                        listOf(
                            UtgiftBeregningBoutgifter(
                                fom = LocalDate.of(2025, 3, 10),
                                tom = LocalDate.of(2025, 3, 12),
                                utgift = 6000,
                            ),
                        ),
                )

            val forventet =
                listOf(
                    lagBeregningsresultatMåned(
                        fom = LocalDate.of(2025, 1, 1),
                        utgifter = utgiftMidlertidigOvernatting,
                        delAvTidligere = true,
                    ),
                    lagBeregningsresultatMåned(
                        fom = LocalDate.of(2025, 3, 10),
                        tom = LocalDate.of(2025, 3, 12),
                        utgifter = utgiftEtterRevurderFra,
                        delAvTidligere = false,
                    ),
                )

            every { boutgifterUtgiftService.hentUtgifterTilBeregning(any()) } returns utgifterRevurdering
            every { vedtakRepository.findByIdOrThrow(any()) } returns innvilgelseBoutgifter(vedtaksperioder = vedtaksperioder)

            val res =
                boutgifterBeregningService
                    .beregn(
                        behandling =
                            saksbehandling(
                                revurderFra = LocalDate.of(2025, 3, 10),
                                forrigeIverksatteBehandlingId = BehandlingId.random(),
                                type = BehandlingType.REVURDERING,
                            ),
                        vedtaksperioder = vedtaksperioderRevurdering,
                        typeVedtak = TypeVedtak.INNVILGELSE,
                    ).perioder

            assertThat(res.size).isEqualTo(2)
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
            }.hasMessage("Det er ikke lagt inn noen oppfylte utgiftsperioder")
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
            }.hasMessage("Vedtaksperioden 01.01.2025–31.01.2025 mangler oppfylt utgift hele eller deler av perioden.")
        }

        @Test
        fun `Kaster feil hvis utgift krysser utbetaling`() {
            val vedtaksperioder =
                listOf(
                    vedtaksperiode(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 10),
                    ),
                    vedtaksperiode(
                        fom = LocalDate.of(2025, 1, 25),
                        tom = LocalDate.of(2025, 2, 5),
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
