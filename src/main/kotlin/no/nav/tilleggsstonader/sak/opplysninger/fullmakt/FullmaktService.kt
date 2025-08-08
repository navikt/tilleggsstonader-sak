package no.nav.tilleggsstonader.sak.opplysninger.fullmakt

import no.nav.tilleggsstonader.kontrakter.felles.Tema.Companion.gjelderTemaTilleggsstønader
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

    fun hentFullmektige(fullmaktsgiversIdent: String): List<FullmektigDto> =
        try {
            fullmaktClient
                .hentFullmektige(fullmaktsgiversIdent)
                .filter { it.temaer.any(::gjelderTemaTilleggsstønader) }
                .filter { it.erAktiv() }
        } catch (ex: Exception) {
            log.error("Kunne ikke hente fullmakter: {}", ex.message)
            emptyList()
        }
}

private fun FullmektigDto.erAktiv() =
    gyldigFraOgMed.isEqualOrBefore(now()) &&
        (gyldigTilOgMed == null || gyldigTilOgMed!!.isAfter(now()))
