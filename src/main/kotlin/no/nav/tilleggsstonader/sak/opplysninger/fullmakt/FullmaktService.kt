package no.nav.tilleggsstonader.sak.opplysninger.fullmakt

import no.nav.tilleggsstonader.kontrakter.felles.Tema
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpServerErrorException

@Component
class FullmaktService(
    private val fullmaktClient: FullmaktClient,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    fun hentFullmektige(fullmaktsgiversIdent: String): List<FullmektigDto> {
        log.info("Henter fullmektige fra tilleggsstonader-integrasjoner...")
        return try {
            fullmaktClient.hentFullmektige(fullmaktsgiversIdent)
                // .filtrerPåTilleggsstønadTemaer()
                .also { log.info("Fant n={} fullmektige på bruker", it.size) }
        } catch (ex: HttpServerErrorException) {
            log.error("Klarte ikke hente fullmakter: {} \n {}", ex.message, ex.stackTraceToString())
            emptyList()
        }
    }
}

private fun List<FullmektigDto>.filtrerPåTilleggsstønadTemaer() =
    filter { it.temaer.any { tema -> tema in listOf(Tema.TSO.name, Tema.TSR.name) } }
