package no.nav.tilleggsstonader.sak.utbetaling.migrering

import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.utbetaling.id.FagsakUtbetalingIdService
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.UtbetalingId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ReMigrerFagsakUtbetalingIdService(
    private val migreringClient: MigreringClient,
    private val fagsakUtbetalingIdService: FagsakUtbetalingIdService,
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val iverksettService: IverksettService,
    private val transactionHandler: TransactionHandler,
) {
    fun migrerForFagsak(fagsakId: FagsakId) {
        try {
            val sisteIverksatteBehandling =
                behandlingService
                    .finnSisteIverksatteBehandling(fagsakId)
                    ?.let { behandlingService.hentSaksbehandling(it.id) }

            val alleIverksatteBehandlingIderPåFagsak =
                behandlingService
                    .hentBehandlinger(fagsakId)
                    .filter { it.resultat.skalIverksettes && it.erFerdigstilt() && it.vedtakstidspunkt != null } // er iverksatt
                    .map { it.id }

            val alleAndelerPåFagsak =
                alleIverksatteBehandlingIderPåFagsak
                    .flatMap { iverksettService.hentAndelTilkjentYtelse(it) }

            val typeAndelerPåFagsaken =
                alleAndelerPåFagsak
                    .let { it.map { andelTilkjentYtelse -> andelTilkjentYtelse.type } }
                    .toSet()
                    .filter { it != TypeAndel.UGYLDIG }
                    .toSet()

            typeAndelerPåFagsaken.forEach { typeAndel ->
                if (sisteIverksatteBehandling != null) {
                    transactionHandler.runInNewTransaction {
                        logger.info("Migrerer $typeAndel for fagsak $fagsakId")
                        migrerForFagsakOgTypeAndel(sisteIverksatteBehandling, typeAndel)
                    }
                }
            }
        } catch (e: Exception) {
            secureLogger.error("Feil ved migrering av fagsak {}", fagsakId, e)
        }
    }

    private fun migrerForFagsakOgTypeAndel(
        sisteIverksatteBehandling: Saksbehandling,
        typeAndel: TypeAndel,
    ): UtbetalingId {
        val utbetalingId =
            fagsakUtbetalingIdService.hentEllerOpprettUtbetalingId(sisteIverksatteBehandling.fagsakId, typeAndel)
        migreringClient.migrer(
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

    companion object {
        private val logger = LoggerFactory.getLogger(ReMigrerFagsakUtbetalingIdService::class.java)
    }
}

fun TypeAndel.tilStønadstype(): StønadstypeIverksetting =
    when (this) {
        TypeAndel.TILSYN_BARN_ENSLIG_FORSØRGER -> StønadstypeIverksetting.TILSYN_BARN_ENSLIG_FORSØRGER
        TypeAndel.TILSYN_BARN_AAP -> StønadstypeIverksetting.TILSYN_BARN_AAP
        TypeAndel.TILSYN_BARN_ETTERLATTE -> StønadstypeIverksetting.TILSYN_BARN_ETTERLATTE

        TypeAndel.LÆREMIDLER_ENSLIG_FORSØRGER -> StønadstypeIverksetting.LÆREMIDLER_ENSLIG_FORSØRGER
        TypeAndel.LÆREMIDLER_AAP -> StønadstypeIverksetting.LÆREMIDLER_AAP
        TypeAndel.LÆREMIDLER_ETTERLATTE -> StønadstypeIverksetting.LÆREMIDLER_ETTERLATTE

        TypeAndel.BOUTGIFTER_AAP -> StønadstypeIverksetting.BOUTGIFTER_AAP
        TypeAndel.BOUTGIFTER_ENSLIG_FORSØRGER -> StønadstypeIverksetting.BOUTGIFTER_ENSLIG_FORSØRGER
        TypeAndel.BOUTGIFTER_ETTERLATTE -> StønadstypeIverksetting.BOUTGIFTER_ETTERLATTE

        else -> error("Uforventet TypeAndel $this")
    }
