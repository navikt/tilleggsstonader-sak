package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse

import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Iverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.*

@Service
class TilkjentYtelseService(
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
) {

    fun hentForBehandlingEllerNull(behandlingId: UUID): TilkjentYtelse? {
        return tilkjentYtelseRepository.findByBehandlingId(behandlingId)
    }

    fun hentForBehandling(behandlingId: UUID): TilkjentYtelse {
        return tilkjentYtelseRepository.findByBehandlingId(behandlingId)
            ?: error("Fant ikke tilkjent ytelse med behandlingsid $behandlingId")
    }

    fun opprettTilkjentYtelse(nyTilkjentYtelse: TilkjentYtelse): TilkjentYtelse {
        return tilkjentYtelseRepository.insert(nyTilkjentYtelse)
    }

    fun harLøpendeUtbetaling(behandlingId: UUID): Boolean {
        return tilkjentYtelseRepository.findByBehandlingId(behandlingId)
            ?.let { it.andelerTilkjentYtelse.any { andel -> andel.tom.isAfter(osloDateNow()) } } ?: false
    }

    fun slettTilkjentYtelseForBehandling(saksbehandling: Saksbehandling) {
        brukerfeilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke reberegne tilkjent ytelse for en behandling som er låst for videre redigering"
        }
        tilkjentYtelseRepository.findByBehandlingId(saksbehandling.id)?.let {
            tilkjentYtelseRepository.deleteById(it.id)
        }
    }

    /**
     * Legger til en nullandel ved første iverksetting for en behandling, dersom det ikke finnes andeler som skal iverksettes
     * for forrige måned. Dette er for å kunne sjekke status på iverksetting uten utbetalinger.
     *
     * @return den nye nullandelen som man kan sjekke status på iverksetting for
     */
    fun leggTilNullAndel(tilkjentYtelse: TilkjentYtelse, iverksetting: Iverksetting, måned: YearMonth): AndelTilkjentYtelse {
        val nullAndel = AndelTilkjentYtelse(
            beløp = 0,
            fom = måned.atDay(1),
            tom = måned.atDay(1),
            satstype = Satstype.UGYLDIG,
            type = TypeAndel.UGYLDIG,
            kildeBehandlingId = tilkjentYtelse.behandlingId,
            iverksetting = iverksetting,
            statusIverksetting = StatusIverksetting.SENDT,
        )

        tilkjentYtelseRepository.update(tilkjentYtelse.copy(andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse + nullAndel))
        return nullAndel
    }
}
