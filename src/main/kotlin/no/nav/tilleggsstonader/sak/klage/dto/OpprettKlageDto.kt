package no.nav.tilleggsstonader.sak.klage.dto

import java.time.LocalDate

data class OpprettKlageDto(val mottattDato: LocalDate, val klageGjelderTilbakekreving: Boolean)
