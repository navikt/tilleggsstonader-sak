package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeStatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate
import java.util.UUID

interface PeriodeMedId : Periode<LocalDate> {
    val id: UUID

    fun kopier(
        fom: LocalDate,
        tom: LocalDate,
    ): PeriodeMedId
}

data class Vedtaksperiode(
    override val id: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType,
) : Periode<LocalDate>,
    KopierPeriode<Vedtaksperiode>,
    PeriodeMedId {
    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): Vedtaksperiode = this.copy(fom = fom, tom = tom)

    override fun kopier(
        fom: LocalDate,
        tom: LocalDate,
    ): Vedtaksperiode = this.copy(fom = fom, tom = tom)

    fun tilDto(forrigeVedtaksperiode: Vedtaksperiode?) =
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
}

fun List<Vedtaksperiode>.tilVedtaksperiodeDto(tidligereVedtaksperioder: List<Vedtaksperiode>?) =
    map {
        it.tilDto(tidligereVedtaksperioder?.find { v -> v.id == it.id })
    }

fun List<Vedtaksperiode>.tilVedtaksperiodeBeregning() =
    map {
        VedtaksperiodeBeregning(
            fom = it.fom,
            tom = it.tom,
            målgruppe = it.målgruppe,
            aktivitet = it.aktivitet,
        )
    }

fun List<Vedtaksperiode>.mergeSammenhengende() =
    this
        .sorted()
        .mergeSammenhengende(
            { v1, v2 ->
                v1.overlapperEllerPåfølgesAv(v2) &&
                    v1.målgruppe == v2.målgruppe &&
                    v1.aktivitet == v2.aktivitet
            },
            { v1, v2 -> v1.medPeriode(fom = minOf(v1.fom, v2.fom), tom = maxOf(v1.tom, v2.tom)) },
        )
