package no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.automatisk

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import java.time.LocalDate

object MålgruppeUtil {
    fun List<Vilkårperiode>.tilMålgrupper(): List<MålgruppeHolder> = this
        .filter { it.type is MålgruppeType }
        .map { MålgruppeHolder(it.fom, it.tom, it.type as MålgruppeType) }
}

data class MålgruppeHolder(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val type: MålgruppeType,
) : Periode<LocalDate>
