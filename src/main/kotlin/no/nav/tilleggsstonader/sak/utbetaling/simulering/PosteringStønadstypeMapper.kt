package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.utbetaling.UtbetalingFagområde
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Periode
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.PosteringType
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import org.slf4j.LoggerFactory
import java.time.YearMonth

/**
 * Brukes til å finne ut hvor mye av beløpet i en periode fra simuleringen som tilhører andre
 * stønadstyper enn den behandlingen selv gjelder for. Dette kan forekomme fordi simuleringen viser
 * alle posteringer på samme fagområde, uavhengig av hvilken fagsak/stønadstype de tilhører.
 */
object PosteringStønadstypeMapper {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Grupperer posteringer per måned (ut fra postering sin fom), og summerer beløp per
     * [Stønadstype] som ikke er [egenStønadstype]. Kun posteringer av type
     * [PosteringType.YTELSE] vurderes. Posteringer med en klassekode vi ikke klarer å mappe til
     * en [TypeAndel]/[Stønadstype] ignoreres.
     *
     * Ved opphør kan en dag vises som en postering med et beløp og en tilsvarende negativ
     * postering med samme beløp, som til sammen summerer til 0. Slike stønadstyper filtreres bort
     * siden de ikke representerer en reell utbetaling som påvirker simuleringen.
     */
    fun beløpFraAndreStønadstyperPerMåned(
        perioder: List<Periode>,
        egenStønadstype: Stønadstype,
    ): Map<YearMonth, Map<Stønadstype, Int>> =
        perioder
            .flatMap { it.posteringer }
            .filter { it.type == PosteringType.YTELSE }
            .mapNotNull { postering ->
                val stønadstype = finnStønadstype(postering.klassekode) ?: return@mapNotNull null
                if (stønadstype == egenStønadstype) {
                    null
                } else {
                    Triple(YearMonth.from(postering.fom), stønadstype, postering.beløp)
                }
            }.groupBy({ it.first }, { it.second to it.third })
            .mapValues { (_, verdier) ->
                verdier
                    .groupBy({ it.first }, { it.second })
                    .mapValues { (_, beløp) -> beløp.sum() }
                    .filterValues { it != 0 }
            }.filterValues { it.isNotEmpty() }

    /**
     * Grupperer posteringer per måned med en klassekode vi ikke klarer å mappe til en
     * [TypeAndel]/[Stønadstype]. Dette gjelder f.eks. utbetalinger knyttet til tiltakspenger, som
     * kan motposteres mot vårt gamle fagområde og dermed dukke opp i simuleringen for fagsaker som
     * utbetaler med gammelt fagområde. Vi har ikke noe forhold til klassekodene til disse
     * utbetalingene, så de grupperes på [UtbetalingFagområde] i stedet for [Stønadstype].
     *
     * Kun posteringer av type [PosteringType.YTELSE] vurderes, og stønadstyper hvor beløpet
     * summeres til 0 (f.eks. ved opphør) filtreres bort.
     */
    fun beløpFraUkjentKildePerMåned(perioder: List<Periode>): Map<YearMonth, Map<UtbetalingFagområde, Int>> =
        perioder
            .flatMap { it.posteringer }
            .filter { it.type == PosteringType.YTELSE }
            .filter { finnStønadstype(it.klassekode) == null }
            .map { postering -> Triple(YearMonth.from(postering.fom), postering.fagområde, postering.beløp) }
            .groupBy({ it.first }, { it.second to it.third })
            .mapValues { (_, verdier) ->
                verdier
                    .groupBy({ it.first }, { it.second })
                    .mapValues { (_, beløp) -> beløp.sum() }
                    .filterValues { it != 0 }
            }.filterValues { it.isNotEmpty() }

    private fun finnStønadstype(klassekode: String): Stønadstype? {
        val typeAndel = klassekodeTilTypeAndel[klassekode]
        if (typeAndel == null) {
            logger.debug("Fant ingen TypeAndel for klassekode=$klassekode")
            return null
        }
        return typeAndel.tilStønadstype()
    }

