package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Iverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnInternStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Totrinnskontroll
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class IverksettService(
    private val iverksettClient: IverksettClient,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val taskService: TaskService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Ved første iverksetting av en behandling er det krav på at det gjøres med jwt, dvs med saksbehandler-token
     * Neste iverksettinger kan gjøres med client_credentials
     *
     * Hvis kallet feiler er det viktig at den samme iverksettingId brukes for å kunne ignorere conflict
     * Vid første iverksetting som gjøres burde man bruke behandlingId for å iverksette
     * for å enkelt kunne gjenbruke samme id vid neste iverksetting
     */
    @Transactional
    fun iverksett(behandlingId: UUID, iverksettingId: UUID) {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        if (!behandling.resultat.skalIverksettes) {
            logger.info("Iverksetter ikke behandling=$behandlingId då status=${behandling.status}")
            return
        }
        val tilkjentYtelse = hentTilkjentYtelseOgOppdaterAndeler(behandlingId, iverksettingId)
        val totrinnskontroll = hentTotrinnskontroll(behandlingId)

        val dto = IverksettDtoMapper.map(
            behandling = behandling,
            tilkjentYtelse = tilkjentYtelse,
            totrinnskontroll = totrinnskontroll,
            iverksettingId = iverksettingId,
            forrigeIverksetting = forrigeIverksetting(),
        )
        opprettHentStatusFraIverksettingTask(behandlingId, iverksettingId)
        iverksettClient.iverksett(dto)
    }

    /**
     * TODO denne skal finne forrige iverksetting og behandlingId som ble brukt
     * I en iverksetting nr 2 på en og samme behandling kan det være den samme behandlingId som man har, og ikke forrigeBehandlingId på behandlingen
     */
    private fun forrigeIverksetting(): ForrigeIverksettingDto? {
        return null
    }

    private fun hentTotrinnskontroll(behandlingId: UUID): Totrinnskontroll {
        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontroll(behandlingId)
            ?: error("Finner ikke totrinnskontroll for behandling=$behandlingId")
        feilHvis(totrinnskontroll.status != TotrinnInternStatus.GODKJENT) {
            "Totrinnskontroll må være godkjent for å kunne iverksette"
        }
        return totrinnskontroll
    }

    private fun opprettHentStatusFraIverksettingTask(behandlingId: UUID, iverksettingId: UUID) {
        taskService.save(
            HentStatusFraIverksettingTask.opprettTask(
                behandlingId = behandlingId,
                iverksettingId = iverksettingId,
            ),
        )
    }

    private fun hentTilkjentYtelseOgOppdaterAndeler(
        behandlingId: UUID,
        iverksettingId: UUID,
    ): TilkjentYtelse {
        val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(behandlingId)
        val iverksetting = Iverksetting(iverksettingId, LocalDateTime.now())
        val oppdaterteAndeler = tilkjentYtelse.andelerTilkjentYtelse.map {
            it.copy(
                statusIverksetting = StatusIverksetting.SENDT,
                iverksetting = iverksetting,
            )
        }
        // TODO skal kun oppdatere andeler som har fått iverksettingId
        andelTilkjentYtelseRepository.updateAll(oppdaterteAndeler)
        return tilkjentYtelse.copy(andelerTilkjentYtelse = oppdaterteAndeler.toSet())
    }
}
