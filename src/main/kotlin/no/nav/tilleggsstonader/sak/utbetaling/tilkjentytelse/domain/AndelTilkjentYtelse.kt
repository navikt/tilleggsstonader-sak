package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.util.erFørsteDagIMåneden
import no.nav.tilleggsstonader.sak.util.erLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.util.erSisteDagIMåneden
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
 * @param utbetalingsdato datoen perioden skal iverksettes
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
    @LastModifiedDate
    val endretTid: LocalDateTime = SporbarUtils.now(),
    val utbetalingsdato: LocalDate,
) {
    init {
        feilHvis(YearMonth.from(fom) != YearMonth.from(tom)) {
            "Forventer at fom($fom) og tom($tom) er i den samme måneden"
        }
        feilHvisIkke(fom <= tom) {
            "Forventer at fom($fom) er mindre eller lik tom($tom)"
        }
        if (satstype == Satstype.MÅNED) {
            validerFørsteOgSisteIMåneden()
        }
        if (satstype == Satstype.DAG) {
            validerLikFomOgTom()
            validerFomIkkeLørdagEllerSøndag()
        }

        validerDataForType()

        validerUtbetalingMaksFørsteArbeidsdagNesteMåned()
    }

    private fun validerDataForType() {
        when (type) {
            TypeAndel.TILSYN_BARN_ENSLIG_FORSØRGER,
            TypeAndel.TILSYN_BARN_AAP,
            TypeAndel.TILSYN_BARN_ETTERLATTE,

            TypeAndel.LÆREMIDLER_ENSLIG_FORSØRGER,
            TypeAndel.LÆREMIDLER_AAP,
            TypeAndel.LÆREMIDLER_ETTERLATTE,

            TypeAndel.BOUTGIFTER_AAP,
            TypeAndel.BOUTGIFTER_ENSLIG_FORSØRGER,
            TypeAndel.BOUTGIFTER_ETTERLATTE,
            -> validerErDagsats()

            TypeAndel.UGYLDIG -> {}
        }
    }

    private fun validerFomIkkeLørdagEllerSøndag() {
        feilHvis(fom.erLørdagEllerSøndag()) {
            "Dagsats som begynner en lørdag eller søndag vil ikke bli utbetalt"
        }
    }

    private fun validerErDagsats() {
        feilHvis(satstype != Satstype.DAG) {
            "Ugyldig satstype=$satstype forventetSatsType=${Satstype.DAG} for type=$type"
        }
    }

    private fun validerLikFomOgTom() {
        feilHvis(fom != tom) {
            "Forventer at fom($fom) er lik tom($tom) for type=$type"
        }
    }

    private fun validerFørsteOgSisteIMåneden() {
        feilHvisIkke(fom.erFørsteDagIMåneden()) {
            "Forventer at fom($fom) er første dagen i måneden for type=$type"
        }
        feilHvisIkke(tom.erSisteDagIMåneden()) {
            "Forventer at tom($tom) er siste dagen i måneden for type=$type"
        }
    }

    private fun validerUtbetalingMaksFørsteArbeidsdagNesteMåned() {
        val fomMåned = fom.toYearMonth()
        val førsteArbeidsdagNesteMåned =
            fomMåned.plusMonths(1).atDay(1).datoEllerNesteMandagHvisLørdagEllerSøndag()

        feilHvis(utbetalingsdato != førsteArbeidsdagNesteMåned && utbetalingsdato.toYearMonth() != fomMåned) {
            "Utbetalingsdato($utbetalingsdato) må være i samme måned ($fomMåned) som andelen gjelder for, eller første arbeidsdag i måneden etter."
        }
    }
}

data class Iverksetting(
    val iverksettingId: UUID,
    val iverksettingTidspunkt: LocalDateTime,
)

/**
 * Det er ønskelig at ulike typer brukes for ulike periodetyper.
 * Disse mappes fra vedtaksperiode sin type målgruppe
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

    BOUTGIFTER_AAP,
    BOUTGIFTER_ENSLIG_FORSØRGER,
    BOUTGIFTER_ETTERLATTE,

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

    fun erOk() = this == OK || this == OK_UTEN_UTBETALING

    companion object {
        /**
         * Hvis utbetalingsmåneden er fremover i tid og det er nytt år så skal det ventes på satsendring før iverksetting.
         */
        fun fraSatsBekreftet(satsBekreftet: Boolean): StatusIverksetting {
            if (!satsBekreftet) {
                return VENTER_PÅ_SATS_ENDRING
            }

            return UBEHANDLET
        }
    }
}
