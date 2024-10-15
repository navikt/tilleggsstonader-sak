package no.nav.tilleggsstonader.sak.opplysninger.fullmakt

import no.nav.tilleggsstonader.kontrakter.felles.Tema
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FullmaktService(
    private val fullmaktClient: FullmaktClient,
) {
    val log = LoggerFactory.getLogger(FullmaktService::class.java)
    fun hentFullmektige(fullmaktsgiversIdent: String): List<FullmektigDto> {
        return try {
            fullmaktClient.hentFullmektige(fullmaktsgiversIdent)
            // .filtrerPåTilleggsstønadTemaer()
        } catch (ex: Exception) {
            log.error("Klarte ikke hente fullmakter: {} \n {}", ex.message, ex.stackTraceToString())
            emptyList()
        }
    }
}

private fun List<FullmektigDto>.filtrerPåTilleggsstønadTemaer() =
    filter { it.temaer.any { tema -> tema in listOf(Tema.TSO.name, Tema.TSR.name) } }
