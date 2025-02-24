package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse

import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Iverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Satstype
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.toYearMonth
import org.springframework.stereotype.Service
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

    fun opprettTilkjentYtelse(
        saksbehandling: Saksbehandling,
        andeler: List<AndelTilkjentYtelse>,
    ): TilkjentYtelse =
        tilkjentYtelseRepository.insert(
            TilkjentYtelse(
                behandlingId = saksbehandling.id,
                andelerTilkjentYtelse = andeler.toSet(),
            ),
        )

    fun harLøpendeUtbetaling(behandlingId: BehandlingId): Boolean =
        tilkjentYtelseRepository
            .findByBehandlingId(behandlingId)
            ?.let { it.andelerTilkjentYtelse.any { andel -> andel.tom.isAfter(osloDateNow()) } } ?: false

    fun slettTilkjentYtelseForBehandling(saksbehandling: Saksbehandling) {
        brukerfeilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke reberegne tilkjent ytelse for en behandling har status ${saksbehandling.status.visningsnavn()}"
        }
        tilkjentYtelseRepository.findByBehandlingId(saksbehandling.id)?.let {
            tilkjentYtelseRepository.deleteById(it.id)
        }
    }

    /**
     * Legger til en nullandel ved første iverksetting i en behandling,
     * om det ikke finnes andeler som skal iverksettes på innvilgelsestidspunktet.
     * Brukes for å sjekke status på iverksetting uten utbetalinger.
     *
     * Dette gjelder dersom det ikke finnes noe å iverksette, kun finnes andeler fremover i tid, eller om det kun
     * eksisterer andeler som venter på satsendring før de skal iverksettes.
     *
     * Fom på nullandelen settes til det minste av måneden man iverksetter i og måneden før første andel.
     * Eksempel: Hvis man i januar innvilger noe for jan-mai som ikke har sats, legges en nullandel for desember inn.
     *
     * @return den nye nullandelen som man kan sjekke status på iverksetting for
     */
    fun leggTilNullAndel(
        tilkjentYtelse: TilkjentYtelse,
        iverksetting: Iverksetting,
        måned: YearMonth,
    ): AndelTilkjentYtelse {
        val månedForNullutbetaling = minOf(måned, finnMånedFørFørsteAndel(tilkjentYtelse) ?: måned)
        val nullAndel =
            AndelTilkjentYtelse(
                beløp = 0,
                fom = månedForNullutbetaling.atDay(1),
                tom = månedForNullutbetaling.atDay(1),
                satstype = Satstype.UGYLDIG,
                type = TypeAndel.UGYLDIG,
                kildeBehandlingId = tilkjentYtelse.behandlingId,
                iverksetting = iverksetting,
                statusIverksetting = StatusIverksetting.SENDT,
                utbetalingsdato = månedForNullutbetaling.atDay(1),
            )

        tilkjentYtelseRepository.update(tilkjentYtelse.copy(andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse + nullAndel))
        return nullAndel
    }

    private fun finnMånedFørFørsteAndel(tilkjentYtelse: TilkjentYtelse): YearMonth? =
        tilkjentYtelse.andelerTilkjentYtelse
            .minOfOrNull { it.fom }
            ?.toYearMonth()
            ?.minusMonths(1)
}
