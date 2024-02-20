package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import no.nav.familie.prosessering.error.TaskExceptionUtenStackTrace
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class IverksettStatusService(
    private val iverksettClient: IverksettClient,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun hentStatusOgOppdaterAndeler(eksternFagsakId: Long, behandlingId: UUID, iverksettingId: UUID) {
        val status = iverksettClient.hentStatus(eksternFagsakId, behandlingId, iverksettingId)
        if (status != IverksettStatus.OK) {
            throw TaskExceptionUtenStackTrace("Status fra oppdrag er ikke ok, status=$status")
        }

        val andeler = hentAndelerForIverksettingId(behandlingId, iverksettingId)
        logger.info("Oppdaterer ${andeler.size} andeler med iverksettingId=$iverksettingId")
        andelTilkjentYtelseRepository.updateAll(andeler.map { it.copy(statusIverksetting = StatusIverksetting.OK) })
    }

    private fun hentAndelerForIverksettingId(
        behandlingId: UUID,
        iverksettingId: UUID,
    ): List<AndelTilkjentYtelse> {
        val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(behandlingId)
        val andeler = tilkjentYtelse.andelerTilkjentYtelse.filter { it.iverksetting?.iverksettingId == iverksettingId }
        feilHvis(andeler.any { it.statusIverksetting != StatusIverksetting.SENDT }) {
            "Finnes andeler på behandling=$behandlingId som har annen status enn ${StatusIverksetting.SENDT}"
        }
        feilHvis(andeler.isEmpty()) {
            "Forventet å finne minimum en andel for behandling=$behandlingId iverksetting=$iverksettingId"
        }
        return andeler
    }
}
