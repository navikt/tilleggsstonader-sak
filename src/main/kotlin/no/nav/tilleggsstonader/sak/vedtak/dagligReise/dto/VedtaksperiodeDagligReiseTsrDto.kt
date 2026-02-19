package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dto.TypeAktivitetDto
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDomene
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate
import java.util.UUID

data class VedtaksperiodeDagligReiseTsrDto(
    val id: UUID = UUID.randomUUID(),
    val fom: LocalDate,
    val tom: LocalDate,
    val typeAktivitet: TypeAktivitetDto,
) {
    fun tilDomene() =
        Vedtaksperiode(
            id = id,
            fom = fom,
            tom = tom,
            målgruppe = FaktiskMålgruppe.ARBEIDSSØKER,
            aktivitet = AktivitetType.TILTAK,
            typeAktivitet = typeAktivitet.tilDomene(),
        )
}

fun List<VedtaksperiodeDagligReiseTsrDto>.tilDomene() = map { it.tilDomene() }.sorted()
