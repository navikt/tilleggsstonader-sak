package no.nav.tilleggsstonader.sak.vedtak.boutgifter

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.UtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate
import java.util.UUID

object BoutgifterTestUtil {
    val vedtaksperiodeIdFørstegangsbehandling = UUID.randomUUID()

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

    val løpendeUtgifterEnBolig: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>> =
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

    val løpendeUtgifterToBoliger: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>> =
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

    val beregningsresultatFørstegangsbehandlingMidlertidigOvernatting =
        listOf(
            BeregningsresultatForLøpendeMåned(
                grunnlag =
                    Beregningsgrunnlag(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 31),
                        utbetalingsdato = LocalDate.of(2025, 1, 1),
                        utgifter = utgiftMidlertidigOvernatting,
                        makssats = 4953,
                        makssatsBekreftet = true,
                        målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                        aktivitet = AktivitetType.TILTAK,
                    ),
                delAvTidligereUtbetaling = false,
            ),
        )

    val beregningsresultatFørstegangsbehandlingLøpendeUtgifterEnBolig =
        listOf(
            BeregningsresultatForLøpendeMåned(
                grunnlag =
                    Beregningsgrunnlag(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 31),
                        utbetalingsdato = LocalDate.of(2025, 1, 1),
                        utgifter = løpendeUtgifterEnBolig,
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
                        utgifter = løpendeUtgifterEnBolig,
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
                        utgifter = løpendeUtgifterEnBolig,
                        makssats = 4953,
                        makssatsBekreftet = true,
                        målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                        aktivitet = AktivitetType.TILTAK,
                    ),
                delAvTidligereUtbetaling = false,
            ),
        )

    val beregningsresultatFørstegangsbehandlingLøpendeUtgifterToBoliger =
        listOf(
            BeregningsresultatForLøpendeMåned(
                grunnlag =
                    Beregningsgrunnlag(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 31),
                        utbetalingsdato = LocalDate.of(2025, 1, 1),
                        utgifter = løpendeUtgifterToBoliger,
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
                        utgifter = løpendeUtgifterToBoliger,
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
                        utgifter = løpendeUtgifterToBoliger,
                        makssats = 4953,
                        makssatsBekreftet = true,
                        målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                        aktivitet = AktivitetType.TILTAK,
                    ),
                delAvTidligereUtbetaling = false,
            ),
        )

    fun innvilgelseBoutgifter(
        vedtaksperioder: List<Vedtaksperiode>,
        beregningsresultat: BeregningsresultatBoutgifter =
            BeregningsresultatBoutgifter(
                beregningsresultatFørstegangsbehandlingMidlertidigOvernatting,
            ),
    ) = GeneriskVedtak(
        behandlingId = BehandlingId.random(),
        data =
            InnvilgelseBoutgifter(
                beregningsresultat = beregningsresultat,
                vedtaksperioder = vedtaksperioder,
                begrunnelse = null,
            ),
        type = TypeVedtak.INNVILGELSE,
        gitVersjon = "versjon-test",
    )

    fun vedtaksperiode(
        id: UUID = vedtaksperiodeIdFørstegangsbehandling,
        fom: LocalDate,
        tom: LocalDate,
        målgruppe: FaktiskMålgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
        aktivitet: AktivitetType = AktivitetType.TILTAK,
    ) = Vedtaksperiode(
        id = id,
        fom = fom,
        tom = tom,
        målgruppe = målgruppe,
        aktivitet = aktivitet,
    )

    fun lagBeregningsresultatMåned(
        fom: LocalDate,
        tom: LocalDate = fom.withDayOfMonth(fom.lengthOfMonth()),
        utgifter: Map<TypeBoutgift, List<UtgiftBeregningBoutgifter>>,
        delAvTidligere: Boolean,
    ) = BeregningsresultatForLøpendeMåned(
        grunnlag =
            Beregningsgrunnlag(
                fom = fom,
                tom = tom,
                utbetalingsdato = fom,
                utgifter = utgifter,
                makssats = 4953,
                makssatsBekreftet = true,
                målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                aktivitet = AktivitetType.TILTAK,
            ),
        delAvTidligereUtbetaling = delAvTidligere,
    )
}
