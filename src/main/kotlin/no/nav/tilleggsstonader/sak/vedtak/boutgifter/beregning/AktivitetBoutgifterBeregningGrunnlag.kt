package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.kontrakter.felles.KopierPeriode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeUtil.ofType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetBoutgifter
import java.time.LocalDate
import java.util.UUID

data class AktivitetBoutgifterBeregningGrunnlag(
    val id: UUID,
    val type: AktivitetType,
    override val fom: LocalDate,
    override val tom: LocalDate,
//    val prosent: Int,
//    val studienivå: Studienivå,
) : Periode<LocalDate>,
    KopierPeriode<AktivitetBoutgifterBeregningGrunnlag> {
    override fun medPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): AktivitetBoutgifterBeregningGrunnlag = this.copy(fom = fom, tom = tom)
}

fun List<Vilkårperiode>.tilAktiviteter(): List<AktivitetBoutgifterBeregningGrunnlag> =
    ofType<AktivitetBoutgifter>()
        .map {
//            val fakta = it.faktaOgVurdering.fakta
            AktivitetBoutgifterBeregningGrunnlag(
                id = it.id,
                type = it.faktaOgVurdering.type.vilkårperiodeType,
                fom = it.fom,
                tom = it.tom,
//                prosent = fakta.takeIfFaktaOrThrow<FaktaAktivitetBoutgifter>().prosent,
//                studienivå = fakta.takeIfFaktaOrThrow<FaktaAktivitetBoutgifter>().studienivå!!,
            )
        }
