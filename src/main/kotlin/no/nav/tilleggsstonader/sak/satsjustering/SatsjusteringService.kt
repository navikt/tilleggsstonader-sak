package no.nav.tilleggsstonader.sak.satsjustering

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.SatsLæremidlerService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SatsjusteringService(
    private val taskService: TaskService,
    private val satsLæremidlerService: SatsLæremidlerService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val behandlingRepository: BehandlingRepository,
) {
    private val logger = LoggerFactory.getLogger(SatsjusteringService::class.java)

    fun opprettTaskerForBehandlingerSomKanSatsjusteres(stønadstype: Stønadstype): List<BehandlingId> {
        feilHvis(stønadstype != Stønadstype.LÆREMIDLER) {
            "Stønadstype $stønadstype støttes ikke for satsjustering."
        }

        val behandlingerTilSatsjustering =
            behandlingRepository
                .finnBehandlingerMedAndelerSomVenterPåSatsjustering(stønadstype)
                .filter { harAndelerSomKanSatsjusteres(it) }

        logger.info(
            "Oppretter tasker for {} behandlinger som trenger satsjustering for stønadstype {}",
            behandlingerTilSatsjustering.size,
            stønadstype,
        )

        taskService.saveAll(
            behandlingerTilSatsjustering
                .map { SatsjusteringTask.opprettTask(it) },
        )

        return behandlingerTilSatsjustering
    }

    private fun harAndelerSomKanSatsjusteres(behandlingId: BehandlingId): Boolean {
        val årMedBekreftedeSatser =
            satsLæremidlerService
                .alleSatser()
                .filter { it.bekreftet }
                .map { it.fom.year }

        return tilkjentYtelseService
            .hentForBehandling(behandlingId)
            .andelerTilkjentYtelse
            .filter { it.statusIverksetting == StatusIverksetting.VENTER_PÅ_SATS_ENDRING }
            .any { it.fom.year in årMedBekreftedeSatser }
    }
}
