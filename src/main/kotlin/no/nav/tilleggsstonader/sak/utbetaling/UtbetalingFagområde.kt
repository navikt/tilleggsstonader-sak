package no.nav.tilleggsstonader.sak.utbetaling

/**
 * Alle fagområder knyttet til tilleggsstønader.
 * Gamle fagområder er ikke stønadsspesifikke.
 * Tilsyn barn, læremidler og boutgifter har brukt disse, men nye saker skal senere over på stønadsspesifikke fagområder.
 */
enum class UtbetalingFagområde(
    val kode: String,
) {
    TILLEGGSSTØNADER("TILLST"),
    TILLEGGSSTØNADER_ARENA("TSTARENA"),
    TILLEGGSSTØNADER_ARENA_MANUELL_POSTERING("MTSTAREN"),

    TILLEGGSTØNADER_PASS_BARN("TILLSTPB"),
    TILLEGGSTØNADER_LÆREMIDLER("TILLSTLM"),
    TILLEGGSTØNADER_BOUTGIFTER("TILLSTBO"),
    TILLEGGSTØNADER_DAGLIG_REISE("TILLSTDR"),
    TILLEGGSTØNADER_REISE_TIL_SAMLING("TILLSTRS"),
    TILLEGGSTØNADER_REISE_OPPSTART("TILLSTRO"),
    TILLEGGSTØNADER_REISE_ARBEID("TILLSTRA"),
    TILLEGGSTØNADER_FLYTTING("TILLSTFL"),
    ;

    fun erNyeFagområder(): Boolean =
        this != TILLEGGSSTØNADER && this != TILLEGGSSTØNADER_ARENA && this != TILLEGGSSTØNADER_ARENA_MANUELL_POSTERING

    companion object {
        // Håndterer hvordan de forskjellige fagområdene returneres fra helved. Se https://github.com/navikt/helved-utbetaling/blob/main/models/main/models/Fagsystem.kt#L6
        fun fraKode(kode: String): UtbetalingFagområde =
            entries.firstOrNull { it.kode == kode }
                ?: UtbetalingFagområde.valueOf(kode)
    }
}
