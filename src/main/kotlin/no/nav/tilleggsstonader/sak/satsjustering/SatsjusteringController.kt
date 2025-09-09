package no.nav.tilleggsstonader.sak.satsjustering

import io.swagger.v3.oas.annotations.tags.Tag
import no.nav.familie.prosessering.internal.TaskService
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.api.Unprotected
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.satser
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Forvaltning")
@RestController
@RequestMapping("/api/forvaltning")
@ProtectedWithClaims(issuer = "azuread")
class SatsjusteringController(
    private val finnBehandlingerForSatsjusteringService: FinnBehandlingerForSatsjusteringService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val taskService: TaskService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping("/satsjustering")
    @Transactional
    fun kjørSatsjustering(): List<BehandlingId> {
        val behandlingerTilSatsjustering =
            finnBehandlingerForSatsjusteringService
                .finnBehandlingerForSatsjustering(Stønadstype.LÆREMIDLER)
                .filter { harAndelerSomKanSatsjusteres(it) }

        val satsjusteringTasker =
            behandlingerTilSatsjustering
                .map { SatsjusteringTask.opprettTask(it) }

        logger.info("Oppretter {} tasker til satsjustering", satsjusteringTasker.size)
        taskService.saveAll(satsjusteringTasker)

        return behandlingerTilSatsjustering
    }

    private fun harAndelerSomKanSatsjusteres(behandlingId: BehandlingId): Boolean {
        val årMedBekreftedeSatser =
            satser
                .filter { it.bekreftet }
                .map { it.fom.year }

        return tilkjentYtelseService
            .hentForBehandling(behandlingId)
            .andelerTilkjentYtelse
            .filter { it.statusIverksetting == StatusIverksetting.VENTER_PÅ_SATS_ENDRING }
            .any { it.fom.year in årMedBekreftedeSatser }
    }
}
