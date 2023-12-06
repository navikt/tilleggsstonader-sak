package no.nav.tilleggsstonader.sak.vilkår.dto

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vilkår.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.domain.VilkårperiodeType
import java.time.LocalDate

data class VilkårperiodeDto(
    val type: VilkårperiodeType,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val vilkår: VilkårDto,
) : Periode<LocalDate>

fun Vilkårperiode.tilDto(vilkår: VilkårDto) =
    VilkårperiodeDto(
        type = this.type,
        fom = this.fom,
        tom = this.tom,
        vilkår = vilkår,
    )

data class OpprettVilkårperiode(
    val type: VilkårperiodeType,
    override val fom: LocalDate,
    override val tom: LocalDate,
) : Periode<LocalDate>

data class Vilkårperioder(
    val målgrupper: List<VilkårperiodeDto>,
    val aktiviteter: List<VilkårperiodeDto>,
)
