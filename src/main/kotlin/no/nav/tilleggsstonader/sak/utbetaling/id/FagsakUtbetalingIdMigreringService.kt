package no.nav.tilleggsstonader.sak.utbetaling.id

import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettClient
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettDtoMapper.tilStønadstype
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.MigrerUtbetalingDto
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FagsakUtbetalingIdMigreringService(
    private val iverksettClient: IverksettClient,
    private val fagsakUtbetalingIdService: FagsakUtbetalingIdService,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService,
) {
    // Returnerer utbetalingId for fagsak og typeAndel
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun migrerForFagsakOgTypeAndel(
        fagsakId: FagsakId,
        typeAndel: TypeAndel,
    ): UUID {
        val utbetalingId = fagsakUtbetalingIdService.hentEllerOpprettUtbetalingId(fagsakId, typeAndel)
        val sisteIverksatteBehandling = behandlingService.finnSisteIverksatteBehandling(fagsakId)
        iverksettClient.migrer(
            MigrerUtbetalingDto(
                sakId = fagsakId.toString(),
                behandlingId = sisteIverksatteBehandling!!.id.toString(),
                iverksettingId = finnSisteIverksettingId(sisteIverksatteBehandling),
                meldeperiode = null,
                uidToStønad = utbetalingId.utbetalingId to typeAndel.tilStønadstype(),
            ),
        )

        return utbetalingId.utbetalingId
    }

    private fun finnSisteIverksettingId(sisteIverksatteBehandling: Behandling): String =
        tilkjentYtelseService
            .hentForBehandling(behandlingId = sisteIverksatteBehandling.id)
            .andelerTilkjentYtelse
            .filter { it.iverksetting != null }
            .maxBy { it.iverksetting!!.iverksettingTidspunkt }
            .iverksetting!!
            .iverksettingId
            .toString()
}
