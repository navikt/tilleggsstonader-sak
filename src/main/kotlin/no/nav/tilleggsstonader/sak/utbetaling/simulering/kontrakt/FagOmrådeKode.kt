package no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt

enum class FagOmrådeKode(val kode: String) {
    BARNETILSYN("BA"), // TODO fix
    ;

    companion object {

        private val kodeMap = FagOmrådeKode.values().associateBy { it.kode }

        fun fraKode(kode: String): FagOmrådeKode {
            return kodeMap[kode] ?: throw IllegalArgumentException("FagOmrådeKode finnes ikke for kode $kode")
        }
    }
}
