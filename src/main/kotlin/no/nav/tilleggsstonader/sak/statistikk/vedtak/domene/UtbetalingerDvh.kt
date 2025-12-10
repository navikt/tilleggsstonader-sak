package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.MakssatsDvhUtil.Companion.finnMakssats
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
import java.time.LocalDate

data class UtbetalingerDvh(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate,
    val type: AndelstypeDvh,
    val beløp: Int,
    val makssats: Int? = null,
    val beløpErBegrensetAvMakssats: Boolean? = null,
) {
    data class JsonWrapper(
        val utbetalinger: List<UtbetalingerDvh>,
    )

    companion object {
        fun fraDomene(
            ytelser: List<AndelTilkjentYtelse>,
            vedtak: Vedtak,
        ): JsonWrapper {
            val gyldigeAndeler = ytelser.filterNot { it.type == TypeAndel.UGYLDIG }
            return JsonWrapper(
                utbetalinger =
                    gyldigeAndeler.map {
                        val (makssats, beløpErBegrensetAvMakssats) =
                            finnMakssats(andelTilkjentYtelse = it, vedtaksdata = vedtak.data)

                        UtbetalingerDvh(
                            fraOgMed = it.fom,
                            tilOgMed = it.tom,
                            type = AndelstypeDvh.fraDomene(it.type),
                            beløp = it.beløp,
                            makssats = makssats,
                            beløpErBegrensetAvMakssats = beløpErBegrensetAvMakssats,
                        )
                    },
            )
        }
    }
}

enum class AndelstypeDvh {
    TILSYN_BARN_ENSLIG_FORSØRGER,
    TILSYN_BARN_AAP,
    TILSYN_BARN_ETTERLATTE,

    LÆREMIDLER_ENSLIG_FORSØRGER,
    LÆREMIDLER_AAP,
    LÆREMIDLER_ETTERLATTE,

    BOUTGIFTER_AAP,
    BOUTGIFTER_ETTERLATTE,
    BOUTGIFTER_ENSLIG_FORSØRGER,

    DAGLIG_REISE_AAP,
    DAGLIG_REISE_ENSLIG_FORSØRGER,
    DAGLIG_REISE_ETTERLATTE,

    DAGLIG_REISE_TILTAK_ARBEIDSFORBEREDENDE,
    DAGLIG_REISE_TILTAK_ARBEIDSRETTET_REHAB,
    DAGLIG_REISE_TILTAK_ARBEIDSTRENING,
    DAGLIG_REISE_TILTAK_AVKLARING,
    DAGLIG_REISE_TILTAK_DIGITAL_JOBBKLUBB,
    DAGLIG_REISE_TILTAK_ENKELTPLASS_AMO,
    DAGLIG_REISE_TILTAK_ENKELTPLASS_FAG_YRKE_HOYERE_UTD,
    DAGLIG_REISE_TILTAK_FORSØK_OPPLÆRINGSTILTAK_LENGER_VARIGHET,
    DAGLIG_REISE_TILTAK_GRUPPE_AMO,
    DAGLIG_REISE_TILTAK_GRUPPE_FAG_YRKE_HOYERE_UTD,
    DAGLIG_REISE_TILTAK_HØYERE_UTDANNING,
    DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE,
    DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE_UNG,
    DAGLIG_REISE_TILTAK_JOBBKLUBB,
    DAGLIG_REISE_TILTAK_OPPFØLGING,
    DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_NAV,
    DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_OPPLÆRING,
    ;

