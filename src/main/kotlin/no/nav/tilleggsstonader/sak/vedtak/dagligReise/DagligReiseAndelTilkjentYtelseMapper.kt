package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Beregningsresultat
import java.time.LocalDate

fun Beregningsresultat.mapTilAndelTilkjentYtelse(behandlingId: BehandlingId): List<AndelTilkjentYtelse> =
    reiser
        .flatMap { it.perioder }
        .groupBy { it.grunnlag.fom }
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
): AndelTilkjentYtelse =
    AndelTilkjentYtelse(
        beløp = beløp,
        fom = fomUkedag,
        tom = fomUkedag,
        satstype = Satstype.ENGANGSBELØP,
        type = målgruppe.tilTypeAndel(Stønadstype.DAGLIG_REISE_TSO),
        kildeBehandlingId = behandlingId,
        utbetalingsdato = fomUkedag,
    )
