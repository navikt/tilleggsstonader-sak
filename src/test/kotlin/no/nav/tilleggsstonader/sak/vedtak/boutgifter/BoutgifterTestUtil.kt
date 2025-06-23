package no.nav.tilleggsstonader.sak.vedtak.boutgifter

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.defaultBehandling
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterBeregnUtil.beregnStønadsbeløp
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.UtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.finnMakssats
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BoutgifterPerUtgiftstype
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import java.time.LocalDate
import java.time.Month.JANUARY
import java.util.UUID

object BoutgifterTestUtil {
    val vedtaksperiodeIdFørstegangsbehandling: UUID = UUID.randomUUID()

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

    fun innvilgelseBoutgifter(
        behandlingId: BehandlingId = defaultBehandling.id,
        vedtaksperioder: List<Vedtaksperiode>,
        beregningsresultat: BeregningsresultatBoutgifter,
    ) = GeneriskVedtak(
        behandlingId = behandlingId,
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

    fun beregningsresultat(
        fom: LocalDate = LocalDate.of(2025, JANUARY, 1),
        tom: LocalDate = fom,
    ) = BeregningsresultatBoutgifter(
        perioder =
            listOf(
                lagBeregningsresultatMåned(
                    fom = fom,
                    tom = tom,
                    utgifter =
                        mapOf(
                            TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to
                                listOf(
                                    lagUtgiftBeregningBoutgifter(
                                        fom = fom,
                                        tom = tom,
                                        utgift = 3000,
                                    ),
                                ),
                        ),
                ),
            ),
    )

    fun lagUtgiftBeregningBoutgifter(
        fom: LocalDate,
        tom: LocalDate,
        utgift: Int = 3000,
    ): UtgiftBeregningBoutgifter =
        UtgiftBeregningBoutgifter(
            fom = fom,
            tom = tom,
            utgift = utgift,
        )

    fun lagBeregningsresultatMåned(
        fom: LocalDate,
        tom: LocalDate = fom.withDayOfMonth(fom.lengthOfMonth()),
        utgifter: BoutgifterPerUtgiftstype,
        delAvTidligereUtbetaling: Boolean = false,
    ): BeregningsresultatForLøpendeMåned {
        val grunnlag =
            Beregningsgrunnlag(
                fom = fom,
                tom = tom,
                utgifter = utgifter,
                makssats = finnMakssats(fom).beløp,
                makssatsBekreftet = true,
                målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                aktivitet = AktivitetType.TILTAK,
            )
        return BeregningsresultatForLøpendeMåned(
            grunnlag =
            grunnlag,
            stønadsbeløp = grunnlag.beregnStønadsbeløp(),
            delAvTidligereUtbetaling = delAvTidligereUtbetaling,
        )
    }
}