    /**
     * [TypeAndel] mappes til en [Stønadstype]. Stønadstypen er delt inn i TSO og TSR for enkelte
     * stønader:
     * - TSO gjelder [TypeAndel] med suffiks _AAP, _ENSLIG_FORSØRGER og _ETTERLATTE
     * - TSR gjelder [TypeAndel] som inneholder _TILTAK_
     */
    private fun TypeAndel.tilStønadstype(): Stønadstype? =
        when (this) {
            TypeAndel.TILSYN_BARN_ENSLIG_FORSØRGER,
            TypeAndel.TILSYN_BARN_AAP,
            TypeAndel.TILSYN_BARN_ETTERLATTE,
            -> Stønadstype.BARNETILSYN

            TypeAndel.LÆREMIDLER_ENSLIG_FORSØRGER,
            TypeAndel.LÆREMIDLER_AAP,
            TypeAndel.LÆREMIDLER_ETTERLATTE,
            -> Stønadstype.LÆREMIDLER

            TypeAndel.BOUTGIFTER_AAP,
            TypeAndel.BOUTGIFTER_ENSLIG_FORSØRGER,
            TypeAndel.BOUTGIFTER_ETTERLATTE,
            -> Stønadstype.BOUTGIFTER

            TypeAndel.DAGLIG_REISE_AAP,
            TypeAndel.DAGLIG_REISE_ENSLIG_FORSØRGER,
            TypeAndel.DAGLIG_REISE_ETTERLATTE,
            -> Stønadstype.DAGLIG_REISE_TSO

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
            -> Stønadstype.DAGLIG_REISE_TSR

            TypeAndel.REISE_TIL_SAMLING_AAP,
            TypeAndel.REISE_TIL_SAMLING_ENSLIG_FORSØRGER,
            TypeAndel.REISE_TIL_SAMLING_ETTERLATTE,
            -> Stønadstype.REISE_TIL_SAMLING_TSO

            TypeAndel.REISE_TIL_SAMLING_TILTAK_ARBEIDSFORBEREDENDE,
            TypeAndel.REISE_TIL_SAMLING_TILTAK_ARBEIDSTRENING,
            TypeAndel.REISE_TIL_SAMLING_TILTAK_AVKLARING,
            TypeAndel.REISE_TIL_SAMLING_TILTAK_ENKELTPLASS_AMO,
            TypeAndel.REISE_TIL_SAMLING_TILTAK_ENKELTPLASS_FAG_YRKE_HOYERE_UTD,
            TypeAndel.REISE_TIL_SAMLING_TILTAK_GRUPPE_AMO,
            TypeAndel.REISE_TIL_SAMLING_TILTAK_GRUPPE_FAG_YRKE_HOYERE_UTD,
            TypeAndel.REISE_TIL_SAMLING_TILTAK_HØYERE_UTDANNING,
            TypeAndel.REISE_TIL_SAMLING_TILTAK_JOBBKLUBB,
            TypeAndel.REISE_TIL_SAMLING_TILTAK_OPPFØLGING,
            TypeAndel.REISE_TIL_SAMLING_TILTAK_UTVIDET_OPPFØLGING_I_OPPLÆRING,
            -> Stønadstype.REISE_TIL_SAMLING_TSR

            TypeAndel.REISE_OPPSTART_AAP,
            TypeAndel.REISE_OPPSTART_ENSLIG_FORSØRGER,
            TypeAndel.REISE_OPPSTART_ETTERLATTE,
            -> Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO

            TypeAndel.REISE_OPPSTART_TILTAK_ARBEIDSFORBEREDENDE,
            TypeAndel.REISE_OPPSTART_TILTAK_ARBEIDSTRENING,
            TypeAndel.REISE_OPPSTART_TILTAK_AVKLARING,
            TypeAndel.REISE_OPPSTART_TILTAK_ENKELTPLASS_AMO,
            TypeAndel.REISE_OPPSTART_TILTAK_ENKELTPLASS_FAG_YRKE_HOYERE_UTD,
            TypeAndel.REISE_OPPSTART_TILTAK_GRUPPE_AMO,
            TypeAndel.REISE_OPPSTART_TILTAK_GRUPPE_FAG_YRKE_HOYERE_UTD,
            TypeAndel.REISE_OPPSTART_TILTAK_HØYERE_UTDANNING,
            TypeAndel.REISE_OPPSTART_TILTAK_JOBBKLUBB,
            TypeAndel.REISE_OPPSTART_TILTAK_OPPFØLGING,
            -> Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR

            TypeAndel.UGYLDIG -> null
        }

