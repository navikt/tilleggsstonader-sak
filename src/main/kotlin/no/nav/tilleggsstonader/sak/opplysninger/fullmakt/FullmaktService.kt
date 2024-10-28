package no.nav.tilleggsstonader.sak.opplysninger.fullmakt

import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.fullmakt.FullmektigDto
import no.nav.tilleggsstonader.sak.util.isEqualOrBefore
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDate.now

@Component
class FullmaktService(
    private val fullmaktClient: FullmaktClient,
) {
    val log: Logger = LoggerFactory.getLogger(javaClass)

    fun hentFullmektige(fullmaktsgiversIdent: String): List<FullmektigDto> {
        return try {
            fullmaktClient.hentFullmektige(fullmaktsgiversIdent)
                .filter { it.gjelderTilleggsstønader() }
                .filter { it.erAktiv() }
        } catch (ex: Exception) {
            log.error("Kunne ikke hente fullmakter: {}", ex.message)
            emptyList()
        }
    }
}

private fun FullmektigDto.gjelderTilleggsstønader() =
    temaer.any { tema -> tema in listOf(Tema.TSO.name, Tema.TSR.name) }

private fun FullmektigDto.erAktiv() =
    gyldigFraOgMed.isEqualOrBefore(now()) &&
        (gyldigTilOgMed == null || gyldigTilOgMed!!.isAfter(now()))
