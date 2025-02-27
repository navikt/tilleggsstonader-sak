package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
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
    val målgruppe: MålgruppeType,
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

    fun tilDto() =
        VedtaksperiodeDto(
            id = id,
            fom = fom,
            tom = tom,
            målgruppeType = målgruppe,
            aktivitetType = aktivitet,
        )
}

fun List<Vedtaksperiode>.tilDto() = map { it.tilDto() }

fun List<Vedtaksperiode>.tilVedtaksperiodeBeregning() =
    map {
        VedtaksperiodeBeregning(
            fom = it.fom,
            tom = it.tom,
            målgruppe = it.målgruppe,
            aktivitet = it.aktivitet,
        )
    }
