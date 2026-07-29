package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import io.mockk.every
import io.mockk.mockk
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
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BoutgifterBeregningLøpendeUtgifterToBoliger {
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

    val løpendeUtgifterToBoliger: BoutgifterPerUtgiftstype =
        mapOf(
            TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER to
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
                    plan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
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

    /**
     * Reproduserer en bug der revurdering feiler med "Vi støtter foreløpig ikke at utbetalingsperioder inneholder
     * mer enn én løpende utgift" når:
     *  - vedtaksperioden starter på en dato midt i måneden (f.eks. den 11.)
     *  - forrige beregning har perioder med kalenderbaserte start-datoer (fra en tidligere bug)
     *  - saksbehandler splitter en utgift: kutter eksisterende og legger til ny fra 1. i neste måned
     *
     * Årsak: beregnFra ble satt til 1. i måneden (f.eks. 01.02), men splitFra delte vedtaksperioden i to deler
     * som begge ble sendt til beregnAktuellePerioder. Den løpende måneden som spenner over beregnFra (f.eks. 11.01->10.02)
     * fikk to vedtaksperioder og resulterte i en utbetalingsperiode som overlappet begge utgiftene.
     */
    @Test
    fun `revurdering med to consecutive utgifter der grensen faller midt i en løpende måned gir ikke feil`() {
        // Vedtaksperiode starter 11. jan – løpende måneder går fra 11. til 10.
        val vedtaksperiode = vedtaksperiode(fom = LocalDate.of(2025, 1, 11), tom = LocalDate.of(2025, 2, 28))

        // Forrige beregning har kalenderbaserte perioder (fra eldre buggy data):
        // 01.01->31.01 og 01.02->28.02 i stedet for 11.01->10.02 og 11.02->28.02
        val originalUtgift =
            mapOf(
                TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER to
                    listOf(
                        lagUtgiftBeregningBoutgifter(
                            fom = LocalDate.of(2025, 1, 1),
                            tom = LocalDate.of(2025, 2, 28),
                            utgift = 3000,
                        ),
                    ),
            )
        val forrigeBeregningsresultat =
            listOf(
                lagBeregningsresultatMåned(
                    fom = LocalDate.of(2025, 1, 1),
                    tom = LocalDate.of(2025, 1, 31),
                    utgifter = originalUtgift,
                    delAvTidligereUtbetaling = false,
                ),
                lagBeregningsresultatMåned(
                    fom = LocalDate.of(2025, 2, 1),
                    tom = LocalDate.of(2025, 2, 28),
                    utgifter = originalUtgift,
                    delAvTidligereUtbetaling = false,
                ),
            )

        // I revurderingen: kutt eksisterende til 31.01 og legg til ny fra 01.02 med lavere beløp
        val revurderingsutgifter =
            mapOf(
                TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER to
                    listOf(
                        lagUtgiftBeregningBoutgifter(
                            fom = LocalDate.of(2025, 1, 1),
                            tom = LocalDate.of(2025, 1, 31),
                            utgift = 3000,
                        ),
                        lagUtgiftBeregningBoutgifter(
                            fom = LocalDate.of(2025, 2, 1),
                            tom = LocalDate.of(2025, 2, 28),
                            utgift = 1000,
                        ),
                    ),
            )

        val innvilgelse =
            innvilgelseBoutgifter(
                beregningsresultat = BeregningsresultatBoutgifter(forrigeBeregningsresultat),
                vedtaksperioder = listOf(vedtaksperiode),
            )

        every { boutgifterUtgiftService.hentUtgifterTilBeregning(any()) } returns revurderingsutgifter
        every { vedtakRepository.findByIdOrThrow(any()) } returns innvilgelse

        val saksbehandling =
            saksbehandling(
                forrigeIverksatteBehandlingId = BehandlingId.random(),
                type = BehandlingType.REVURDERING,
            )

        // beregnFra = 01.02.2025 (ny utgift starter der, forrige tom endret seg på 31.01)
        val beregnFra = LocalDate.of(2025, 2, 1)

        val res =
            boutgifterBeregningService
                .beregn(
                    behandling = saksbehandling,
                    vedtaksperioder = listOf(vedtaksperiode),
                    plan = Beregningsplan(Beregningsomfang.FRA_DATO, beregnFra),
                    typeVedtak = TypeVedtak.INNVILGELSE,
                ).perioder

        // Periode fra januar beholdes fra forrige vedtak, februarperiode beregnes på nytt med 1000 kr
        assertThat(res).hasSize(2)
        assertThat(res[0].fom).isEqualTo(LocalDate.of(2025, 1, 1))
        assertThat(
            res[0]
                .grunnlag.utgifter[TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER]!!
                .single()
                .utgift,
        ).isEqualTo(3000)
        assertThat(res[1].fom).isEqualTo(LocalDate.of(2025, 2, 1))
        assertThat(
            res[1]
                .grunnlag.utgifter[TypeBoutgift.LØPENDE_UTGIFTER_TO_BOLIGER]!!
                .single()
                .utgift,
        ).isEqualTo(1000)
    }
}
