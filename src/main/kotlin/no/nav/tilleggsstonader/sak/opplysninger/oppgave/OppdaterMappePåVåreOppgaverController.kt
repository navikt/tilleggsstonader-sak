package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.tilleggsstonader.kontrakter.felles.Behandlingstema
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
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
    private val oppgaveClient: OppgaveClient,
    private val oppgaveService: OppgaveService,
    private val behandlingRepository: BehandlingRepository,
    private val oppgaveRepository: OppgaveRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private fun utførEndringSomSystem() {
        SpringTokenValidationContextHolder().setTokenValidationContext(null)
    }

    val gyldigeOppgaveTyper = setOf(
        Oppgavetype.Journalføring,
        Oppgavetype.BehandleSak,
        Oppgavetype.BehandleUnderkjentVedtak,
        Oppgavetype.GodkjenneVedtak,
    ).map { it.value }.toSet()

    @GetMapping("/kontroller")
    fun kontroller(): Map<String, Any> {
        utførEndringSomSystem()

        val oppgaver = oppgaveClient.hentOppgaver(
            FinnOppgaveRequestDto(
                behandlingstema = Behandlingstema.TilsynBarn.name,
                ident = null,
                oppgaverPåVent = false,
                limit = 1000,
            ).tilFinnOppgaveRequest(
                aktørid = null,
                ventemappe = oppgaveService.finnMappe(OppgaveUtil.ENHET_NR_NAY, OppgaveMappe.PÅ_VENT),
            ),
        )
            .oppgaver
            .filter { oppgave -> oppgave.oppgavetype !in gyldigeOppgaveTyper }

        val oppgaveInfo = oppgaver
            .filter { it.tildeltEnhetsnr != "4462" }
            .groupBy { it.oppgavetype }
            .mapValues { it.value.groupBy { it.tildeltEnhetsnr }.mapValues { it.value.size } }
        return mapOf(
            "oppgaver" to oppgaveInfo,
        )
    }
}
