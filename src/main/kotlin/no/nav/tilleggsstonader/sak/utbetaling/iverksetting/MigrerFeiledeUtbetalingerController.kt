package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.utbetaling.id.FagsakUtbetalingIdMigreringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.Executors

@Tag(name = "Forvaltning")
@RestController
@RequestMapping("/api/forvaltning/migrer-saker-helved")
@ProtectedWithClaims(issuer = "azuread")
class MigrerFeiledeUtbetalingerController(
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val fagsakUtbetalingIdMigreringService: FagsakUtbetalingIdMigreringService,
    private val behandlingService: BehandlingService,
    private val tilgangService: TilgangService,
    private val taskService: TaskService,
) {
    @PostMapping("{iverksettingId}")
    @Transactional
    fun migrer(
        @PathVariable iverksettingId: UUID,
    ) {
        tilgangService.validerHarUtviklerrolle()
        val andelerForIverksettingId = andelTilkjentYtelseRepository.findByIverksettingIverksettingId(iverksettingId)

        feilHvis(andelerForIverksettingId.isEmpty()) {
            "Finner ingen andeler for iverksettingId=$iverksettingId"
        }

        val behandlingId =
            andelTilkjentYtelseRepository.finnBehandlingIdForIverksettingId(
                andelerForIverksettingId.first().iverksetting!!.iverksettingId,
            )
        val fagsakId = behandlingService.hentBehandling(behandlingId).fagsakId

        // Nullstill iverksetting
        andelTilkjentYtelseRepository.updateAll(
            andelerForIverksettingId.map {
                it.copy(
                    iverksetting = null,
                    statusIverksetting = StatusIverksetting.UBEHANDLET,
                )
            },
        )

        // Valider at denne behandlingen skal iverksettes i dag etter nullstilling av iverksetting
        val behandlingIderTilIverksetting =
            andelTilkjentYtelseRepository.finnBehandlingerForIverksetting(utbetalingsdato = LocalDate.now())
        feilHvis(behandlingIderTilIverksetting.size != 1) {
            "Forventer at kun en behandling skal iverksettes, men følgende skal iverksettes: $behandlingIderTilIverksetting"
        }
        feilHvis(behandlingIderTilIverksetting.single() != behandlingId) {
            "Forventet at behandlingId=$behandlingId skal iverksettes, men $behandlingIderTilIverksetting skal iverksettes"
        }

        // Hack for at migrering-kallet må kjøres mot helved med client-credentials
        val migreringAvFagsakFuture =
            Executors.newVirtualThreadPerTaskExecutor().submit {
                // Migrerer slik at kan utbetales over kafka
                fagsakUtbetalingIdMigreringService.migrerForFagsak(fagsakId, overstyrToggle = true)
            }

        // Venter på at kallet fullføres, propagerer evt exception som blir kastet
        migreringAvFagsakFuture.get()

        // Kjører DagligIverksettBehandlingTask, slik at andelen blir iverksatt
        taskService.save(
            DagligIverksettBehandlingTask.opprettTask(behandlingId, LocalDate.now(), UUID.randomUUID()),
        )
    }
}
