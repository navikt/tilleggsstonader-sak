package no.nav.tilleggsstonader.sak.hendelser.personhendelse.dødsfall

import java.time.LocalDate

data class DødsfallHendelse(
    val dødsdato: LocalDate,
    val personidenter: Set<String>,
)
