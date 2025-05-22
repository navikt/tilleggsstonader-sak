package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

data class VedtaksperiodeBeregning(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppe: FaktiskMålgruppe,
    val aktivitet: AktivitetType,
) : Periode<LocalDate>,
    KopierPeriode<VedtaksperiodeBeregning> {
    constructor(vedtaksperiodeDto: VedtaksperiodeDto) : this(
        fom = vedtaksperiodeDto.fom,
        tom = vedtaksperiodeDto.tom,
        målgruppe = vedtaksperiodeDto.målgruppeType,
        aktivitet = vedtaksperiodeDto.aktivitetType,
    )

    init {
        validatePeriode()
    }

    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): VedtaksperiodeBeregning = this.copy(fom = fom, tom = tom)

    companion object {
        fun List<VedtaksperiodeBeregning>.mergeSammenhengende() =
            mergeSammenhengende(
                { v1, v2 -> v1.overlapperEllerPåfølgesAv(v2) },
                { v1, v2 -> v1.medPeriode(fom = minOf(v1.fom, v2.fom), tom = maxOf(v1.tom, v2.tom)) },
            )
    }
}
