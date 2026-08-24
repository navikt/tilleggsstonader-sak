package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatOffentligTransport

fun BeregningReiseTilSamling.mapTilAndelTilkentYtelse(): List<AndelTilkjentYtelse> =


    lagAndelForReiseTilSamling()


private fun lagAndelForReiseTilSamling(
    saksbehandling: Saksbehandling
    fomUkedag
)