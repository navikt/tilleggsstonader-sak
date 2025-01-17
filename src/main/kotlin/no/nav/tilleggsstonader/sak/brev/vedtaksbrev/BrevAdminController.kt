package no.nav.tilleggsstonader.sak.brev.vedtaksbrev

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.tilleggsstonader.kontrakter.dokdist.DistribuerJournalpostRequest
import no.nav.tilleggsstonader.kontrakter.dokdist.Distribusjonstype
import no.nav.tilleggsstonader.kontrakter.felles.Fagsystem
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerVedtaksbrevRepository
import no.nav.tilleggsstonader.sak.brev.brevmottaker.domain.BrevmottakerVedtaksbrev
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.journalføring.JournalpostClient
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/brev/admin")
@ProtectedWithClaims(issuer = "azuread")
class BrevAdminController(
    private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
    private val brevmottakerVedtaksbrevRepository: BrevmottakerVedtaksbrevRepository,
    private val journalpostClient: JournalpostClient,
) {

    private fun utførEndringSomSystem() {
        SpringTokenValidationContextHolder().setTokenValidationContext(null)
    }

    @PostMapping("/{journalpostId}")
    fun distribuerPåNytt(@PathVariable journalpostId: String) {
        utførEndringSomSystem()

        val id = namedParameterJdbcTemplate.query(
            "select id from brevmottakere where journalpostid=:journalpostId",
            mapOf("journalpostId" to journalpostId),
        ) { rs, _ -> UUID.fromString(rs.getString("id")) }
            .single()

        val brevmottaker = brevmottakerVedtaksbrevRepository.findByIdOrThrow(id)
        val bestillingId = distribuerTilBrevmottaker(brevmottaker)
        brevmottakerVedtaksbrevRepository.update(brevmottaker.copy(bestillingId = bestillingId))
    }

    private fun distribuerTilBrevmottaker(it: BrevmottakerVedtaksbrev) = journalpostClient.distribuerJournalpost(
        DistribuerJournalpostRequest(
            journalpostId = it.journalpostId
                ?: error("Ugyldig tilstand. Mangler journalpostId for brev som skal distribueres"),
            bestillendeFagsystem = Fagsystem.TILLEGGSSTONADER,
            dokumentProdApp = "TILLEGGSSTONADER-SAK",
            distribusjonstype = Distribusjonstype.VEDTAK,
        ),
    )
}
