package no.nav.tilleggsstonader.sak.utbetaling.id

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Forvaltning")
@RestController
@RequestMapping("/api/forvaltning/migrer-utbetalinger")
@ProtectedWithClaims(issuer = "azuread")
class FagsakUtbetalingIdMigreringController(
    private val tilgangService: TilgangService,
    private val fagsakUtbetalingIdRepository: FagsakUtbetalingIdRepository,
    private val taskService: TaskService,
) {
    @PostMapping
    fun migrerUtbetalinger() {
        tilgangService.validerHarUtviklerrolle()
        val fagsakerUtenUtbetalingId = fagsakUtbetalingIdRepository.finnAlleFagsakerUtenUtbetalingId().take(10)
        taskService.saveAll(
            fagsakerUtenUtbetalingId
                .map { FagsakUtbetalingIdMigrieringTask.opprettTask(it) }
                .filter {
                    taskService
                        .finnAlleTaskerMedPayloadOgType(
                            payload = it.toString(),
                            type = FagsakUtbetalingIdMigrieringTask.TYPE,
                        ).isEmpty()
                },
        )
    }
}
