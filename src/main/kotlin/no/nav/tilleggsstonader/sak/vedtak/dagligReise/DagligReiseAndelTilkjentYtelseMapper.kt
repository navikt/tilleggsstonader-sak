package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.util.erLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Beregningsresultat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

fun Beregningsresultat.mapTilAndelTilkjentYtelse(behandlingId: BehandlingId): List<AndelTilkjentYtelse> =
    reiser
        .flatMap { it.perioder }
        .groupBy { it.grunnlag.fom.datoEllerForrigeUkedagHvisHelg() }
        .map { (fom, reiseperioder) ->
            val målgrupper = reiseperioder.flatMap { it.grunnlag.vedtaksperioder }.map { it.målgruppe }

            require(målgrupper.distinct().size == 1) {
                "Støtter foreløpig ikke ulike målgrupper på samme utbetalingsdato"
            }

            lagAndelForDagligReise(
                behandlingId = behandlingId,
                fomUkedag = fom,
                beløp = reiseperioder.sumOf { it.beløp },
                målgruppe = målgrupper.first(),
            )
        }

private fun lagAndelForDagligReise(
    behandlingId: BehandlingId,
    fomUkedag: LocalDate,
    beløp: Int,
    målgruppe: FaktiskMålgruppe,
): AndelTilkjentYtelse {
    // TODO: Vurder om vi skal ha engangsutbetaling i stedet for dagsats.
    return AndelTilkjentYtelse(
        beløp = beløp,
        fom = fomUkedag,
        tom = fomUkedag,
        satstype = Satstype.DAG,
        type = målgruppe.tilTypeAndel(Stønadstype.DAGLIG_REISE_TSO),
        kildeBehandlingId = behandlingId,
        utbetalingsdato = fomUkedag,
    )
}

private fun LocalDate.datoEllerForrigeUkedagHvisHelg() =
    if (this.erLørdagEllerSøndag()) {
        with(TemporalAdjusters.previous(DayOfWeek.FRIDAY))
    } else {
        this
    }
