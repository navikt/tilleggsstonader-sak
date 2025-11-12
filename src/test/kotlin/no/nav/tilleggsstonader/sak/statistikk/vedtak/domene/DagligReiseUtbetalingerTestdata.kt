package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Aktivitet
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.Billettype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate
import java.util.UUID.randomUUID

fun lagDagligReiseInnvilgelseMedBeløp(
    fom: LocalDate,
    tom: LocalDate,
    beløp: Int,
): Pair<InnvilgelseDagligReise, AndelTilkjentYtelse> {
    val målgruppe = MålgruppeType.AAP.faktiskMålgruppe()

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

    val beregningsresultat =
        BeregningsresultatDagligReise(
            offentligTransport =
                BeregningsresultatOffentligTransport(
                    reiser =
                        listOf(
                            BeregningsresultatForReise(
                                perioder =
                                    listOf(
                                        BeregningsresultatForPeriode(
                                            grunnlag =
                                                BeregningsgrunnlagOffentligTransport(
                                                    fom = fom,
                                                    tom = tom,
                                                    prisEnkeltbillett = 50,
                                                    prisSyvdagersbillett = 300,
                                                    pris30dagersbillett = 1000,
                                                    antallReisedagerPerUke = 5,
                                                    vedtaksperioder =
                                                        listOf(
                                                            no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.VedtaksperiodeGrunnlag(
                                                                id = randomUUID(),
                                                                fom = fom,
                                                                tom = tom,
                                                                aktivitet = aktivitet.type,
                                                                målgruppe = målgruppe,
                                                                antallReisedagerIVedtaksperioden = 20,
                                                            ),
                                                        ),
                                                    antallReisedager = 20,
                                                ),
                                            beløp = beløp,
                                            billettdetaljer =
                                                mapOf(
                                                    Billettype.TRETTIDAGERSBILLETT to
                                                        beløp,
                                                ),
                                        ),
                                    ),
                            ),
                        ),
                ),
        )

    val vedtaksdata =
        InnvilgelseDagligReise(
            vedtaksperioder = listOf(vedtaksperiode),
            beregningsresultat = beregningsresultat,
            begrunnelse = "test",
        )

    val andel =
        AndelTilkjentYtelse(
            fom = fom,
            tom = tom,
            beløp = beløp,
            id = randomUUID(),
            satstype = Satstype.DAG,
            utbetalingsdato = fom,
            type = TypeAndel.DAGLIG_REISE_AAP,
            kildeBehandlingId = BehandlingId.random(),
        )

    return vedtaksdata to andel
}
