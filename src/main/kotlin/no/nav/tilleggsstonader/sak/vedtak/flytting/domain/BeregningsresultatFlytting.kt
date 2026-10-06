package no.nav.tilleggsstonader.sak.vedtak.flytting.domain

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import java.math.BigDecimal
import java.time.LocalDate

data class BeregningsresultatFlytting(
    val resultater: List<BeregningsresultatFlyttevilkår>,
)

data class BeregningsresultatFlyttevilkår(
    val vilkårId: VilkårId,
    val fom: LocalDate,
    val tom: LocalDate,
    val flyttemåte: Flyttemåte,
    val grunnlag: BeregningsgrunnlagFlytting,
    val beløp: BigDecimal,
)

enum class Flyttemåte {
    FLYTTEBYRÅ,
    EGEN_KJØRING,
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(BeregningsgrunnlagFlyttebyrå::class, name = "FLYTTEBYRÅ"),
    JsonSubTypes.Type(BeregningsgrunnlagEgenKjøring::class, name = "EGEN_KJØRING"),
)
sealed interface BeregningsgrunnlagFlytting

data class BeregningsgrunnlagFlyttebyrå(
    val tilbud1Pris: BigDecimal,
    val tilbud2Pris: BigDecimal,
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
