package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.util.erLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.util.toYearMonth
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
 * Når man iverksetter lagres informasjon ned i [iverksetting] og [statusIverksetting] blir oppdatert
 * Når man fått kvittering fra økonomi oppdateres [statusIverksetting] på nytt
 *
 * @param iverksetting når vi iverksetter en andel så oppdateres dette feltet med id og tidspunkt
 * @param utbetalingsmåned måneden perioden skal iverksettes
 */
data class AndelTilkjentYtelse(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column("belop")
    val beløp: Int,
    val fom: LocalDate,
    val tom: LocalDate,
    val satstype: Satstype,
    val type: TypeAndel,
    val kildeBehandlingId: BehandlingId,
    @Version
    val version: Int = 0,
    val statusIverksetting: StatusIverksetting = StatusIverksetting.UBEHANDLET,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL)
    val iverksetting: Iverksetting? = null,
    @Column("utbetalingsmaned")
    val utbetalingsmåned: YearMonth = fom.toYearMonth(),
    @LastModifiedDate
    val endretTid: LocalDateTime = SporbarUtils.now(),
) {

    init {
        feilHvis(YearMonth.from(fom) != YearMonth.from(tom)) {
            "For å unngå at man iverksetter frem i tiden skal perioder kun løpe over 1 måned"
        }
        feilHvis(utbetalingsmåned > fom.toYearMonth()) {
            "Finnes ingen grunn til å sette utbetalingsdato etter gjeldende dato for perioden"
        }

        validerDataForType()
    }

    private fun validerDataForType() {
        when (type) {
            TypeAndel.TILSYN_BARN_ENSLIG_FORSØRGER,
            TypeAndel.TILSYN_BARN_AAP,
            TypeAndel.TILSYN_BARN_ETTERLATTE,
            -> validerTilsynBarn()

            TypeAndel.LÆREMIDLER_ENSLIG_FORSØRGER,
            TypeAndel.LÆREMIDLER_AAP,
            TypeAndel.LÆREMIDLER_ETTERLATTE,
            -> validerLæremidler()

            TypeAndel.UGYLDIG -> {}
        }
    }

    private fun validerTilsynBarn() {
        validerSatstype(Satstype.DAG)
        validerLikFomOgTom()
        feilHvis(satstype == Satstype.DAG && fom.erLørdagEllerSøndag()) {
            "Dagsats som begynner en lørdag eller søndag vil ikke bli utbetalt"
        }
    }

    private fun validerLæremidler() {
        error("Har ikke lagt inn validering av andeler for læremidler ennå")
    }

    private fun validerSatstype(forventetSatstype: Satstype) {
        feilHvis(satstype != forventetSatstype) {
            "Forventer at satstype=$satstype er av type=$forventetSatstype"
        }
    }

    private fun validerLikFomOgTom() {
        feilHvis(fom != tom) {
            "Forventer at fom=tom for type=$type"
        }
    }
}

data class Iverksetting(
    val iverksettingId: UUID,
    val iverksettingTidspunkt: LocalDateTime,
)

/**
 * Det er ønskelig at ulike typer brukes for ulike periodetyper.
 * Disse mappes fra stønadsperioden sin type målgruppe
 * [TypeAndel.UGYLDIG] Brukes for å lagre ned andeler med nullbeløp. Disse skal ikke iverksettes,
 *  men nødvendige for å iverksette første gang og ved opphør tilbake i tid.
 */
enum class TypeAndel {
    TILSYN_BARN_ENSLIG_FORSØRGER,
    TILSYN_BARN_AAP,
    TILSYN_BARN_ETTERLATTE,

    LÆREMIDLER_ENSLIG_FORSØRGER,
    LÆREMIDLER_AAP,
    LÆREMIDLER_ETTERLATTE,

    UGYLDIG,
}

/**
 * [Satstype.UGYLDIG] Brukes for å lagre ned andeler med nullbeløp. Disse skal ikke iverksettes,
 * men nødvendige for å iverksette første gang og ved opphør tilbake i tid.
 */
enum class Satstype {
    DAG,
    MÅNED,
    ENGANGSBELØP,
    UGYLDIG,
}

/**
 * Når man oppretter andelen er statusen [StatusIverksetting.UBEHANDLET]
 * Når man iverksetter oppdateres status til [StatusIverksetting.SENDT]
 * Når man mottatt kvittering oppdateres statusen til [StatusIverksetting.OK]
 * Hvis det er lagret en andel som ikke blir iverksatt fordi en ny behandling blir gjeldende, settes status til [StatusIverksetting.UAKTUELL]
 */
enum class StatusIverksetting {
    UBEHANDLET,
    SENDT,
    OK,
    OK_UTEN_UTBETALING,
    UAKTUELL,
    VENTER_PÅ_SATS_ENDRING,
    ;

    fun erOk() = this == StatusIverksetting.OK || this == StatusIverksetting.OK_UTEN_UTBETALING
}
