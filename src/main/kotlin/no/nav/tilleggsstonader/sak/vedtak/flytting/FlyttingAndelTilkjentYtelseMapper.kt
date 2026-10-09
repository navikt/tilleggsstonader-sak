package no.nav.tilleggsstonader.sak.vedtak.flytting

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.libs.feil.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagEgenKjøring
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlyttebyrå
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlytting
import java.math.BigDecimal

fun BeregningsresultatFlytting.mapTilAndeler(stønadstype: Stønadstype): List<AndelTilkjentYtelse> =
    resultater
        .filter { resultat ->
            when (val grunnlag = resultat.grunnlag) {
                is BeregningsgrunnlagFlyttebyrå -> grunnlag.erBetalingDokumentert
                is BeregningsgrunnlagEgenKjøring -> true
            }
        }.map { resultat ->
            val typeAndel =
                when (stønadstype) {
                    // TODO Hva er beste måten å faktisk hente ut målgruppe på? Hva bør vi lagre på beregningsgrunnlaget?
                    // Trenger vi bare å lagre målgruppe eller skal vi lagre vedtaksperiodene også
                    Stønadstype.FLYTTING_TSO -> TypeAndel.FLYTTING_AAP
                    Stønadstype.FLYTTING_TSR -> TypeAndel.FLYTTING_ARBEIDSSØKER
                    else -> feil("Flytting kan ikke opprette andeler for stønadstype=$stønadstype")
                }
            val beløp = resultat.beløp
            feilHvis(beløp < BigDecimal.ZERO || beløp > Int.MAX_VALUE.toBigDecimal()) {
                "Flyttebeløpet kan ikke lagres som andel"
            }
            val satsBekreftet = (resultat.grunnlag as? BeregningsgrunnlagEgenKjøring)?.satsBekreftet ?: true
            val dato = resultat.fom.datoEllerNesteMandagHvisLørdagEllerSøndag()
            AndelTilkjentYtelse(
                beløp = beløp.intValueExact(),
                fom = dato,
                tom = dato,
                satstype = Satstype.DAG,
                type = typeAndel,
                statusIverksetting = StatusIverksetting.fraSatsBekreftet(satsBekreftet),
                utbetalingsdato = dato,
            )
        }
