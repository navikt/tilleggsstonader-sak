package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.util.tilFørsteDagIMåneden
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn

fun BeregningsresultatTilsynBarn.mapTilAndelTilkjentYtelse(saksbehandling: Saksbehandling): List<AndelTilkjentYtelse> =
    perioder.flatMap {
        it.beløpsperioder.map { beløpsperiode ->
            val satstype = Satstype.DAG
            val førsteHverdagIMåneden =
                beløpsperiode.dato
                    .tilFørsteDagIMåneden()
                    .datoEllerNesteMandagHvisLørdagEllerSøndag()
            AndelTilkjentYtelse(
                beløp = beløpsperiode.beløp,
                fom = beløpsperiode.dato,
                tom = beløpsperiode.dato,
                satstype = satstype,
                type = beløpsperiode.målgruppe.tilTypeAndel(Stønadstype.BARNETILSYN),
                kildeBehandlingId = saksbehandling.id,
                utbetalingsdato = førsteHverdagIMåneden,
            )
        }
    }

fun finnPeriodeFraAndel(
    beregningsresultat: BeregningsresultatTilsynBarn,
    andelTilkjentYtelse: AndelTilkjentYtelse,
) = beregningsresultat.perioder
    .flatMap {
        // Lages andel per beløpsperiode, som igjen lages fra vedtaksperiodeGrunnlag
        it.grunnlag.vedtaksperiodeGrunnlag.mapIndexed { index, grunnlag ->
            grunnlag to it.beløpsperioder[index]
        }
    }.single {
        // Fom og tom på andel er beløpsperiode sin dato
        it.second.dato == andelTilkjentYtelse.fom
    }.first.vedtaksperiode
