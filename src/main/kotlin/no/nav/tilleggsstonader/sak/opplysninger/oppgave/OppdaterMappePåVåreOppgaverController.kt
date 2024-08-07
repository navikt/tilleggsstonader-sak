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

@RestController
@RequestMapping("/api/oppgave/admin")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OppdaterMappePåVåreOppgaverController(
    private val oppgaveService: OppgaveService,
    private val behandlingRepository: BehandlingRepository,
    private val oppgaveRepository: OppgaveRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private fun utførEndringSomSystem() {
        SpringTokenValidationContextHolder().setTokenValidationContext(null)
    }

    @GetMapping("/sjekkEnheter")
    fun sjekkEnheterIProd() {
        utførEndringSomSystem()

        val response = oppgaveService.hentOppgaver(
            FinnOppgaveRequestDto(
                behandlingstema = Behandlingstema.TilsynBarn.name,
                ident = null,
                oppgaverPåVent = false,
                limit = 1000,
            ),
        )
        response.oppgaver
            .groupBy { it.tildeltEnhetsnr }
            .mapValues { it.value.size }
            .let { logger.info("Oppgaver er fordelt på enheter=$it") }
    }
}
