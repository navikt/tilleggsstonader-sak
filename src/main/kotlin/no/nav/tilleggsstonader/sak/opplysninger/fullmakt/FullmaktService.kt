package no.nav.tilleggsstonader.sak.opplysninger.fullmakt

import no.nav.tilleggsstonader.kontrakter.felles.Tema
import org.springframework.stereotype.Component

@Component
class FullmaktService(
    private val fullmaktClient: FullmaktClient,
) {
    fun hentFullmektige(fullmaktsgiversIdent: String): List<FullmektigDto> {
        return fullmaktClient.hentFullmektige(fullmaktsgiversIdent)
        // .filtrerPåTilleggsstønadTemaer()
    }
}

private fun List<FullmektigDto>.filtrerPåTilleggsstønadTemaer() =
    filter { it.temaer.any { tema -> tema in listOf(Tema.TSO.name, Tema.TSR.name) } }
