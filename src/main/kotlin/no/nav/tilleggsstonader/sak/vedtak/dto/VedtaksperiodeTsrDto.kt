package no.nav.tilleggsstonader.sak.vedtak.dto

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.felles.domain.VedtaksperiodeId
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

/**
 * Forenklet vedtaksperiode for TSR-varianter der typeandel bestemmes av tiltaksvariant.
 * Aktivitet settes til en fast verdi, mens saksbehandler velger faktisk målgruppe (avhengig av at toggle for målgruppe UNGDOMSPROGRAMMET er på).
 */
data class VedtaksperiodeTsrDto(
    val id: VedtaksperiodeId = VedtaksperiodeId.random(),
    val fom: LocalDate,
    val tom: LocalDate,
    val målgruppeType: FaktiskMålgruppe = FaktiskMålgruppe.ARBEIDSSØKER,
) {
    fun tilDomene() =
        Vedtaksperiode(
            id = id,
            fom = fom,
            tom = tom,
            målgruppe = målgruppeType,
            aktivitet = AktivitetType.TILTAK,
        )
}

fun List<VedtaksperiodeTsrDto>.tilDomene() = map { it.tilDomene() }.sorted()
