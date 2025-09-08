package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.tasks.GjennoprettOppgavePåBehandlingTask
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/forvaltning/oppgave")
@ProtectedWithClaims(issuer = "azuread")
class GjenopprettOppgaveController(
    private val tilgangService: TilgangService,
    private val taskService: TaskService,
) {
    @PostMapping("/gjenopprett/{behandlingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun gjenopprettOppgave(
        @PathVariable("behandlingId") behandlingId: BehandlingId,
    ) {
        tilgangService.validerHarUtviklerrolle()
        taskService.save(
            GjennoprettOppgavePåBehandlingTask.opprettTask(behandlingId),
        )
    }
}
