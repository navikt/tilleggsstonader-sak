package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate
import java.util.UUID
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.Stønadsperiode as StønadsperiodeInngangsvilkår

data class Stønadsperiode(
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

fun StønadsperiodeInngangsvilkår.tilGrunnlagStønadsperiode() = Stønadsperiode(
    id = this.id,
    fom = this.fom,
    tom = this.tom,
    målgruppe = this.målgruppe,
    aktivitet = this.aktivitet,
)

fun List<StønadsperiodeInngangsvilkår>.tilSortertGrunnlagStønadsperiode() =
    this.map { it.tilGrunnlagStønadsperiode() }.sortedBy { it.fom }
