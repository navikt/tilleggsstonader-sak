package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Iverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth

@Service
class TilkjentYtelseService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {
    fun hentForBehandlingEllerNull(behandlingId: BehandlingId): TilkjentYtelse? = tilkjentYtelseRepository.findByBehandlingId(behandlingId)

    fun hentForBehandling(behandlingId: BehandlingId): TilkjentYtelse =
        tilkjentYtelseRepository.findByBehandlingId(behandlingId)
            ?: error("Fant ikke tilkjent ytelse med behandlingsid $behandlingId")

    fun hentForBehandlingMedLås(behandlingId: BehandlingId): TilkjentYtelse =
        tilkjentYtelseRepository.findByBehandlingIdForUpdate(behandlingId)
            ?: error("Fant ikke tilkjent ytelse med behandlingsid $behandlingId")

    fun lagreTilkjentYtelse(
        behandlingId: BehandlingId,
        andeler: List<AndelTilkjentYtelse>,
    ): TilkjentYtelse =
        tilkjentYtelseRepository.insert(
            TilkjentYtelse(
                behandlingId = behandlingId,
                andelerTilkjentYtelse = andeler.toSet(),
            ),
        )

    fun harLøpendeUtbetaling(behandlingId: BehandlingId): Boolean =
        tilkjentYtelseRepository
            .findByBehandlingId(behandlingId)
            ?.let { it.andelerTilkjentYtelse.any { andel -> andel.tom.isAfter(LocalDate.now()) } } ?: false

    fun slettTilkjentYtelseForBehandling(saksbehandling: Saksbehandling) {
        saksbehandling.status.validerKanBehandlingRedigeres()
        tilkjentYtelseRepository.findByBehandlingId(saksbehandling.id)?.let {
            tilkjentYtelseRepository.deleteById(it.id)
        }
    }

    /**
     * Legger til en nullandel ved første iverksetting i en behandling,
     * om det ikke finnes andeler som skal iverksettes på innvilgelsestidspunktet.
     * Brukes for å sjekke status på iverksetting uten utbetalinger.
     *
     * Brukes kun dersom man skal opphøre alle tidligere andeler på saken.
     *
     * Fom på nullandelen settes til første dag i [måned]
     *
     * @return den nye nullandelen som man kan sjekke status på iverksetting for
     */
    fun leggTilNullAndel(
        tilkjentYtelse: TilkjentYtelse,
        iverksetting: Iverksetting,
        måned: YearMonth,
    ): AndelTilkjentYtelse {
        val nullAndel =
            AndelTilkjentYtelse(
                beløp = 0,
                fom = måned.atDay(1),
                tom = måned.atDay(1),
                satstype = Satstype.UGYLDIG,
                type = TypeAndel.UGYLDIG,
                iverksetting = iverksetting,
                statusIverksetting = StatusIverksetting.SENDT,
                utbetalingsdato = måned.atDay(1),
            )

        tilkjentYtelseRepository.update(tilkjentYtelse.copy(andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse + nullAndel))
        return nullAndel
    }

    fun settAndelerForBehandlingTilUaktuellHvisFinnes(behandlingId: BehandlingId) {
        val tilkjentYtelse = hentForBehandlingEllerNull(behandlingId)
        if (tilkjentYtelse != null && tilkjentYtelse.andelerTilkjentYtelse.isNotEmpty()) {
            logger.info("Setter andeler for behandling $behandlingId til uaktuell")
            tilkjentYtelseRepository.update(
                tilkjentYtelse.copy(
                    andelerTilkjentYtelse =
                        tilkjentYtelse.andelerTilkjentYtelse
                            .map { it.copy(statusIverksetting = StatusIverksetting.UAKTUELL) }
                            .toSet(),
                ),
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TilkjentYtelseService::class.java)
    }
}
