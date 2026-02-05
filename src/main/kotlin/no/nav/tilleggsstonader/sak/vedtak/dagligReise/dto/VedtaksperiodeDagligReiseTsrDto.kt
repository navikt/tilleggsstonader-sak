package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate
import java.util.UUID

data class VedtaksperiodeDagligReiseTsrDto(
    val id: UUID = UUID.randomUUID(),
    val fom: LocalDate,
    val tom: LocalDate,
    val typeAktivitet: TypeAktivitet,
) {
    fun tilDomene() =
        Vedtaksperiode(
            id = id,
            fom = fom,
            tom = tom,
            målgruppe = FaktiskMålgruppe.ARBEIDSSØKER,
            aktivitet = AktivitetType.TILTAK,
            typeAktivitet = typeAktivitet,
        )
}

fun List<VedtaksperiodeDagligReiseTsrDto>.tilDomene() = map { it.tilDomene() }.sorted()
