package no.nav.tilleggsstonader.sak.opplysninger.aktivitet

import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import java.time.LocalDate

data class RegisterAktiviteterDto(
    val periodeHentetFra: LocalDate,
    val periodeHentetTil: LocalDate,
    val aktiviteter: List<AktivitetArenaDto>,
)
