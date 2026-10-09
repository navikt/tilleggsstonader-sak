package no.nav.tilleggsstonader.sak.vedtak.flytting.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
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
    val grunnlag: BeregningsgrunnlagFlytting,
)

enum class Flyttemåte {
    FLYTTEBYRÅ,
    FLYTTE_SELV,
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(BeregningsgrunnlagFlyttebyrå::class, name = "FLYTTEBYRÅ"),
    JsonSubTypes.Type(BeregningsgrunnlagEgenKjøring::class, name = "FLYTTE_SELV"),
)
sealed interface BeregningsgrunnlagFlytting

data class BeregningsgrunnlagFlyttebyrå(
    val tilbud1Pris: BigDecimal,
    val tilbud2Pris: BigDecimal,
    val erBetalingDokumentert: Boolean,
) : BeregningsgrunnlagFlytting

data class BeregningsgrunnlagEgenKjøring(
    val avstandEnVei: Int,
    val sats: BigDecimal,
    val satsBekreftet: Boolean,
    val henger: BigDecimal,
    val bompenger: BigDecimal,
    val ferge: BigDecimal,
    val parkering: BigDecimal,
) : BeregningsgrunnlagFlytting
