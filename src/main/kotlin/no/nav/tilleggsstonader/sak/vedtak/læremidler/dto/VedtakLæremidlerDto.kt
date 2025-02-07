package no.nav.tilleggsstonader.sak.vedtak.læremidler.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import java.time.LocalDate
import java.util.UUID

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true,
)
@JsonSubTypes(
    JsonSubTypes.Type(AvslagLæremidlerDto::class, name = "AVSLAG"),
    JsonSubTypes.Type(InnvilgelseLæremidlerRequest::class, name = "INNVILGELSE"),
    JsonSubTypes.Type(OpphørLæremidlerRequest::class, name = "OPPHØR"),
    failOnRepeatedNames = true,
)
sealed class VedtakLæremidlerDto(
    open val type: TypeVedtak,
)

sealed interface VedtakLæremidlerRequest : VedtakRequest

sealed interface VedtakLæremidlerResponse : VedtakResponse

data class VedtaksperiodeDto(
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val status: VedtaksperiodeStatus,
)

enum class VedtaksperiodeStatus {
    NY,
    ENDRET,
    UENDRET,
    SLETTET,
}

fun List<Vedtaksperiode>.tilDto() = this.map { VedtaksperiodeDto(id = it.id, fom = it.fom, tom = it.tom, status = it.status) }

fun List<VedtaksperiodeDto>.tilDomene() = this.map { Vedtaksperiode(id = it.id, fom = it.fom, tom = it.tom, status = it.status) }
