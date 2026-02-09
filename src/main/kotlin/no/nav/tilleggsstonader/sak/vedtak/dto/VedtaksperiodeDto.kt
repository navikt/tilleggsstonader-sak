package no.nav.tilleggsstonader.sak.vedtak.dto

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate
import java.util.UUID

data class LagretVedtaksperiodeDto(
    val id: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppeType: FaktiskMålgruppe,
    val aktivitetType: AktivitetType,
    val typeAktivitet: TypeAktivitet? = null,
    val vedtaksperiodeFraForrigeVedtak: VedtaksperiodeDto?,
) : Periode<LocalDate>,
    KopierPeriode<LagretVedtaksperiodeDto> {
    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): LagretVedtaksperiodeDto = this.copy(fom = fom, tom = tom)

    fun tilVedtaksperiodeDto() =
        VedtaksperiodeDto(
            id = id,
            fom = fom,
            tom = tom,
            målgruppeType = målgruppeType,
            aktivitetType = aktivitetType,
            typeAktivitet = typeAktivitet,
        )
}

data class VedtaksperiodeDto(
    val id: UUID = UUID.randomUUID(),
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppeType: FaktiskMålgruppe,
    val aktivitetType: AktivitetType,
    val typeAktivitet: TypeAktivitet? = null,
) : Periode<LocalDate> {
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

fun List<Vedtaksperiode>.tilLagretVedtaksperiodeDto(tidligereVedtaksperioder: List<Vedtaksperiode>?) =
    map {
        it.tilLagretVedtaksperiodeDto(tidligereVedtaksperioder?.find { v -> v.id == it.id })
    }.sorted()

fun Vedtaksperiode.tilLagretVedtaksperiodeDto(forrigeVedtaksperiode: Vedtaksperiode?) =
    LagretVedtaksperiodeDto(
        id = id,
        fom = fom,
        tom = tom,
        målgruppeType = målgruppe,
        aktivitetType = aktivitet,
        typeAktivitet = typeAktivitet,
        vedtaksperiodeFraForrigeVedtak = forrigeVedtaksperiode?.tilDto(),
    )

fun List<Vedtaksperiode>.tilDto() = map { it.tilDto() }.sorted()

fun Vedtaksperiode.tilDto() =
    VedtaksperiodeDto(
        id = id,
        fom = fom,
        tom = tom,
        målgruppeType = målgruppe,
        aktivitetType = aktivitet,
        typeAktivitet = typeAktivitet,
    )

fun List<VedtaksperiodeDto>.tilDomene() = map { it.tilDomene() }.sorted()

fun List<VedtaksperiodeDto>.tilVedtaksperiodeBeregning() = map { VedtaksperiodeBeregning(it) }
