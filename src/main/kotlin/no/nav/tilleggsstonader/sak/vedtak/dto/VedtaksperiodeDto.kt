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
    val status: VedtaksperiodeStatus = VedtaksperiodeStatus.NY,
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

enum class VedtaksperiodeStatus {
    NY,
    ENDRET,
    UENDRET,
}

fun List<Vedtaksperiode>.tilVedtaksperiodeDto(tidligereVedtaksperioder: List<Vedtaksperiode>?) =
    map {
        it.tilDto(tidligereVedtaksperioder?.find { v -> v.id == it.id })
    }

fun Vedtaksperiode.tilDto(forrigeVedtaksperiode: Vedtaksperiode?) =
    VedtaksperiodeDto(
        id = id,
        fom = fom,
        tom = tom,
        målgruppeType = målgruppe,
        aktivitetType = aktivitet,
        status = utledStatus(forrigeVedtaksperiode),
    )

private fun Vedtaksperiode.utledStatus(forrigeVedtaksperiode: Vedtaksperiode?): VedtaksperiodeStatus =
    when {
        forrigeVedtaksperiode == null -> VedtaksperiodeStatus.NY
        this.fom == forrigeVedtaksperiode.fom && this.tom == forrigeVedtaksperiode.tom ->
            VedtaksperiodeStatus.UENDRET

        else -> VedtaksperiodeStatus.ENDRET
    }

fun List<VedtaksperiodeDto>.tilDomene() = map { it.tilDomene() }

fun List<VedtaksperiodeDto>.tilVedtaksperiodeBeregning() = map { VedtaksperiodeBeregning(it) }
