package no.nav.tilleggsstonader.sak.opplysninger.pdl

import no.nav.tilleggsstonader.kontrakter.pdl.GeografiskTilknytningDto
import no.nav.tilleggsstonader.kontrakter.pdl.PdlGeografiskTilknytningRequest
import no.nav.tilleggsstonader.kontrakter.pdl.PdlGeografiskTilknytningVariables
import no.nav.tilleggsstonader.kontrakter.pdl.PdlHentGeografiskTilknytning
import no.nav.tilleggsstonader.libs.http.client.AbstractRestClient
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlAnnenForelder
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlBarn
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlBolkResponse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlHentIdenter
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdentBolkRequest
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdentBolkRequestVariables
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdentBolkResponse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdentRequest
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdentRequestVariables
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlPersonBolkRequest
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlPersonBolkRequestVariables
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlPersonKort
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlPersonRequest
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlPersonRequestVariables
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlResponse
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlSøker
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlSøkerData
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

@Service
class PdlClient(
    @Value("\${clients.pdl.uri}") private val pdlUrl: URI,
    @Qualifier("azureClientCredential") restTemplate: RestTemplate,
) : AbstractRestClient(restTemplate) {
    private val pdlUri: String = UriComponentsBuilder.fromUri(pdlUrl).pathSegment(PdlConfig.PATH_GRAPHQL).toUriString()

    fun hentSøker(personIdent: String): PdlSøker {
        val request =
            PdlPersonRequest(
                variables = PdlPersonRequestVariables(personIdent),
                query = PdlConfig.søkerQuery,
            )

        val pdlResponse = postForEntity<PdlResponse<PdlSøkerData>>(pdlUri, request, PdlUtil.httpHeaders)

        return feilsjekkOgReturnerData(personIdent, pdlResponse) { it.person }
    }

    fun hentBarn(personIdenter: List<String>): Map<String, PdlBarn> {
        if (personIdenter.isEmpty()) return emptyMap()
        val request =
            PdlPersonBolkRequest(
                variables = PdlPersonBolkRequestVariables(personIdenter),
                query = PdlConfig.forelderBarnQuery,
            )

        val pdlResponse = postForEntity<PdlBolkResponse<PdlBarn>>(pdlUri, request, PdlUtil.httpHeaders)

        return feilsjekkOgReturnerData(pdlResponse)
    }

    fun hentAndreForeldre(personIdenter: List<String>): Map<String, PdlAnnenForelder> {
        if (personIdenter.isEmpty()) return emptyMap()
        val request =
            PdlPersonBolkRequest(
                variables = PdlPersonBolkRequestVariables(personIdenter),
                query = PdlConfig.annenForelderQuery,
            )
        val pdlResponse = postForEntity<PdlBolkResponse<PdlAnnenForelder>>(pdlUri, request, PdlUtil.httpHeaders)
        return feilsjekkOgReturnerData(pdlResponse)
    }

    fun hentPersonKortBolk(personIdenter: List<String>): Map<String, PdlPersonKort> {
        require(personIdenter.size <= 100) { "Liste med personidenter må være færre enn 100 st" }
        val request =
            PdlPersonBolkRequest(
                variables = PdlPersonBolkRequestVariables(personIdenter),
                query = PdlConfig.personBolkKortQuery,
            )
        val pdlResponse = postForEntity<PdlBolkResponse<PdlPersonKort>>(pdlUri, request, PdlUtil.httpHeaders)
        return feilsjekkOgReturnerData(pdlResponse)
    }

    /**
     * @param ident Ident til personen, samme hvilke type (Folkeregisterident, aktørid eller npid)
     * @return liste med personidenter (Folkeregisterident, aktørid eller npid)
     */
    fun hentPersonidenter(ident: String): PdlIdenter {
        val request =
            PdlIdentRequest(
                variables = PdlIdentRequestVariables(ident = ident, historikk = true),
                query = PdlConfig.hentIdentQuery,
            )
        val pdlResponse = postForEntity<PdlResponse<PdlHentIdenter>>(pdlUri, request, PdlUtil.httpHeaders)

        val pdlIdenter = feilsjekkOgReturnerData(ident, pdlResponse) { it.hentIdenter }

        if (pdlIdenter.identer.isEmpty()) {
            secureLogger.error("Finner ikke personidenter for personIdent i PDL $ident ")
        }
        return pdlIdenter
    }

    /**
     * @param identer Identene til personene, samme hvilke type (Folkeregisterident, aktørid eller npid).
     * For tiden (2020-03-22) maks 100 identer lovlig i spørring.
     * @return map med søkeident som nøkkel og liste av folkeregisteridenter
     */
    fun hentIdenterBolk(identer: List<String>): Map<String, PdlIdent> {
        feilHvis(identer.size > MAKS_ANTALL_IDENTER) {
            "Feil i spørring mot PDL. Antall identer i spørring overstiger $MAKS_ANTALL_IDENTER"
        }
        val request =
            PdlIdentBolkRequest(
                variables = PdlIdentBolkRequestVariables(identer, "FOLKEREGISTERIDENT"),
                query = PdlConfig.hentIdenterBolkQuery,
            )
        val pdlResponse = postForEntity<PdlIdentBolkResponse>(pdlUri, request, PdlUtil.httpHeaders)

        return feilmeldOgReturnerData(pdlResponse)
    }

    fun hentGeografiskTilknytning(ident: String): GeografiskTilknytningDto? {
        val request =
            PdlGeografiskTilknytningRequest(
                variables = PdlGeografiskTilknytningVariables(ident),
                query = PdlConfig.hentGeografiskTilknytningQuery,
            )

        val response = postForEntity<PdlResponse<PdlHentGeografiskTilknytning>>(pdlUri, request, PdlUtil.httpHeaders)

        return feilsjekkOgReturnerData(ident, response) { it.hentGeografiskTilknytning }
    }

    companion object {
        const val MAKS_ANTALL_IDENTER = 100
    }
}
