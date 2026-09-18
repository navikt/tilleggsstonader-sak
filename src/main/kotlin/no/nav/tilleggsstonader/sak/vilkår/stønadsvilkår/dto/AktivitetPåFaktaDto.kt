package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dto

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeAktivitet
import java.time.LocalDate

data class AktivitetPåFaktaDto(
    val aktivitetType: String,
    val tiltaksvariant: String?,
    val fom: LocalDate,
    val tom: LocalDate,
)

fun VilkårperiodeAktivitet.tilAktivitetPåFaktaDto() =
    AktivitetPåFaktaDto(
        aktivitetType = this.type.tilDbType(),
        tiltaksvariant = this.tiltaksvariant?.beskrivelse,
        fom = this.fom,
        tom = this.tom,
    )
