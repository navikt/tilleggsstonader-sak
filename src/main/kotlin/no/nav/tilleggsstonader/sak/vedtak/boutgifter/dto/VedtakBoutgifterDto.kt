package no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
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
//    JsonSubTypes.Type(AvslagBoutgifterDto::class, name = "AVSLAG"),
    JsonSubTypes.Type(InnvilgelseBoutgifterRequest::class, name = "INNVILGELSE"),
//    JsonSubTypes.Type(OpphørBoutgifterRequest::class, name = "OPPHØR"),
    failOnRepeatedNames = true,
)
sealed class VedtakBoutgifterDto(
    open val type: TypeVedtak,
)

sealed interface VedtakBoutgifterRequest : VedtakRequest

sealed interface VedtakBoutgifterResponse : VedtakResponse

data class VedtaksperiodeBoutgifterDto(
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
//    val status: VedtaksperiodeStatus? = null,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType,
)

fun List<Vedtaksperiode>.tilDto() =
    this.map {
        VedtaksperiodeBoutgifterDto(
            id = it.id,
            fom = it.fom,
            tom = it.tom,
            målgruppe = it.målgruppe,
            aktivitet = it.aktivitet,
        )
    }

fun List<VedtaksperiodeBoutgifterDto>.tilDomene() =
    this
        .map {
            Vedtaksperiode(
                id = it.id,
                fom = it.fom,
                tom = it.tom,
                målgruppe = it.målgruppe,
                aktivitet = it.aktivitet,
            )
        }.sorted()
