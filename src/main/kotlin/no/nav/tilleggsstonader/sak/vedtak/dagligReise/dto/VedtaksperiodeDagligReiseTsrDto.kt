package no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDtoInterface
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate
import java.util.UUID

data class VedtaksperiodeDagligReiseTsrDto(
    override val id: UUID = UUID.randomUUID(),
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val typeAktivitet: TypeAktivitet,
) : VedtaksperiodeDtoInterface {
    override val målgruppeType: FaktiskMålgruppe = FaktiskMålgruppe.ARBEIDSSØKER
    override val aktivitetType: AktivitetType = AktivitetType.TILTAK

    fun tilDomene() =
        Vedtaksperiode(
            id = id,
            fom = fom,
            tom = tom,
            målgruppe = målgruppeType,
            aktivitet = aktivitetType,
            typeAktivitet = typeAktivitet,
        )
}

fun List<VedtaksperiodeDagligReiseTsrDto>.tilDomene() = map { it.tilDomene() }.sorted()
