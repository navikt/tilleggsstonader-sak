package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.tilleggsstonader.kontrakter.felles.Behandlingstema
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto.FinnOppgaveRequestDto
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.jvm.optionals.getOrNull

@RestController
@RequestMapping("/api/oppgave/admin")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OppdaterMappePåVåreOppgaverController(
    private val oppgaveClient: OppgaveClient,
    private val oppgaveService: OppgaveService,
    private val behandlingRepository: BehandlingRepository,
    private val oppgaveRepository: OppgaveRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private fun utførEndringSomSystem() {
        SpringTokenValidationContextHolder().setTokenValidationContext(null)
    }

    @GetMapping("/kontroller")
    fun kontroller(): Map<String, Any> {
        utførEndringSomSystem()

        val response = oppgaveClient.hentOppgaver(
            FinnOppgaveRequestDto(
                behandlingstema = Behandlingstema.TilsynBarn.name,
                ident = null,
                oppgaverPåVent = false,
                limit = 1000,
            ).tilFinnOppgaveRequest(null, oppgaveService.finnVentemappe()),
        )

        val mapper = oppgaveService.finnMapper(response.oppgaver.mapNotNull { it.tildeltEnhetsnr }.distinct()).map { it.id to it }

        val oppgaver = response.oppgaver
            .groupBy { it.oppgavetype }
            .mapValues { it.value.groupBy { it.mappeId?.getOrNull() }.mapValues { it.value.size } }
        return mapOf(
            "mapper" to mapper,
            "oppgaver" to oppgaver,
        )
    }
}
