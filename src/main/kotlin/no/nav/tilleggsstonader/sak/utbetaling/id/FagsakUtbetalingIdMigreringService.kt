package no.nav.tilleggsstonader.sak.utbetaling.id

import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettClient
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettDtoMapper.tilStønadstype
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.MigrerUtbetalingDto
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FagsakUtbetalingIdMigreringService(
    private val iverksettClient: IverksettClient,
    private val fagsakUtbetalingIdService: FagsakUtbetalingIdService,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val iverksettService: IverksettService,
    private val transactionHandler: TransactionHandler,
    private val unleashService: UnleashService,
) {
    fun migrerForFagsak(fagsakId: FagsakId) {
        if (unleashService.isEnabled(Toggle.SKAL_MIGRERE_UTBETALING_MOT_KAFKA)) {
            val sisteIverksatteBehandling =
                behandlingService
                    .finnSisteIverksatteBehandling(fagsakId)
                    ?.let { behandlingService.hentSaksbehandling(it.id) }
            val andelTilkjentYtelseListe =
                sisteIverksatteBehandling?.let { iverksettService.hentAndelTilkjentYtelse(it.id) }
            val typeAndelerPåFagsaken =
                andelTilkjentYtelseListe?.let { it.map { andelTilkjentYtelse -> andelTilkjentYtelse.type } }
                    ?: emptyList()

            typeAndelerPåFagsaken.forEach { typeAndel ->
                if (skalMigrereTilKafka(fagsakId, typeAndel) && sisteIverksatteBehandling !== null) {
                    transactionHandler.runInNewTransaction {
                        migrerForFagsakOgTypeAndel(sisteIverksatteBehandling, typeAndel)
                    }
                }
            }
        }
    }

    private fun skalMigrereTilKafka(
        fagsakId: FagsakId,
        typeAndel: TypeAndel,
    ): Boolean =
        !fagsakUtbetalingIdService.finnesUtbetalingsId(fagsakId, typeAndel) &&
            typeAndel in
            listOf(
                TypeAndel.LÆREMIDLER_AAP,
                TypeAndel.LÆREMIDLER_ETTERLATTE,
                TypeAndel.LÆREMIDLER_ENSLIG_FORSØRGER,
            )

    // Returnerer utbetalingId for fagsak og typeAndel
    private fun migrerForFagsakOgTypeAndel(
        sisteIverksatteBehandling: Saksbehandling,
        typeAndel: TypeAndel,
    ): UUID {
        val utbetalingId =
            fagsakUtbetalingIdService.hentEllerOpprettUtbetalingId(sisteIverksatteBehandling.fagsakId, typeAndel)
        iverksettClient.migrer(
            MigrerUtbetalingDto(
                sakId = sisteIverksatteBehandling.eksternFagsakId.toString(),
                behandlingId = sisteIverksatteBehandling.eksternId.toString(),
                iverksettingId = finnSisteIverksettingId(sisteIverksatteBehandling.id),
                meldeperiode = null,
                uidToStønad = utbetalingId.utbetalingId to typeAndel.tilStønadstype(),
            ),
        )

        return utbetalingId.utbetalingId
    }

    private fun finnSisteIverksettingId(sisteIverksatteBehandlingId: BehandlingId): String =
        tilkjentYtelseService
            .hentForBehandling(behandlingId = sisteIverksatteBehandlingId)
            .andelerTilkjentYtelse
            .mapNotNull { it.iverksetting }
            .maxByOrNull { it.iverksettingTidspunkt }
            ?.iverksettingId
            ?.toString()
            ?: error("Fant ingen iverksetting for behandling $sisteIverksatteBehandlingId")
}
