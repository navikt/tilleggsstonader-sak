package no.nav.tilleggsstonader.sak.vedtak.boutgifter

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.tilFørsteDagIMåneden
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter

object BoutgifterAndelTilkjentYtelseMapper {
    fun finnAndelTilkjentYtelse(
        saksbehandling: Saksbehandling,
        beregningsresultat: BeregningsresultatBoutgifter,
    ): List<AndelTilkjentYtelse> =
        beregningsresultat.perioder
            .sorted()
            .map {
                val førsteUkedagIMåneden = it.fom.tilFørsteDagIMåneden().datoEllerNesteMandagHvisLørdagEllerSøndag()
                AndelTilkjentYtelse(
                    beløp = it.stønadsbeløp,
                    fom = førsteUkedagIMåneden,
                    tom = førsteUkedagIMåneden,
                    satstype = Satstype.DAG,
                    type = it.grunnlag.målgruppe.tilTypeAndel(Stønadstype.BOUTGIFTER),
                    kildeBehandlingId = saksbehandling.id,
                    statusIverksetting = StatusIverksetting.fraSatsBekreftet(it.grunnlag.makssatsBekreftet),
                    utbetalingsdato = it.fom.datoEllerNesteMandagHvisLørdagEllerSøndag(),
                )
            }
}