    /**
     * Klassekoder hentet fra `StønadTypeTilleggsstønader` i
     * https://github.com/navikt/helved-utbetaling/blob/main/models/main/models/Utbetalinger.kt
     * Kun klassekoder som er relevante for våre [TypeAndel]-verdier er tatt med. Deprecated
     * skrivefeil-varianter i den eksterne kilden er utelatt siden de har samme klassekode som den
     * gjeldende varianten.
     *
     * `internal` for å kunne verifisere i test at alle [TypeAndel] (unntatt [TypeAndel.UGYLDIG])
     * har en tilhørende klassekode her.
     */
    internal val klassekodeTilTypeAndel: Map<String, TypeAndel> =
        mapOf(
            "TSTBASISP2-OP" to TypeAndel.TILSYN_BARN_ENSLIG_FORSØRGER,
            "TSTBASISP4-OP" to TypeAndel.TILSYN_BARN_AAP,
            "TSTBASISP5-OP" to TypeAndel.TILSYN_BARN_ETTERLATTE,
            "TSLMASISP2-OP" to TypeAndel.LÆREMIDLER_ENSLIG_FORSØRGER,
            "TSLMASISP3-OP" to TypeAndel.LÆREMIDLER_AAP,
            "TSLMASISP4-OP" to TypeAndel.LÆREMIDLER_ETTERLATTE,
            "TSBUASIA-OP" to TypeAndel.BOUTGIFTER_AAP,
            "TSBUAISP2-OP" to TypeAndel.BOUTGIFTER_ENSLIG_FORSØRGER,
            "TSBUAISP3-O" to TypeAndel.BOUTGIFTER_ETTERLATTE,
            "TSDRASISP4-OP" to TypeAndel.DAGLIG_REISE_ENSLIG_FORSØRGER,
            "TSDRASISP1-OP" to TypeAndel.DAGLIG_REISE_AAP,
            "TSDRASISP3-OP" to TypeAndel.DAGLIG_REISE_ETTERLATTE,
            "TSROSASISP2-OP" to TypeAndel.REISE_TIL_SAMLING_ENSLIG_FORSØRGER,
            "TSROSASISP3-OP" to TypeAndel.REISE_TIL_SAMLING_AAP,
            "TSROSASISP4-OP" to TypeAndel.REISE_TIL_SAMLING_ETTERLATTE,
            "TSROAAISP2-OP" to TypeAndel.REISE_OPPSTART_ENSLIG_FORSØRGER,
            "TSROAAISP3-OP" to TypeAndel.REISE_OPPSTART_AAP,
            "TSROAAISP4-OP" to TypeAndel.REISE_OPPSTART_ETTERLATTE,
            "TSROAAFT-OP" to TypeAndel.REISE_OPPSTART_TILTAK_ARBEIDSFORBEREDENDE,
            "TSROAATTILT-OP" to TypeAndel.REISE_OPPSTART_TILTAK_ARBEIDSTRENING,
            "TSROAAAG-OP" to TypeAndel.REISE_OPPSTART_TILTAK_AVKLARING,
            "TSROAEPAMO-OP" to TypeAndel.REISE_OPPSTART_TILTAK_ENKELTPLASS_AMO,
            "TSROAEPVGSHOY-OP" to TypeAndel.REISE_OPPSTART_TILTAK_ENKELTPLASS_FAG_YRKE_HOYERE_UTD,
            "TSROAGRAMO-OP" to TypeAndel.REISE_OPPSTART_TILTAK_GRUPPE_AMO,
            "TSROAGRVGSHOY-OP" to TypeAndel.REISE_OPPSTART_TILTAK_GRUPPE_FAG_YRKE_HOYERE_UTD,
            "TSROAHOYUTD-OP" to TypeAndel.REISE_OPPSTART_TILTAK_HØYERE_UTDANNING,
            "TSROAJK2009-OP" to TypeAndel.REISE_OPPSTART_TILTAK_JOBBKLUBB,
            "TSROAOPPFAG-OP" to TypeAndel.REISE_OPPSTART_TILTAK_OPPFØLGING,
            "TSDRAFT-OP" to TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSFORBEREDENDE,
            "TSDRARREHABAGDAG-OP" to TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSRETTET_REHAB,
            "TSDRATTT2-OP" to TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSTRENING,
            "TSDRAAG-OP" to TypeAndel.DAGLIG_REISE_TILTAK_AVKLARING,
            "TSDRDIGJK-OP" to TypeAndel.DAGLIG_REISE_TILTAK_DIGITAL_JOBBKLUBB,
            "TSDREPAMO-OP" to TypeAndel.DAGLIG_REISE_TILTAK_ENKELTPLASS_AMO,
            "TSDREPVGSHOY-OP" to TypeAndel.DAGLIG_REISE_TILTAK_ENKELTPLASS_FAG_YRKE_HOYERE_UTD,
            "TSDRFOLV" to TypeAndel.DAGLIG_REISE_TILTAK_FORSØK_OPPLÆRINGSTILTAK_LENGER_VARIGHET,
            "TSDRGRAMO-OP" to TypeAndel.DAGLIG_REISE_TILTAK_GRUPPE_AMO,
            "TSDRGRVGSHOY-OP" to TypeAndel.DAGLIG_REISE_TILTAK_GRUPPE_FAG_YRKE_HOYERE_UTD,
            "TSDRHOYUTD-OP" to TypeAndel.DAGLIG_REISE_TILTAK_HØYERE_UTDANNING,
            "TSDRIPS-OP" to TypeAndel.DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE,
            "TSDRIPSUNG-OP" to TypeAndel.DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE_UNG,
            "TSDRJB2009-OP" to TypeAndel.DAGLIG_REISE_TILTAK_JOBBKLUBB,
            "TSDROPPFAG2-OP" to TypeAndel.DAGLIG_REISE_TILTAK_OPPFØLGING,
            "TSDRUTVAVKLOPPF-OP" to TypeAndel.DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_NAV,
            "TSDRUTVOPPFOPPL-OP" to TypeAndel.DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_OPPLÆRING,
            "TSROSAFT-OP" to TypeAndel.REISE_TIL_SAMLING_TILTAK_ARBEIDSFORBEREDENDE,
            "TSROSATTILT-OP" to TypeAndel.REISE_TIL_SAMLING_TILTAK_ARBEIDSTRENING,
            "TSROSAAGR-OP" to TypeAndel.REISE_TIL_SAMLING_TILTAK_AVKLARING,
            "TSROSEPAMO-OP" to TypeAndel.REISE_TIL_SAMLING_TILTAK_ENKELTPLASS_AMO,
            "TSROSEPVGSHOY-OP" to TypeAndel.REISE_TIL_SAMLING_TILTAK_ENKELTPLASS_FAG_YRKE_HOYERE_UTD,
            "TSROSGRAMO-OP" to TypeAndel.REISE_TIL_SAMLING_TILTAK_GRUPPE_AMO,
            "TSROSVGSHOY-OP" to TypeAndel.REISE_TIL_SAMLING_TILTAK_GRUPPE_FAG_YRKE_HOYERE_UTD,
            "TSROSHOYUTD-OP" to TypeAndel.REISE_TIL_SAMLING_TILTAK_HØYERE_UTDANNING,
            "TSROSJK2009-OP" to TypeAndel.REISE_TIL_SAMLING_TILTAK_JOBBKLUBB,
            "TSROSOPPFAG-OP" to TypeAndel.REISE_TIL_SAMLING_TILTAK_OPPFØLGING,
            "TSROSUAOPPF-OP" to TypeAndel.REISE_TIL_SAMLING_TILTAK_UTVIDET_OPPFØLGING_I_OPPLÆRING,
        )
}
