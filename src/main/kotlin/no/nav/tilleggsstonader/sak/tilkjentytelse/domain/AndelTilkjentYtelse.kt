package no.nav.tilleggsstonader.sak.tilkjentytelse.domain

import no.nav.tilleggsstonader.sak.tilkjentytelse.kontrakt.AndelTilkjentYtelseDto
import no.nav.tilleggsstonader.sak.util.Månedsperiode
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

data class AndelTilkjentYtelse(
    @Column("belop")
    val beløp: Int,
    @Column("stonad_fom")
    val stønadFom: LocalDate,
    @Column("stonad_tom")
    val stønadTom: LocalDate,
    val kildeBehandlingId: UUID,
) {

    constructor(
        beløp: Int,
        periode: Månedsperiode,
        kildeBehandlingId: UUID,
    ) : this(
        beløp,
        periode.fom.atDay(1),
        periode.tom.atEndOfMonth(),
        kildeBehandlingId,
    )

    fun erStønadOverlappende(fom: LocalDate): Boolean = this.periode.inneholder(YearMonth.from(fom))

    // Kan vi bruke periode direkt i domeneobjektet?
    val periode get() = Månedsperiode(stønadFom, stønadTom)

    fun tilIverksettDto() =
        AndelTilkjentYtelseDto(
            beløp = this.beløp,
            periode = this.periode,
            kildeBehandlingId = this.kildeBehandlingId,
        )
}
