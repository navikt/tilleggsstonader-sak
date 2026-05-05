package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Iverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import java.time.LocalDate

object TilkjentYtelseUtil {
    fun tilkjentYtelse(
        behandlingId: BehandlingId,
        vararg andeler: AndelTilkjentYtelse = arrayOf(andelTilkjentYtelse()),
    ): TilkjentYtelse =
        TilkjentYtelse(
            behandlingId = behandlingId,
            andelerTilkjentYtelse = andeler.toSet(),
        )

    fun andelTilkjentYtelse(
        beløp: Int = 11554,
        fom: LocalDate = LocalDate.of(2021, 1, 1),
        tom: LocalDate = fom,
        satstype: Satstype = Satstype.DAG,
        type: TypeAndel = TypeAndel.TILSYN_BARN_AAP,
        statusIverksetting: StatusIverksetting = StatusIverksetting.UBEHANDLET,
        iverksetting: Iverksetting? = null,
        utbetalingsdato: LocalDate = fom,
    ) = AndelTilkjentYtelse(
        beløp = beløp,
        fom = fom,
        tom = tom,
        satstype = satstype,
        type = type,
        statusIverksetting = statusIverksetting,
        iverksetting = iverksetting,
        utbetalingsdato = utbetalingsdato,
    )

    fun nullAndel(
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate = fom,
        statusIverksetting: StatusIverksetting = StatusIverksetting.SENDT,
        iverksetting: Iverksetting? = null,
        utbetalingsdato: LocalDate = fom,
    ) = AndelTilkjentYtelse(
        beløp = 0,
        fom = fom,
        tom = tom,
        satstype = Satstype.UGYLDIG,
        type = TypeAndel.UGYLDIG,
        statusIverksetting = statusIverksetting,
        iverksetting = iverksetting,
        utbetalingsdato = utbetalingsdato,
    )
}
