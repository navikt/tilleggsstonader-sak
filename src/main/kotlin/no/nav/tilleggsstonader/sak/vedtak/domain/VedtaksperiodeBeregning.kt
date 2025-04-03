package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate

/**
 * TODO denne kan erstatte VedtaksperiodeBeregningsgrunnlag når denne har tatt i bruk faktisk målgruppe
 */
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
}
