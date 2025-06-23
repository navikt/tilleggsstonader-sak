package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.lagUtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.BoutgifterBeregnUtil.beregnStønadsbeløp
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate
import java.util.UUID.randomUUID

fun lagBoutgifterInnvilgelseMedBeløp(
    fom: LocalDate,
    tom: LocalDate,
    månedsbeløp: Int,
    makssats: Int,
    utgift: Int,
): Pair<InnvilgelseBoutgifter, AndelTilkjentYtelse> {
    val aap = MålgruppeType.AAP.faktiskMålgruppe()
    val tiltak = AktivitetType.TILTAK

    val vedtaksperiode =
        Vedtaksperiode(
            id = randomUUID(),
            fom = fom,
            tom = tom,
            målgruppe = aap,
            aktivitet = tiltak,
        )

    val løpendeUtgift =
        lagUtgiftBeregningBoutgifter(
            fom = fom,
            tom = tom,
            utgift = utgift,
        )

    val alleUtgifter = TypeBoutgift.LØPENDE_UTGIFTER_EN_BOLIG to listOf(løpendeUtgift)

    val grunnlag =
        Beregningsgrunnlag(
            fom = fom,
            tom = tom,
            utgifter = mapOf(alleUtgifter),
            makssats = makssats,
            makssatsBekreftet = true,
            målgruppe = aap,
            aktivitet = tiltak,
        )

    val vedtaksdata =
        InnvilgelseBoutgifter(
            beregningsresultat =
                BeregningsresultatBoutgifter(
                    perioder =
                        listOf(
                            BeregningsresultatForLøpendeMåned(
                                grunnlag = grunnlag,
                                stønadsbeløp = grunnlag.beregnStønadsbeløp(),
                            ),
                        ),
                ),
            vedtaksperioder = listOf(vedtaksperiode),
        )

    val andelTilkjentYtelse =
        AndelTilkjentYtelse(
            fom = fom,
            tom = tom,
            id = randomUUID(),
            beløp = månedsbeløp,
            satstype = Satstype.DAG,
            type = TypeAndel.BOUTGIFTER_AAP,
            kildeBehandlingId = BehandlingId.random(),
            utbetalingsdato = fom,
        )

    return vedtaksdata to andelTilkjentYtelse
}
