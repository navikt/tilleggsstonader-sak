package no.nav.tilleggsstonader.sak.vedtak.flytting.dto

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.felles.domain.VedtaksperiodeId
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import java.time.LocalDate

/**
 * Vedtaksperiode for flytting. Flytting har ikke aktivitet, kun målgruppe.
 */
data class VedtaksperiodeFlyttingDto(
    val id: VedtaksperiodeId = VedtaksperiodeId.random(),
    val fom: LocalDate,
    val tom: LocalDate,
    val målgruppeType: FaktiskMålgruppe,
) {
    fun tilDomene() =
        Vedtaksperiode(
            id = id,
            fom = fom,
            tom = tom,
            målgruppe = målgruppeType,
            aktivitet = null,
        )
}

fun List<VedtaksperiodeFlyttingDto>.tilDomene() = map { it.tilDomene() }.sorted()
