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
                TypeAndel.UGYLDIG -> throw Error("Trenger ikke statistikk på ugyldige betalinger")
            }
    }
}
