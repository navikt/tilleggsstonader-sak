package no.nav.tilleggsstonader.sak.vedtak.dto

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet

data class TypeAktivitetDto(
    val kode: TypeAktivitet,
    val beskrivelse: String,
)

fun TypeAktivitet.tilDto() =
    TypeAktivitetDto(
        kode = this,
        beskrivelse = this.beskrivelse,
    )

fun TypeAktivitetDto.tilDomene() = this.kode