    companion object {
        fun fraDomene(typeAndel: TypeAndel) =
            when (typeAndel) {
                TypeAndel.TILSYN_BARN_ENSLIG_FORSØRGER -> TILSYN_BARN_ENSLIG_FORSØRGER
                TypeAndel.TILSYN_BARN_AAP -> TILSYN_BARN_AAP
                TypeAndel.TILSYN_BARN_ETTERLATTE -> TILSYN_BARN_ETTERLATTE
                TypeAndel.LÆREMIDLER_ENSLIG_FORSØRGER -> LÆREMIDLER_ENSLIG_FORSØRGER
                TypeAndel.LÆREMIDLER_AAP -> LÆREMIDLER_AAP
                TypeAndel.LÆREMIDLER_ETTERLATTE -> LÆREMIDLER_ETTERLATTE
                TypeAndel.BOUTGIFTER_AAP -> BOUTGIFTER_AAP
                TypeAndel.BOUTGIFTER_ENSLIG_FORSØRGER -> BOUTGIFTER_ENSLIG_FORSØRGER
                TypeAndel.BOUTGIFTER_ETTERLATTE -> BOUTGIFTER_ETTERLATTE
                TypeAndel.DAGLIG_REISE_AAP -> DAGLIG_REISE_AAP
                TypeAndel.DAGLIG_REISE_ENSLIG_FORSØRGER -> DAGLIG_REISE_ENSLIG_FORSØRGER
                TypeAndel.DAGLIG_REISE_ETTERLATTE -> DAGLIG_REISE_ETTERLATTE
                TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSFORBEREDENDE -> DAGLIG_REISE_TILTAK_ARBEIDSFORBEREDENDE
                TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSRETTET_REHAB -> DAGLIG_REISE_TILTAK_ARBEIDSRETTET_REHAB
                TypeAndel.DAGLIG_REISE_TILTAK_ARBEIDSTRENING -> DAGLIG_REISE_TILTAK_ARBEIDSTRENING
                TypeAndel.DAGLIG_REISE_TILTAK_AVKLARING -> DAGLIG_REISE_TILTAK_AVKLARING
                TypeAndel.DAGLIG_REISE_TILTAK_DIGITAL_JOBBKLUBB -> DAGLIG_REISE_TILTAK_DIGITAL_JOBBKLUBB
                TypeAndel.DAGLIG_REISE_TILTAK_ENKELTPLASS_AMO -> DAGLIG_REISE_TILTAK_ENKELTPLASS_AMO
                TypeAndel.DAGLIG_REISE_TILTAK_ENKELTPLASS_FAG_YRKE_HOYERE_UTD -> DAGLIG_REISE_TILTAK_ENKELTPLASS_FAG_YRKE_HOYERE_UTD
                TypeAndel.DAGLIG_REISE_TILTAK_FORSØK_OPPLÆRINGSTILTAK_LENGER_VARIGHET ->
                    DAGLIG_REISE_TILTAK_FORSØK_OPPLÆRINGSTILTAK_LENGER_VARIGHET
                TypeAndel.DAGLIG_REISE_TILTAK_GRUPPE_AMO -> DAGLIG_REISE_TILTAK_GRUPPE_AMO
                TypeAndel.DAGLIG_REISE_TILTAK_GRUPPE_FAG_YRKE_HOYERE_UTD -> DAGLIG_REISE_TILTAK_GRUPPE_FAG_YRKE_HOYERE_UTD
                TypeAndel.DAGLIG_REISE_TILTAK_HØYERE_UTDANNING -> DAGLIG_REISE_TILTAK_HØYERE_UTDANNING
                TypeAndel.DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE -> DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE
                TypeAndel.DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE_UNG -> DAGLIG_REISE_TILTAK_INDIVIDUELL_JOBBSTØTTE_UNG
                TypeAndel.DAGLIG_REISE_TILTAK_JOBBKLUBB -> DAGLIG_REISE_TILTAK_JOBBKLUBB
                TypeAndel.DAGLIG_REISE_TILTAK_OPPFØLGING -> DAGLIG_REISE_TILTAK_OPPFØLGING
                TypeAndel.DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_NAV -> DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_NAV
                TypeAndel.DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_OPPLÆRING -> DAGLIG_REISE_TILTAK_UTVIDET_OPPFØLGING_I_OPPLÆRING
                TypeAndel.UGYLDIG -> throw Error("Trenger ikke statistikk på ugyldige betalinger")
            }
    }
}
