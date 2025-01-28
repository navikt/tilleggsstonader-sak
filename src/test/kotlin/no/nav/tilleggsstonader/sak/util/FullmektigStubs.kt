package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.kontrakter.fullmakt.FullmektigDto
import java.time.LocalDate.now
import java.time.LocalDate.parse

object FullmektigStubs {
    val gyldig =
        FullmektigDto(
            fullmektigIdent = "66611144422",
            fullmektigNavn = "Maud-Rita",
            gyldigFraOgMed = parse("2023-01-01"),
            gyldigTilOgMed = now().plusYears(10),
            temaer = listOf("TSO"),
        )
    val gyldigPåUbestemtTid = gyldig.copy(gyldigTilOgMed = null)
    val ikkeGyldigEnda = gyldig.copy(gyldigFraOgMed = now().plusDays(10))
    val utgått = gyldig.copy(gyldigTilOgMed = now().minusYears(1))
    val ikkeRelevantTema = gyldig.copy(temaer = listOf("AAP"))
    val medFlereTemaer = gyldig.copy(temaer = listOf("AAP", "XXX", "TSR"))
}
