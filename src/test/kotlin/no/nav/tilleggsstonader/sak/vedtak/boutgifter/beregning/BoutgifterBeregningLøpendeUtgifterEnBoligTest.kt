package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.libs.utils.dato.april
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mai
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
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
                    plan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                    typeVedtak = TypeVedtak.INNVILGELSE,
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
                    plan = Beregningsplan(Beregningsomfang.FRA_DATO, tidligsteEndring),
                    typeVedtak = TypeVedtak.INNVILGELSE,
                ).perioder

        assertThat(res.size).isEqualTo(4)
        assertThat(res).isEqualTo(forventet)
    }

    @Test
    fun `faktiske utgifter midt i perioden - splitter ved begge grenser slik at normale utgifter etter faktiske får ny periodetelling`() {
        val vilkårperioder =
            Vilkårperioder(
                målgrupper =
                    listOf(
                        målgruppe(
                            fom = 15 februar 2026,
                            tom = 31 mai 2026,
                        ),
                    ),
                aktiviteter =
                    listOf(
                        aktivitet(
                            fom = 1 januar 2026,
                            tom = 31 mai 2026,
                        ),
                    ),
            )
        every { vilkårperiodeService.hentVilkårperioder(any()) } returns vilkårperioder

        val utgifter =
            mapOf(
                TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                    listOf(
                        lagUtgiftBeregningBoutgifter(
                            fom = 1 februar 2026,
                            tom = 28 februar 2026,
                            utgift = 600,
                        ),
                        lagUtgiftBeregningBoutgifter(
                            fom = 1 mars 2026,
                            tom = 31 mars 2026,
                            utgift = 11500,
                            skalFåDekketFaktiskeUtgifter = true,
                        ),
                        lagUtgiftBeregningBoutgifter(
                            fom = 1 april 2026,
                            tom = 30 april 2026,
                            utgift = 600,
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
                                fom = 15 februar 2026,
                                tom = 30 april 2026,
                            ),
                        ),
                    plan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                    typeVedtak = TypeVedtak.INNVILGELSE,
                ).perioder

        assertThat(perioder).hasSize(3)

        // Normale utgifter
        assertThat(perioder[0].fom).isEqualTo(15 februar 2026)
        assertThat(perioder[0].tom).isEqualTo(28 februar 2026) // Blir kuttet pga faktiske utgifter i neste periode
        assertThat(perioder[0].stønadsbeløp).isEqualTo(600)

        // Faktiske utgifter
        assertThat(perioder[1].fom).isEqualTo(1 mars 2026)
        assertThat(perioder[1].tom).isEqualTo(31 mars 2026)
        assertThat(perioder[1].stønadsbeløp).isEqualTo(11500)

        // Normale utgifter (ny periodetelling etter faktiske)
        assertThat(perioder[2].fom).isEqualTo(1 april 2026)
        assertThat(perioder[2].tom).isEqualTo(30 april 2026)
        assertThat(perioder[2].stønadsbeløp).isEqualTo(600)
    }
}
