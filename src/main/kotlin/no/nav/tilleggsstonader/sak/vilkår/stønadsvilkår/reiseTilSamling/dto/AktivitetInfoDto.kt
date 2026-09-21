package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.dto

import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import java.time.LocalDate

data class AktivitetInfoDto(
    val aktivitetId: VilkårperiodeGlobalId,
    val aktivitetType: String,
    val tiltaksvariantBeskrivelse: String?,
    val fom: LocalDate,
    val tom: LocalDate,
)

fun VilkårperiodeAktivitet.tilAktivitetInfoDto() =
    AktivitetInfoDto(
        aktivitetId = this.globalId,
        aktivitetType = (this.type as? AktivitetType)?.name ?: error("Aktivitet har ugyldig type=${this.type}"),
        tiltaksvariantBeskrivelse = this.tiltaksvariant?.beskrivelse,
        fom = this.fom,
        tom = this.tom,
    )
