package no.nav.tilleggsstonader.sak.vilkår.dto

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vilkår.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate
import java.util.UUID

data class StønadsperiodeDto(
    val id: UUID?,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val målgruppe: MålgruppeType,
    val aktivitet: AktivitetType,
) : Periode<LocalDate> {
    init {
        validatePeriode()
    }
}

fun Stønadsperiode.tilDto() = StønadsperiodeDto(
    id = this.id,
    fom = this.fom,
    tom = this.tom,
    målgruppe = this.målgruppe,
    aktivitet = this.aktivitet,
)

fun List<Stønadsperiode>.tilSortertDto() = this.map { it.tilDto() }.sortedBy { it.fom }
