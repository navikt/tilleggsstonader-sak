package no.nav.tilleggsstonader.sak.vedtak.læremidler.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeStatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
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

data class VedtaksperiodeLæremidlerDto(
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val målgruppeType: FaktiskMålgruppe,
    val aktivitetType: AktivitetType,
    val status: VedtaksperiodeStatus? = null,
)

fun List<Vedtaksperiode>.tilDto() =
    this.map {
        VedtaksperiodeLæremidlerDto(
            id = it.id,
            fom = it.fom,
            tom = it.tom,
            målgruppeType = it.målgruppe,
            aktivitetType = it.aktivitet,
            status = it.status,
        )
    }

fun List<VedtaksperiodeLæremidlerDto>.tilDomene() =
    this
        .map {
            Vedtaksperiode(
                id = it.id,
                fom = it.fom,
                tom = it.tom,
                målgruppe = it.målgruppeType,
                aktivitet = it.aktivitetType,
            )
        }.sorted()
