package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.UtgiftBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID.randomUUID

fun lagTilsynBarnInnvilgelseMedBeløp(
    fom: LocalDate,
    tom: LocalDate,
    månedsbeløp: Int,
    makssats: Int,
    utgift: Int,
): Pair<InnvilgelseTilsynBarn, AndelTilkjentYtelse> {
    val målgruppe = MålgruppeType.AAP

    val vedtaksperiode =
        Vedtaksperiode(
            id = randomUUID(),
            fom = fom,
            tom = tom,
            målgruppe = målgruppe,
            aktivitet = AktivitetType.TILTAK,
        )
    val aktivitet =
        Aktivitet(
            id = randomUUID(),
            type = AktivitetType.TILTAK,
            fom = fom,
            tom = tom,
            aktivitetsdager = 5,
        )
    val grunnlag =
        VedtaksperiodeGrunnlag(
            vedtaksperiode = VedtaksperiodeBeregning(vedtaksperiode.tilDto()),
            aktiviteter = listOf<Aktivitet>(aktivitet),
            antallDager = 25,
        )

    val beregningsgrunnlag =
        Beregningsgrunnlag(
            måned = fom.toYearMonth(),
            makssats = makssats,
            vedtaksperiodeGrunnlag = listOf(grunnlag),
            utgifter = listOf(UtgiftBarn(BarnId.random(), utgift)),
            utgifterTotal = utgift,
            antallBarn = 1,
        )

    val beløpsperiode = Beløpsperiode(dato = fom, beløp = månedsbeløp, målgruppe = målgruppe)

    val beregningsresultat =
        BeregningsresultatTilsynBarn(
            perioder =
                listOf(
                    BeregningsresultatForMåned(
                        dagsats = BigDecimal.TEN,
                        månedsbeløp = månedsbeløp,
                        grunnlag = beregningsgrunnlag,
                        beløpsperioder = listOf(beløpsperiode),
                    ),
                ),
        )

    val vedtaksdata =
        InnvilgelseTilsynBarn(
            vedtaksperioder = listOf(vedtaksperiode),
            beregningsresultat = beregningsresultat,
        )

    val andel =
        AndelTilkjentYtelse(
            fom = fom,
            tom = fom,
            beløp = månedsbeløp,
            id = randomUUID(),
            satstype = Satstype.DAG,
            utbetalingsdato = fom,
            type = TypeAndel.TILSYN_BARN_AAP,
            kildeBehandlingId = BehandlingId.random(),
        )

    return vedtaksdata to andel
}
