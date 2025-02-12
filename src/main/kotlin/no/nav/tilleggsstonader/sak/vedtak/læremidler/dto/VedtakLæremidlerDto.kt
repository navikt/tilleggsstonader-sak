package no.nav.tilleggsstonader.sak.vedtak.læremidler.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import java.time.LocalDate

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

data class VedtaksperiodeLæremidlerDto(
    val fom: LocalDate,
    val tom: LocalDate,
)

fun List<Vedtaksperiode>.tilDto() = this.map { VedtaksperiodeLæremidlerDto(fom = it.fom, tom = it.tom) }

fun List<VedtaksperiodeLæremidlerDto>.tilDomene() = this.map { Vedtaksperiode(fom = it.fom, tom = it.tom) }.sorted()
