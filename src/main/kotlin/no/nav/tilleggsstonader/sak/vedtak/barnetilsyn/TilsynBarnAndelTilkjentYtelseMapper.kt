package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.util.tilFørsteDagIMåneden
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeGrunnlag
import java.time.LocalDate

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
): Periode<LocalDate> {
    val perioderMedBeløpsperiode: List<VedtaksperiodeGrunnlagMedBeløpsperiode> =
        beregningsresultat.perioder
            .flatMap {
                // En beløpsperiode lages av vedtaksperiodeGrunnlag, så vil finnes på samme indeks
                it.grunnlag.vedtaksperiodeGrunnlag.mapIndexed { index, grunnlag ->
                    VedtaksperiodeGrunnlagMedBeløpsperiode(grunnlag, it.beløpsperioder[index])
                }
            }

    // Ved revurdering kan vi få en beløpsperiode på siste dag i måneden som blir helg, får beløp 0 og skal utbetales neste måned.
    // Kan da få beløpsperioder/utbetalinger på samme dag, og vedtaksperioden ettfølger hverandre. Merger for å vise hele vedtaksperioden.
    val periodeMedBeløpsperiodeTilhørendeAndel =
        perioderMedBeløpsperiode
            .filter {
                // Fom og tom på andel er beløpsperiode sin dato
                it.beløpsperiode.dato == andelTilkjentYtelse.fom
            }.map { Datoperiode(it.vedtaksperiodeGrunnlag.vedtaksperiode.fom, it.vedtaksperiodeGrunnlag.vedtaksperiode.tom) }
            .sorted()
            .mergeSammenhengende()

    if (periodeMedBeløpsperiodeTilhørendeAndel.size != 1) {
        throw IllegalStateException("Forventet å finne nøyaktig én periode for andel, fant ${periodeMedBeløpsperiodeTilhørendeAndel.size}")
    }

    return periodeMedBeløpsperiodeTilhørendeAndel.single()
}

private data class VedtaksperiodeGrunnlagMedBeløpsperiode(
    val vedtaksperiodeGrunnlag: VedtaksperiodeGrunnlag,
    val beløpsperiode: Beløpsperiode,
)
