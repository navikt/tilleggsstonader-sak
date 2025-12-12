package no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.libs.log.logger
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feil
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
 * @fom styrer når betalingen skal inntreffe i OS. For dagsats må dette være en dag OS er åpent (altså ikke en helge- eller helligdag).
 * Denne begrensningen gjelder ikke for engangsbeløp.
 * @param iverksetting når vi iverksetter en andel så oppdateres dette feltet med id og tidspunkt
 * @param utbetalingsdato bestemmer når prosessering skal plukke opp betalingen og sende den til utsjekk.
 */
data class AndelTilkjentYtelse(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column("belop")
    val beløp: Int,
    override val fom: LocalDate,
    override val tom: LocalDate,
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
    val brukersNavKontor: String? = null,
) : Periode<LocalDate> {
    init {
        feilHvis(YearMonth.from(fom) != YearMonth.from(tom)) {
            "Forventer at fom($fom) og tom($tom) er i den samme måneden"
        }
        feilHvisIkke(fom <= tom) {
            "Forventer at fom($fom) er mindre eller lik tom($tom)"
        }
        when (satstype) {
            Satstype.DAG -> {
                validerLikFomOgTom()
                validerFomIkkeLørdagEllerSøndag()
            }
            Satstype.MÅNED -> validerFørsteOgSisteIMåneden()
            Satstype.ENGANGSBELØP -> validerKrysserIkkeÅrsskifte()
            Satstype.UGYLDIG -> {}
        }
        validerAndelHarFåttRettSatstype()
        validerUtbetalingMaksFørsteArbeidsdagNesteMåned()
    }

    private fun validerAndelHarFåttRettSatstype() {
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

            TypeAndel.DAGLIG_REISE_AAP,
            TypeAndel.DAGLIG_REISE_ENSLIG_FORSØRGER,
            TypeAndel.DAGLIG_REISE_ETTERLATTE,
            TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSFORBEREDENDE,
            TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSRETTET_REHAB,
            TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSTRENING,
            TypeAndel.DAGLIG_REISE_TILTAK_AVKLARING,
            TypeAndel.DAGLIG_REISE_TILTAK_DIGITAL_JOBBKLUBB,
            TypeAndel.DAGLIG_REISE_TILTAK_ENKELTPLASS_AMO,
            TypeAndel.DAGLIG_REISE_TILTAK_ENKELTPLASS_FAG_YRKE_HOYERE_UTD,
            TypeAndel.DAGLIG_REISE_TILTAK_FORSØK_OPPLÆRINGSTILTAK_LENGER_VARIGHET,
            TypeAndel.DAGLIG_REISE_TILTAK_GRUPPE_AMO,
            TypeAndel.DAGLIG_REISE_TILTAK_GRUPPE_FAG_YRKE_HOYERE_UTD,
            TypeAndel.DAGLIG_REISE_TILTAK_HØYERE_UTDANNING,
            TypeAndel.DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE,
            TypeAndel.DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE_UNG,
            TypeAndel.DAGLIG_REISE_TILTAK_JOBBKLUBB,
            TypeAndel.DAGLIG_REISE_TILTAK_OPPFØLGING,
            TypeAndel.DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_NAV,
            TypeAndel.DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_OPPLÆRING,

            -> satstype skalVære Satstype.DAG

            TypeAndel.UGYLDIG -> {}
        }
    }

    private fun validerFomIkkeLørdagEllerSøndag() {
        feilHvis(fom.erLørdagEllerSøndag()) {
            "Dagsats som begynner en lørdag eller søndag vil ikke bli utbetalt"
        }
    }

    private infix fun Satstype.skalVære(forventetSatstype: Satstype) {
        feilHvis(this != forventetSatstype) {
            "Forventet satstype=$forventetSatstype for type=$type, men fikk $this"
        }
    }

    private fun validerLikFomOgTom() {
        feilHvis(fom != tom) {
            "Forventer at fom($fom) er lik tom($tom) for type=$type"
        }
    }

    private fun validerFørsteOgSisteIMåneden() {
        feilHvisIkke(fom.erFørsteDagIMåneden()) {
            "Forventer at fom($fom) er første dagen i måneden for ty" +
                "pe=$type"
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

    private fun validerKrysserIkkeÅrsskifte() {
        if (fom.year != tom.year) {
            logger.error("andel med id ${this.id} strekker seg over et årsskifte, men det tillater ikke OS for satstype=ENG")
            feil("Utbetalingen kan ikke krysse et årsskifte, den må da splittes i to")
        }
    }

    fun erNullandel() = beløp == 0 && satstype == Satstype.UGYLDIG && type == TypeAndel.UGYLDIG
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

    DAGLIG_REISE_AAP,
    DAGLIG_REISE_ENSLIG_FORSØRGER,
    DAGLIG_REISE_ETTERLATTE,

    DAGLIG_REISE_TILTAK_ARBEIDSFORBEREDENDE, // ARBFORB - TSDRAFT-OP
    DAGLIG_REISE_TILTAK_ARBEIDSRETTET_REHAB, // ARBRRHDAG - TSDRARREHABAGDAG-OP
    DAGLIG_REISE_TILTAK_ARBEIDSTRENING, // ARBTREN - TSDRATTT2-OP
    DAGLIG_REISE_TILTAK_AVKLARING, // AVKLARAG - TSDRAAG-OP
    DAGLIG_REISE_TILTAK_DIGITAL_JOBBKLUBB, // DIGIOPPARB - TSDRDIGJK-OP
    DAGLIG_REISE_TILTAK_ENKELTPLASS_AMO, // ENKELAMO - TSDREPAMO-OP
    DAGLIG_REISE_TILTAK_ENKELTPLASS_FAG_YRKE_HOYERE_UTD, // ENKFAGYRKE - TSDREPVGSHOY-OP
    DAGLIG_REISE_TILTAK_FORSØK_OPPLÆRINGSTILTAK_LENGER_VARIGHET, // FORSOPPLEV - TSDRFOLV
    DAGLIG_REISE_TILTAK_GRUPPE_AMO, // GRUPPEAMO - TSDRGRAMO-OP
    DAGLIG_REISE_TILTAK_GRUPPE_FAG_YRKE_HOYERE_UTD, // GRUFAGYRKE - TSDRGRVGSHOY-OP
    DAGLIG_REISE_TILTAK_HØYERE_UTDANNING, // HOYEREUTD - TSDRHOYUTD-OP
    DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE, // INDJOBSTOT - TSDRIPS-OP
    DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE_UNG, // IPSUNG - TSDRIPSUNG-OP
    DAGLIG_REISE_TILTAK_JOBBKLUBB, // JOBBK - TSDRJB2009-OP
    DAGLIG_REISE_TILTAK_OPPFØLGING, // INDOPPFAG - TSDROPPFAG2-OP
    DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_NAV, // UTVAOONAV - TSDRUTVAVKLOPPF-OP
    DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_OPPLÆRING, // UTVOPPFOPL - TSDRUTVOPPFOPPL-OP

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
 * TODO: Fyll inn resterende statuser når vi får info om hva de betyr
 */
enum class StatusIverksetting {
    UBEHANDLET,
    SENDT,
    OK,
    OK_UTEN_UTBETALING,
    UAKTUELL,
    VENTER_PÅ_SATS_ENDRING,

    FEILET,
    MOTTATT,
    HOS_OPPDRAG,
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
