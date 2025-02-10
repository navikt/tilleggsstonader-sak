package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate

data class VedtaksperiodeBeregningsgrunnlag(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetType,
) : Periode<LocalDate>,
    KopierPeriode<VedtaksperiodeBeregningsgrunnlag> {
    constructor(stønadsperiode: Stønadsperiode) : this(
        fom = stønadsperiode.fom,
        tom = stønadsperiode.tom,
        målgruppe = stønadsperiode.målgruppe,
        aktivitet = stønadsperiode.aktivitet,
    )
    constructor(vedtaksperiodeDto: VedtaksperiodeDto) : this(
        fom = vedtaksperiodeDto.fom,
        tom = vedtaksperiodeDto.tom,
        målgruppe = vedtaksperiodeDto.målgruppe,
        aktivitet = vedtaksperiodeDto.aktivitet,
    )

    init {
        validatePeriode()
    }

    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): VedtaksperiodeBeregningsgrunnlag = this.copy(fom = fom, tom = tom)
}
