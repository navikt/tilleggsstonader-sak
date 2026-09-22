package no.nav.tilleggsstonader.sak.vedtak.dto

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.felles.domain.VedtaksperiodeId
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

/**
 * Forenklet vedtaksperiode for TSR-varianter (daglig reise og reise til samling), der
 * typeandel bestemmes av tiltaksvariant i stedet for målgruppe/aktivitet. Saksbehandler
 * trenger derfor kun å oppgi fom/tom - målgruppe og aktivitet settes til faste verdier.
 */
data class VedtaksperiodeTsrDto(
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

fun List<VedtaksperiodeTsrDto>.tilDomene() = map { it.tilDomene() }.sorted()
