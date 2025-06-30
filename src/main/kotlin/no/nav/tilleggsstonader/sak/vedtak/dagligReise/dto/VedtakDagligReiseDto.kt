package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakRequest
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtakResponse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate
import java.util.UUID

sealed class VedtakDagligReiseDto(
    open val type: TypeVedtak,
)

sealed interface VedtakDagligReiseRequest : VedtakRequest

sealed interface VedtakDagligReiseResponse : VedtakResponse

data class VedtaksperiodeDagligReiseDto(
    val id: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
//    val status: VedtaksperiodeStatus? = null,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType,
)

fun List<Vedtaksperiode>.tilDto() =
    this.map {
        VedtaksperiodeDagligReiseDto(
            id = it.id,
            fom = it.fom,
            tom = it.tom,
            målgruppe = it.målgruppe,
            aktivitet = it.aktivitet,
        )
    }

fun List<VedtaksperiodeDagligReiseDto>.tilDomene() =
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
