package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.dto

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import java.time.LocalDate

data class AktivitetMedReiserDto(
    val aktivitetId: VilkårperiodeGlobalId,
    val aktivitetType: AktivitetType,
    val tiltaksvariant: TypeAktivitet?,
    val fom: LocalDate,
    val tom: LocalDate,
    val reiser: List<VilkårReiseOppstartAvslutningHjemreiseDto>,
)
