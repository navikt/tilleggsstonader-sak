package no.nav.tilleggsstonader.sak.vedtak.flytting.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import java.math.BigDecimal
import java.time.LocalDate

data class BeregningsresultatFlytting(
    val resultater: List<BeregningsresultatFlyttevilkår>,
)

data class BeregningsresultatFlyttevilkår(
    val fraTidligereVedtak: Boolean = false,
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: BigDecimal,
    val målgruppe: FaktiskMålgruppe,
    val grunnlag: BeregningsgrunnlagFlytting,
)

fun BeregningsresultatFlyttevilkår.skalUtbetales(): Boolean =
    when (val beregningsgrunnlag = grunnlag) {
        is BeregningsgrunnlagFlyttebyrå -> beregningsgrunnlag.erUtgifterDokumentert
        is BeregningsgrunnlagFlytteSelv -> beregningsgrunnlag.erUtgifterDokumentert
    }

enum class Flyttemåte {
    FLYTTEBYRÅ,
    FLYTTE_SELV,
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(BeregningsgrunnlagFlyttebyrå::class, name = "FLYTTEBYRÅ"),
    JsonSubTypes.Type(BeregningsgrunnlagFlytteSelv::class, name = "FLYTTE_SELV"),
)
sealed interface BeregningsgrunnlagFlytting

data class BeregningsgrunnlagFlyttebyrå(
    val tilbud: List<BeregningsgrunnlagFlyttebyråTilbud>,
    val erUtgifterDokumentert: Boolean,
) : BeregningsgrunnlagFlytting {
    init {
        require(tilbud.filter { it.erValgt }.size == 1) { "Det må være nøyaktig ett valgt tilbud" }
    }
}

data class BeregningsgrunnlagFlyttebyråTilbud(
    val navn: String,
    val pris: BigDecimal,
    val erValgt: Boolean,
)

data class BeregningsgrunnlagFlytteSelv(
    val avstandEnVei: Int,
    val sats: BigDecimal,
    val satsBekreftet: Boolean,
    val henger: BigDecimal,
    val leiebil: BigDecimal,
    val bompenger: BigDecimal,
    val ferge: BigDecimal,
    val parkering: BigDecimal,
    val annet: BigDecimal,
    val erUtgifterDokumentert: Boolean,
) : BeregningsgrunnlagFlytting
