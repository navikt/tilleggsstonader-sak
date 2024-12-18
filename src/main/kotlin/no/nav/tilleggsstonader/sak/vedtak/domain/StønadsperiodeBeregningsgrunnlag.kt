package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate
import java.util.UUID

data class StønadsperiodeBeregningsgrunnlag(
    val id: UUID? = null,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetType,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }
}

fun Stønadsperiode.tilStønadsperiodeBeregningsgrunnlag() = StønadsperiodeBeregningsgrunnlag(
    id = this.id,
    fom = this.fom,
    tom = this.tom,
    målgruppe = this.målgruppe,
    aktivitet = this.aktivitet,
)

fun List<Stønadsperiode>.tilSortertStønadsperiodeBeregningsgrunnlag() =
    this.map { it.tilStønadsperiodeBeregningsgrunnlag() }.sortedBy { it.fom }
