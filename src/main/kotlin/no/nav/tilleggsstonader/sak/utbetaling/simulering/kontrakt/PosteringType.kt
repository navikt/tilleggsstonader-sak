package no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt

enum class PosteringType(
    val kode: String,
) {
    YTELSE("YTEL"),
    FEILUTBETALING("FEIL"),
    FORSKUDSSKATT("SKAT"),
    JUSTERING("JUST"),
    TREKK("TREK"),
    MOTPOSTERING("MOTP"),
    ;

    companion object {
        private val kodeMap = entries.associateBy { it.kode }

        fun fraKode(kode: String): PosteringType =
            kodeMap[kode] ?: throw IllegalArgumentException("PosteringType finnes ikke for kode $kode")
    }
}
