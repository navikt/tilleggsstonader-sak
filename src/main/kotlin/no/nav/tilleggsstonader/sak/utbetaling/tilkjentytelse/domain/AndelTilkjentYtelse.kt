package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain

import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.Månedsperiode
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

/**
 * [AndelTilkjentYtelse] inneholder informasjon om en andel er utbetalt eller ikke
 * Når man iverksetter lagres informasjon ned i [iverksetting] og [status] blir oppdatert
 * Når man fått kvittering fra økonomi oppdateres [status] på nytt
 *
 * @param iverksetting når vi iverksetter en andel så oppdateres dette feltet med id og tidspunkt
 */
data class AndelTilkjentYtelse(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column("belop")
    val beløp: Int,
    val fom: LocalDate,
    val tom: LocalDate,
    val satstype: Satstype,
    val type: StønadstypeIverksetting,
    val kildeBehandlingId: UUID,
    @Version
    val version: Int = 0,
    val status: StatusIverksetting = StatusIverksetting.UBEHANDLET,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
    val iverksetting: Iverksetting? = null,
    @LastModifiedDate
    val endretTid: LocalDateTime = SporbarUtils.now(),
) {

    init {
        feilHvis(YearMonth.from(fom) != YearMonth.from(tom)) {
            "For å unngå at man iverksetter frem i tiden skal perioder kun løpe over 1 måned"
        }
    }

    fun erStønadOverlappende(fom: LocalDate): Boolean = this.periode.inneholder(YearMonth.from(fom))

    // Kan vi bruke periode direkt i domeneobjektet?
    val periode get() = Månedsperiode(fom, tom)
}

data class Iverksetting(
    val iverksettingId: UUID,
    val iverksettingTidspunkt: LocalDateTime,
)

/**
 * Det er ønskelig at ulike typer brukes for ulike periodetyper.
 * Disse mappes fra stønadsperioden sin type målgruppe
 */
enum class StønadstypeIverksetting {
    TILSYN_BARN_ENSLIG_FORSØRGER,
    TILSYN_BARN_AAP,
    TILSYN_BARN_ETTERLATTE,
}

enum class Satstype {
    DAG,
    MÅNED,
    ENGANGS,
}

/**
 * Når man oppretter andelen er statusen [UBEHANDLET]
 * Når man iverksetter oppdateres status til [SENDT]
 * Når man mottatt kvittering oppdateres statusen til [OK]
 * Hvis det er lagret en andel som ikke blir iverksatt fordi en ny behandling blir gjeldende, settes status til [UAKTUELL]
 */
enum class StatusIverksetting {
    UBEHANDLET,
    SENDT,
    OK,
    UAKTUELL,
}
