package no.nav.tilleggsstonader.sak.iverksett

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.TilkjentYtelseMedMetadata
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.kontrakt.AndelTilkjentYtelseDto
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.kontrakt.TilkjentYtelseDto
import java.time.LocalDate
import java.time.YearMonth

fun TilkjentYtelse.tilIverksettDto(): TilkjentYtelseDto = TilkjentYtelseDto(
    andelerTilkjentYtelse = andelerTilkjentYtelse.map { andel -> andel.tilIverksettDto() },
    startmåned = YearMonth.from(startdato),
)

fun AndelTilkjentYtelse.tilIverksettDto() =
    AndelTilkjentYtelseDto(
        beløp = this.beløp,
        periode = this.periode,
        kildeBehandlingId = this.kildeBehandlingId,
    )

fun TilkjentYtelse.tilTilkjentYtelseMedMetaData(
    saksbehandlerId: String,
    eksternBehandlingId: Long,
    stønadstype: Stønadstype,
    eksternFagsakId: Long,
    vedtaksdato: LocalDate,
): TilkjentYtelseMedMetadata {
    return TilkjentYtelseMedMetadata(
        tilkjentYtelse = this.tilIverksettDto(),
        saksbehandlerId = saksbehandlerId,
        eksternBehandlingId = eksternBehandlingId,
        stønadstype = stønadstype,
        eksternFagsakId = eksternFagsakId,
        behandlingId = this.behandlingId,
        vedtaksdato = vedtaksdato,
    )
}
