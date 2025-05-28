package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import java.time.LocalDate
import kotlin.collections.component1
import kotlin.collections.component2

fun BeregningsresultatLæremidler.mapTilAndelTilkjentYtelse(behandlingId: BehandlingId): List<AndelTilkjentYtelse> =
    perioder
        .groupBy { it.grunnlag.utbetalingsdato }
        .entries
        .sortedBy { (utbetalingsdato, _) -> utbetalingsdato }
        .flatMap { (utbetalingsdato, perioderMedSammeUtbetalingsdato) ->
            val førstePerioden = perioderMedSammeUtbetalingsdato.first()
            val satsBekreftet = førstePerioden.grunnlag.satsBekreftet

            feilHvisIkke(perioderMedSammeUtbetalingsdato.all { it.grunnlag.satsBekreftet == satsBekreftet }) {
                "Alle perioder for et utbetalingsdato må være bekreftet eller ikke bekreftet"
            }

            mapTilYtelse(perioderMedSammeUtbetalingsdato, utbetalingsdato, behandlingId, satsBekreftet)
        }

private fun mapTilYtelse(
    perioder: List<BeregningsresultatForMåned>,
    utbetalingsdato: LocalDate,
    behandlingId: BehandlingId,
    satsBekreftet: Boolean,
): List<AndelTilkjentYtelse> =
    perioder
        .groupBy { it.grunnlag.målgruppe.tilTypeAndel(Stønadstype.LÆREMIDLER) }
        .map { (typeAndel, perioder) ->
            AndelTilkjentYtelse(
                beløp = perioder.sumOf { it.beløp },
                fom = utbetalingsdato,
                tom = utbetalingsdato,
                satstype = Satstype.DAG,
                type = typeAndel,
                kildeBehandlingId = behandlingId,
                statusIverksetting = StatusIverksetting.fraSatsBekreftet(satsBekreftet),
                utbetalingsdato = utbetalingsdato,
            )
        }
