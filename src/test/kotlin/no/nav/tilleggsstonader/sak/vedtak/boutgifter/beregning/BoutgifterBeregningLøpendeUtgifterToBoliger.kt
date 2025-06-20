package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
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
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BoutgifterPerUtgiftstype
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BoutgifterBeregningLøpendeUtgifterToBoliger {
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
        )

    val løpendeUtgifterToBoliger: BoutgifterPerUtgiftstype =
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

    val vedtaksperioderFørstegangsbehandling =
        listOf(vedtaksperiode(fom = LocalDate.of(2025, 1, 1), tom = LocalDate.of(2025, 3, 31)))

    val beregningsresultatFørstegangsbehandlingLøpendeUtgifterToBoliger =
        listOf(
            BeregningsresultatForLøpendeMåned(
                grunnlag =
                    Beregningsgrunnlag(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 31),
                        utgifter = løpendeUtgifterToBoliger,
                        makssats = 4953,
                        makssatsBekreftet = true,
                        målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                        aktivitet = AktivitetType.TILTAK,
                    ),
                stønadsbeløp = 3000,
            ),
            BeregningsresultatForLøpendeMåned(
                grunnlag =
                    Beregningsgrunnlag(
                        fom = LocalDate.of(2025, 2, 1),
                        tom = LocalDate.of(2025, 2, 28),
                        utgifter = løpendeUtgifterToBoliger,
                        makssats = 4953,
                        makssatsBekreftet = true,
                        målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                        aktivitet = AktivitetType.TILTAK,
                    ),
                stønadsbeløp = 3000,
            ),
            BeregningsresultatForLøpendeMåned(
                grunnlag =
                    Beregningsgrunnlag(
                        fom = LocalDate.of(2025, 3, 1),
                        tom = LocalDate.of(2025, 3, 31),
                        utgifter = løpendeUtgifterToBoliger,
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
    }

    @Test
    fun `Kan beregne for løpende utgifter to boliger`() {
        every { boutgifterUtgiftService.hentUtgifterTilBeregning(any()) } returns løpendeUtgifterToBoliger

        val res =
            boutgifterBeregningService
                .beregn(
                    behandling = saksbehandling(),
                    vedtaksperioder = vedtaksperioderFørstegangsbehandling,
                    typeVedtak = TypeVedtak.INNVILGELSE,
                ).perioder

        assertThat(res).isEqualTo(beregningsresultatFørstegangsbehandlingLøpendeUtgifterToBoliger)
    }

    @Test
    fun `Beholder perioder fra før revuderFra, og beregner nye perioder ved revurdering`() {
        val utgifterRevurdering: BoutgifterPerUtgiftstype =
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

        val utgifterEtterRevuderFra: BoutgifterPerUtgiftstype =
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
                    delAvTidligereUtbetaling = true,
                ),
                lagBeregningsresultatMåned(
                    fom = LocalDate.of(2025, 2, 1),
                    utgifter = løpendeUtgifterToBoliger,
                    delAvTidligereUtbetaling = true,
                ),
                lagBeregningsresultatMåned(
                    fom = LocalDate.of(2025, 3, 1),
                    utgifter = løpendeUtgifterToBoliger,
                    delAvTidligereUtbetaling = true,
                ),
                lagBeregningsresultatMåned(
                    fom = LocalDate.of(2025, 4, 1),
                    utgifter = utgifterEtterRevuderFra,
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
