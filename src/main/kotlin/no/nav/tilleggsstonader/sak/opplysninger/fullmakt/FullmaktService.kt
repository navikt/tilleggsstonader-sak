package no.nav.tilleggsstonader.sak.opplysninger.fullmakt

import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.fullmakt.FullmektigDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FullmaktService(
    private val fullmaktClient: FullmaktClient,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    fun hentFullmektige(fullmaktsgiversIdent: String): List<FullmektigDto> {
        log.info("Henter fullmektige fra tilleggsstonader-integrasjoner...")
        return try {
            fullmaktClient.hentFullmektige(fullmaktsgiversIdent).filtrerPåTilleggsstønadTemaer()
        } catch (ex: Exception) {
            log.error("Kunne ikke hente fullmakter: {}", ex.message)
            emptyList()
        }
    }
}

private fun List<FullmektigDto>.filtrerPåTilleggsstønadTemaer() =
    filter { it.temaer.any { tema -> tema in listOf(Tema.TSO.name, Tema.TSR.name) } }
