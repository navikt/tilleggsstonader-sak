package no.nav.tilleggsstonader.sak.vedtak.dto

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate
import java.util.UUID

data class VedtaksperiodeDto(
    val id: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppeType: FaktiskMålgruppe,
    val aktivitetType: AktivitetType,
) : Periode<LocalDate>,
    KopierPeriode<VedtaksperiodeDto> {
    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): VedtaksperiodeDto = this.copy(fom = fom, tom = tom)

    fun tilDomene() =
        Vedtaksperiode(
            id = id,
            fom = fom,
            tom = tom,
            målgruppe = målgruppeType,
            aktivitet = aktivitetType,
        )
}

fun List<VedtaksperiodeDto>.tilDomene() = map { it.tilDomene() }

fun List<VedtaksperiodeDto>.tilVedtaksperiodeBeregning() = map { VedtaksperiodeBeregning(it) }
