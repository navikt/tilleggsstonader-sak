package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.GjenopprettOppgavePåBehandlingTask
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.Executors

@Tag(name = "Forvaltning")
@RestController
@RequestMapping("/api/forvaltning/oppgave")
@ProtectedWithClaims(issuer = "azuread")
class OppgaveforvaltningController(
    private val tilgangService: TilgangService,
    private val oppgaveService: OppgaveService,
    private val taskService: TaskService,
) {
    @PostMapping("/gjenopprett/{behandlingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun gjenopprettOppgave(
        @PathVariable("behandlingId") behandlingId: BehandlingId,
    ) {
        tilgangService.validerHarUtviklerrolle()
        taskService.save(
            GjenopprettOppgavePåBehandlingTask.opprettTask(behandlingId),
        )
    }

    @PostMapping("/oppdater-status/{behandlingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun oppdaterStatus(
        @PathVariable("behandlingId") behandlingId: BehandlingId,
    ) {
        tilgangService.validerHarUtviklerrolle()
        // Kjører i egen tråd for å ikke bruke OBO for å hente oppgave da det ikke er sikkert man har tilgang
        Executors.newVirtualThreadPerTaskExecutor().submit {
            oppgaveService.oppdaterStatusPåIkkeFerdigstilteOppgaverForBehandling(behandlingId)
        }
    }
}
