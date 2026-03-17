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
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.lagUtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.vilkårperioder
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BoutgifterPerUtgiftstype
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BoutgifterBeregningLøpendeUtgifterEnBoligTest {
    val boutgifterUtgiftService = mockk<BoutgifterUtgiftService>()
    val vedtakRepository = mockk<VedtakRepository>()
    val vilkårperiodeService = mockk<VilkårperiodeService>()
    val unleashService = mockk<UnleashService>(relaxed = true)

    val vedtaksperiodeValideringService =
        VedtaksperiodeValideringService(
            vilkårperiodeService = vilkårperiodeService,
        )

    val satsBoutgifterService =
        SatsBoutgifterService(
            satsBoutgifterProvider = SatsBoutgifterProvider(),
        )

    val boutgifterBeregningService =
        BoutgifterBeregningService(
            boutgifterUtgiftService = boutgifterUtgiftService,
            vedtaksperiodeValideringService = vedtaksperiodeValideringService,
            vedtakRepository = vedtakRepository,
            unleashService = unleashService,
            satsBoutgifterService = satsBoutgifterService,
        )

    val løpendeUtgifterEnBolig: BoutgifterPerUtgiftstype =
        mapOf(
            TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                listOf(
                    lagUtgiftBeregningBoutgifter(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 3, 31),
                        utgift = 3000,
                    ),
                ),
        )

    val vedtaksperioderFørstegangsbehandling =
        listOf(vedtaksperiode(fom = LocalDate.of(2025, 1, 1), tom = LocalDate.of(2025, 3, 31)))

    val beregningsresultatFørstegangsbehandlingLøpendeUtgifterEnBolig =
        listOf(
            BeregningsresultatForLøpendeMåned(
                grunnlag =
                    Beregningsgrunnlag(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 31),
                        utgifter = løpendeUtgifterEnBolig,
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
                        utgifter = løpendeUtgifterEnBolig,
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
                        utgifter = løpendeUtgifterEnBolig,
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
    fun `Kan beregne for løpende utgifter en bolig`() {
        every { boutgifterUtgiftService.hentUtgifterTilBeregning(any()) } returns løpendeUtgifterEnBolig

        val beregningsresultat =
            boutgifterBeregningService
                .beregn(
                    behandling = saksbehandling(),
                    vedtaksperioder = vedtaksperioderFørstegangsbehandling,
                    typeVedtak = TypeVedtak.INNVILGELSE,
                    tidligsteEndring = null,
                ).perioder

        assertThat(beregningsresultat).isEqualTo(beregningsresultatFørstegangsbehandlingLøpendeUtgifterEnBolig)
    }

    @Test
    fun `Beholder perioder fra før revuderFra, og beregner nye perioder ved revurdering`() {
        val utgifterRevurdering: BoutgifterPerUtgiftstype =
            mapOf(
                TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                    listOf(
                        lagUtgiftBeregningBoutgifter(
                            fom = LocalDate.of(2025, 1, 1),
                            tom = LocalDate.of(2025, 3, 31),
                            utgift = 3000,
                        ),
                        lagUtgiftBeregningBoutgifter(
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

        val utgifterEtterRevuderFra: BoutgifterPerUtgiftstype =
            mapOf(
                TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                    listOf(
                        lagUtgiftBeregningBoutgifter(
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
                    delAvTidligereUtbetaling = true,
                ),
                lagBeregningsresultatMåned(
                    fom = LocalDate.of(2025, 2, 1),
                    utgifter = løpendeUtgifterEnBolig,
                    delAvTidligereUtbetaling = true,
                ),
                lagBeregningsresultatMåned(
                    fom = LocalDate.of(2025, 3, 1),
                    utgifter = løpendeUtgifterEnBolig,
                    delAvTidligereUtbetaling = true,
                ),
                lagBeregningsresultatMåned(
                    fom = LocalDate.of(2025, 4, 1),
                    utgifter = utgifterEtterRevuderFra,
                ),
            )

        every { boutgifterUtgiftService.hentUtgifterTilBeregning(any()) } returns utgifterRevurdering
        every { vedtakRepository.findByIdOrThrow(any()) } returns innvilgelseBoutgifter

        val saksbehandling =
            saksbehandling(
                forrigeIverksatteBehandlingId = BehandlingId.random(),
                type = BehandlingType.REVURDERING,
            )
        val tidligsteEndring = LocalDate.of(2025, 4, 1)

        val res =
            boutgifterBeregningService
                .beregn(
                    behandling = saksbehandling,
                    vedtaksperioder = vedtaksperioderRevurdering,
                    typeVedtak = TypeVedtak.INNVILGELSE,
                    tidligsteEndring = tidligsteEndring,
                ).perioder

        assertThat(res.size).isEqualTo(4)
        assertThat(res).isEqualTo(forventet)
    }

    @Test
    fun `faktiske utgifter starter ny periodetelling - vilkårperiode midt i måneden med grense mellom normal og faktiske utgifter`() {
        val vidtVilkårperioder =
            Vilkårperioder(
                målgrupper = listOf(målgruppe(fom = LocalDate.of(2025, 9, 15), tom = LocalDate.of(2026, 5, 31))),
                aktiviteter = listOf(aktivitet(fom = LocalDate.of(2025, 9, 15), tom = LocalDate.of(2026, 5, 31))),
            )
        every { vilkårperiodeService.hentVilkårperioder(any()) } returns vidtVilkårperioder

        val utgifter =
            mapOf(
                TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                    listOf(
                        lagUtgiftBeregningBoutgifter(
                            fom = LocalDate.of(2025, 9, 1),
                            tom = LocalDate.of(2026, 2, 28),
                            utgift = 600,
                        ),
                        lagUtgiftBeregningBoutgifter(
                            fom = LocalDate.of(2026, 3, 1),
                            tom = LocalDate.of(2026, 5, 31),
                            utgift = 11500,
                            skalFåDekketFaktiskeUtgifter = true,
                        ),
                    ),
            )
        every { boutgifterUtgiftService.hentUtgifterTilBeregning(any()) } returns utgifter

        val perioder =
            boutgifterBeregningService
                .beregn(
                    behandling = saksbehandling(),
                    vedtaksperioder =
                        listOf(
                            vedtaksperiode(
                                fom = LocalDate.of(2025, 9, 15),
                                tom = LocalDate.of(2026, 5, 31),
                            ),
                        ),
                    typeVedtak = TypeVedtak.INNVILGELSE,
                    tidligsteEndring = null,
                ).perioder

        assertThat(perioder).hasSize(9)

        // Normale perioder — 30-dagersvinduer fra 15.09 (forskyves ikke til kalendergrenser)
        assertThat(perioder[0].fom).isEqualTo(LocalDate.of(2025, 9, 15))
        assertThat(perioder[0].tom).isEqualTo(LocalDate.of(2025, 10, 14))
        assertThat(perioder[0].stønadsbeløp).isEqualTo(600)

        assertThat(perioder[5].fom).isEqualTo(LocalDate.of(2026, 2, 15))
        assertThat(perioder[5].tom).isEqualTo(LocalDate.of(2026, 2, 28)) // avkortet til utgift-tom, ikke 14.03
        assertThat(perioder[5].stønadsbeløp).isEqualTo(600)

        // Faktiske utgifter — starter på nytt fra 01.03 (ikke 15.03 som ville fulgt 30-dagerstellingen)
        assertThat(perioder[6].fom).isEqualTo(LocalDate.of(2026, 3, 1))
        assertThat(perioder[6].tom).isEqualTo(LocalDate.of(2026, 3, 31))
        assertThat(perioder[6].stønadsbeløp).isEqualTo(11500) // faktiske utgifter, ikke cappa av makssats

        assertThat(perioder[7].fom).isEqualTo(LocalDate.of(2026, 4, 1))
        assertThat(perioder[7].tom).isEqualTo(LocalDate.of(2026, 4, 30))
        assertThat(perioder[7].stønadsbeløp).isEqualTo(11500)

        assertThat(perioder[8].fom).isEqualTo(LocalDate.of(2026, 5, 1))
        assertThat(perioder[8].tom).isEqualTo(LocalDate.of(2026, 5, 31))
        assertThat(perioder[8].stønadsbeløp).isEqualTo(11500)
    }
}
