package no.nav.tilleggsstonader.sak.opplysninger.pdl

import no.nav.tilleggsstonader.sak.infrastruktur.config.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlBolkResponse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdentBolkResponse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders

val logger: Logger = LoggerFactory.getLogger(PdlClient::class.java)

object PdlUtil {
    val httpHeaders = HttpHeaders().apply {
        add("Tema", "ENF") // TODO
        add("behandlingsnummer", "B289")
    }
}

inline fun <reified DATA : Any, reified RESULT : Any> feilsjekkOgReturnerData(
    ident: String?,
    pdlResponse: PdlResponse<DATA>,
    dataMapper: (DATA) -> RESULT?,
): RESULT {
    if (pdlResponse.harFeil()) {
        if (pdlResponse.errors?.any { it.extensions?.notFound() == true } == true) {
            throw PdlNotFoundException()
        }
        secureLogger.error("Feil ved henting av ${RESULT::class} fra PDL: ${pdlResponse.errorMessages()}")
        throw PdlRequestException("Feil ved henting av ${RESULT::class} fra PDL. Se secure logg for detaljer.")
    }
    if (pdlResponse.harAdvarsel()) {
        logger.warn("Advarsel ved henting av ${RESULT::class} fra PDL. Se securelogs for detaljer.")
        secureLogger.warn("Advarsel ved henting av ${RESULT::class} fra PDL: ${pdlResponse.extensions?.warnings}")
    }
    val data = dataMapper.invoke(pdlResponse.data)
    if (data == null) {
        val errorMelding = if (ident != null) "Feil ved oppslag på ident $ident. " else "Feil ved oppslag på person."
        secureLogger.error(
            errorMelding +
                "PDL rapporterte ingen feil men returnerte tomt datafelt",
        )
        throw PdlRequestException("Manglende ${RESULT::class} ved feilfri respons fra PDL. Se secure logg for detaljer.")
    }
    return data
}

inline fun <reified RESULT : Any> feilsjekkOgReturnerData(pdlResponse: PdlBolkResponse<RESULT>): Map<String, RESULT> {
    if (pdlResponse.data == null) {
        secureLogger.error("Data fra pdl er null ved bolkoppslag av ${RESULT::class} fra PDL: ${pdlResponse.errorMessages()}")
        throw PdlRequestException("Data er null fra PDL -  ${RESULT::class}. Se secure logg for detaljer.")
    }

    val feil = pdlResponse.data.personBolk.filter { it.code != "ok" }.associate { it.ident to it.code }
    if (feil.isNotEmpty()) {
        secureLogger.error("Feil ved henting av ${RESULT::class} fra PDL: $feil")
        throw PdlRequestException("Feil ved henting av ${RESULT::class} fra PDL. Se secure logg for detaljer.")
    }
    if (pdlResponse.harAdvarsel()) {
        logger.warn("Advarsel ved henting av ${RESULT::class} fra PDL. Se securelogs for detaljer.")
        secureLogger.warn("Advarsel ved henting av ${RESULT::class} fra PDL: ${pdlResponse.extensions?.warnings}")
    }
    return pdlResponse.data.personBolk.associateBy({ it.ident }, { it.person!! })
}

fun feilmeldOgReturnerData(pdlResponse: PdlIdentBolkResponse): Map<String, PdlIdent> {
    if (pdlResponse.data == null) {
        secureLogger.error("Data fra pdl er null ved bolkoppslag av identer fra PDL: ${pdlResponse.errorMessages()}")
        throw PdlRequestException("Data er null fra PDL -  ${PdlIdentBolkResponse::class}. Se secure logg for detaljer.")
    }

    val feil = pdlResponse.data.hentIdenterBolk.filter { it.code != "ok" }.associate { it.ident to it.code }
    if (feil.isNotEmpty()) {
        // Logg feil og gå vider. Ved feil returneres nåværende ident.
        logger.error("Feil ved henting av ${PdlIdentBolkResponse::class}. Nåværende ident returnert.")
        secureLogger.error("Feil ved henting av ${PdlIdentBolkResponse::class} fra PDL: $feil. Nåværende ident returnert.")
    }
    return pdlResponse.data.hentIdenterBolk.associateBy({ it.ident }, { it.gjeldende() })
}
