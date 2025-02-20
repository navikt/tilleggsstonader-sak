package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate
import java.util.UUID

data class Vedtaksperiode(
    val id: UUID,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetType,
) : Periode<LocalDate> {
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
