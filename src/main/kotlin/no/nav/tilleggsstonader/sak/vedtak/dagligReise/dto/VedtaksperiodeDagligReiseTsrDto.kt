package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.felles.domain.VedtaksperiodeId
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

data class VedtaksperiodeDagligReiseTsrDto(
    val id: VedtaksperiodeId = VedtaksperiodeId.random(),
    val fom: LocalDate,
    val tom: LocalDate,
) {
    fun tilDomene() =
        Vedtaksperiode(
            id = id,
            fom = fom,
            tom = tom,
            målgruppe = FaktiskMålgruppe.ARBEIDSSØKER,
            aktivitet = AktivitetType.TILTAK,
        )
}

fun List<VedtaksperiodeDagligReiseTsrDto>.tilDomene() = map { it.tilDomene() }.sorted()
