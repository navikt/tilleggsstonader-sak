package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.security.token.support.core.api.Unprotected
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.StatusEnum
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.concurrent.Executors

@RestController
@Unprotected
class OppfriskGamleOppgaverController(
    private val oppgaveRepository: OppgaveRepository,
    private val oppgaveClient: OppgaveClient,
) {
    private val logger = LoggerFactory.getLogger(OppfriskGamleOppgaverController::class.java)

    @GetMapping("/oppgaver/oppfrisk")
    fun oppfriskGamleOppgaver() {
        Executors.newVirtualThreadPerTaskExecutor().submit {
            oppgaveRepository
                .findByStatusAndTypeInAndSporbarOpprettetTidBefore(
                    status = Oppgavestatus.ÅPEN,
                    type = setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak, Oppgavetype.GodkjenneVedtak),
                    førTid = LocalDate.of(2025, 7, 1).atStartOfDay(),
                ).forEach { lagretOppgave ->
                    logger.info("Oppfrisker oppgave ${lagretOppgave.gsakOppgaveId} for behandling ${lagretOppgave.behandlingId}")
                    val oppgave = oppgaveClient.finnOppgaveMedId(lagretOppgave.gsakOppgaveId)
                    oppgaveRepository.update(
                        lagretOppgave.copy(
                            tildeltEnhetsnummer = oppgave.tildeltEnhetsnr,
                            tilordnetSaksbehandler = oppgave.tilordnetRessurs,
                            enhetsmappeId = oppgave.mappeId?.orElse(null),
                            status =
                                oppgave.status?.let {
                                    when (it) {
                                        StatusEnum.AAPNET, StatusEnum.OPPRETTET, StatusEnum.UNDER_BEHANDLING -> Oppgavestatus.ÅPEN
                                        StatusEnum.FERDIGSTILT -> Oppgavestatus.FERDIGSTILT
                                        StatusEnum.FEILREGISTRERT -> Oppgavestatus.FEILREGISTRERT
                                    }
                                } ?: lagretOppgave.status,
                        ),
                    )
                }
        }
    }
}
