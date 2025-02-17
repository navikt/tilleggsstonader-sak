package no.nav.tilleggsstonader.sak.vedtak.dto

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate

data class VedtaksperiodeDto(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppeType: MålgruppeType,
    val aktivitetType: AktivitetType,
) : Periode<LocalDate>,
    KopierPeriode<VedtaksperiodeDto> {
    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): VedtaksperiodeDto = this.copy(fom = fom, tom = tom)
}

fun List<VedtaksperiodeDto>.tilVedtaksperiode() = map { Vedtaksperiode(it) }
