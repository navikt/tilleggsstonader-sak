package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.innvilgelseBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.lagBeregningsresultatMåned
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.vilkårperioder
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BoutgifterBeregningMidlertidigUtgiftTest {
    val boutgifterUtgiftService = mockk<BoutgifterUtgiftService>()
    val vedtakRepository = mockk<VedtakRepository>()
    val vilkårperiodeService = mockk<VilkårperiodeService>()
    val unleashService = mockk<UnleashService>()

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
            unleashService = unleashService,
        )

    val utgiftMidlertidigOvernatting: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>> =
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
            vedtaksperiode(
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 1, 31),
            ),
        )

    val beregningsresultatFørstegangsbehandlingMidlertidigOvernatting =
        listOf(
            BeregningsresultatForLøpendeMåned(
                grunnlag =
                    Beregningsgrunnlag(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 31),
                        utgifter = utgiftMidlertidigOvernatting,
                        makssats = 4953,
                        makssatsBekreftet = true,
                        målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                        aktivitet = AktivitetType.TILTAK,
                    ),
                stønadsbeløp = 3000,
            ),
        )

    @BeforeEach
    fun setup() {
        every { vilkårperiodeService.hentVilkårperioder(any()) } returns vilkårperioder
        every { unleashService.isEnabled(Toggle.SKAL_VISE_DETALJERT_BEREGNINGSRESULTAT) } returns true
    }

    @Test
    fun `Kan beregne for midlertidig utgift`() {
        every { boutgifterUtgiftService.hentUtgifterTilBeregning(any()) } returns utgiftMidlertidigOvernatting

        val res =
            boutgifterBeregningService
                .beregn(
                    behandling = saksbehandling(),
                    vedtaksperioder = vedtaksperioder,
                    typeVedtak = TypeVedtak.INNVILGELSE,
                ).perioder

        assertThat(res).isEqualTo(beregningsresultatFørstegangsbehandlingMidlertidigOvernatting)
    }

    @Test
    fun `Kaster feil hvis ingen utgift`() {
        every { boutgifterUtgiftService.hentUtgifterTilBeregning(any()) } returns emptyMap()

        assertThatThrownBy {
            boutgifterBeregningService.beregn(
                behandling = saksbehandling(),
                vedtaksperioder = vedtaksperioder,
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
                behandling = saksbehandling(),
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
                behandling = saksbehandling(),
                vedtaksperioder = vedtaksperioder,
                typeVedtak = TypeVedtak.INNVILGELSE,
            )
        }.hasMessageContaining("Vi støtter foreløpig ikke at utgifter krysser ulike utbetalingsperioder")
    }

    @Test
    fun `Beholder perioder fra før revuderFra, og beregner nye perioder ved revurdering`() {
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
                    delAvTidligereUtbetaling = true,
                ),
                lagBeregningsresultatMåned(
                    fom = LocalDate.of(2025, 3, 10),
                    tom = LocalDate.of(2025, 4, 9),
                    utgifter = utgiftEtterRevurderFra,
                ),
            )

        every { boutgifterUtgiftService.hentUtgifterTilBeregning(any()) } returns utgifterRevurdering
        every { vedtakRepository.findByIdOrThrow(any()) } returns
            innvilgelseBoutgifter(
                beregningsresultat =
                    BeregningsresultatBoutgifter(
                        beregningsresultatFørstegangsbehandlingMidlertidigOvernatting,
                    ),
                vedtaksperioder = vedtaksperioder,
            )

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
}
