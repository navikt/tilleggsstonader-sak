package no.nav.tilleggsstonader.sak.vedtak.dto

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate
import java.util.UUID

interface VedtaksperiodeDtoInterface : Periode<LocalDate> {
    val id: UUID
    override val fom: LocalDate
    override val tom: LocalDate
    val målgruppeType: FaktiskMålgruppe
    val aktivitetType: AktivitetType
}

data class LagretVedtaksperiodeDto(
    override val id: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val målgruppeType: FaktiskMålgruppe,
    override val aktivitetType: AktivitetType,
    val status: VedtaksperiodeStatus = VedtaksperiodeStatus.NY,
    val vedtaksperiodeFraForrigeVedtak: VedtaksperiodeDto?,
) : VedtaksperiodeDtoInterface,
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
        )
}

data class VedtaksperiodeDto(
    override val id: UUID = UUID.randomUUID(),
    override val fom: LocalDate,
    override val tom: LocalDate,
    override val målgruppeType: FaktiskMålgruppe,
    override val aktivitetType: AktivitetType,
) : VedtaksperiodeDtoInterface {
    fun tilDomene() =
        Vedtaksperiode(
            id = id,
            fom = fom,
            tom = tom,
            målgruppe = målgruppeType,
            aktivitet = aktivitetType,
        )
}

enum class VedtaksperiodeStatus {
    NY,
    ENDRET,
    UENDRET,
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
        status = utledStatus(forrigeVedtaksperiode),
        vedtaksperiodeFraForrigeVedtak = forrigeVedtaksperiode?.tilDto(),
    )

fun Vedtaksperiode.tilDto() =
    VedtaksperiodeDto(
        id = id,
        fom = fom,
        tom = tom,
        målgruppeType = målgruppe,
        aktivitetType = aktivitet,
    )

private fun Vedtaksperiode.utledStatus(forrigeVedtaksperiode: Vedtaksperiode?): VedtaksperiodeStatus =
    when {
        forrigeVedtaksperiode == null -> VedtaksperiodeStatus.NY
        this.fom == forrigeVedtaksperiode.fom && this.tom == forrigeVedtaksperiode.tom ->
            VedtaksperiodeStatus.UENDRET

        else -> VedtaksperiodeStatus.ENDRET
    }

fun List<VedtaksperiodeDto>.tilDomene() = map { it.tilDomene() }.sorted()

fun List<VedtaksperiodeDto>.tilVedtaksperioderLæremidler() =
    map {
        no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode(
            id = it.id,
            fom = it.fom,
            tom = it.tom,
            målgruppe = it.målgruppeType,
            aktivitet = it.aktivitetType,
        )
    }.sorted()

fun List<VedtaksperiodeDto>.tilVedtaksperiodeBeregning() = map { VedtaksperiodeBeregning(it) }
