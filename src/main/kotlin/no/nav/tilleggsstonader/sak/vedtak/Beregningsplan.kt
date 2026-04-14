package no.nav.tilleggsstonader.sak.vedtak

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDate

data class Beregningsplan(
    val omfang: Beregningsomfang,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val fraDato: LocalDate? = null,
) {
    init {
        require(omfang != Beregningsomfang.FRA_DATO || fraDato != null) {
            "fraDato må settes ved omfang ${Beregningsomfang.FRA_DATO}"
        }
        require(omfang == Beregningsomfang.FRA_DATO || fraDato == null) {
            "fraDato kan kun settes ved omfang ${Beregningsomfang.FRA_DATO}"
        }
    }

    fun beregnFra(): LocalDate? =
        when (omfang) {
            Beregningsomfang.ALLE_PERIODER -> null
            Beregningsomfang.FRA_DATO -> fraDato
            Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT -> error("beregnFra-dato er ikke relevant for $omfang")
        }

    fun legacyTidligsteEndring(): LocalDate? =
        when (omfang) {
            Beregningsomfang.ALLE_PERIODER -> null
            Beregningsomfang.FRA_DATO -> fraDato
            Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT -> null
        }
}

enum class Beregningsomfang {
    ALLE_PERIODER,
    FRA_DATO,
    GJENBRUK_FORRIGE_RESULTAT,
}
