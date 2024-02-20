package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
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
import java.time.YearMonth
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
     * Skal brukes når man iverksetter en behandling første gang, uansatt om det er behandling 1 eller behandling 2 på en fagsak
     * Dette fordi man skal iverksette alle andeler tom forrige måned.
     * Dvs denne skal brukes fra [no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.BeslutteVedtakSteg]
     *
     * Ved første iverksetting av en behandling er det krav på at det gjøres med jwt, dvs med saksbehandler-token
     * Neste iverksettinger kan gjøres med client_credentials
     */
    @Transactional
    fun iverksettBehandlingFørsteGang(behandlingId: UUID) {
        // TODO denne skal oppdatere andeler i forrige behandling til UAKTUELL hvis de ikke er iverksatte
        iverksett(behandlingId, behandlingId, YearMonth.now().minusMonths(1))
    }

    /**
     * Hvis kallet feiler er det viktig at den samme iverksettingId brukes for å kunne ignorere conflict
     * Vid første iverksetting som gjøres burde man bruke behandlingId for å iverksette
     * for å enkelt kunne gjenbruke samme id vid neste iverksetting
     *
     * @param [måned] Når man iverksetter en behandling første gangen så skal [måned] settes til forrige måned
     * fordi man skal iverksette tidligere måneder direkte.
     * Når man iverksetter samme behandling neste gang skal man bruke inneværende måned for å iverksette aktuell måned
     */
    @Transactional
    fun iverksett(behandlingId: UUID, iverksettingId: UUID, måned: YearMonth = YearMonth.now()) {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        if (!behandling.resultat.skalIverksettes) {
            logger.info("Iverksetter ikke behandling=$behandlingId då status=${behandling.status}")
            return
        }
        val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(behandlingId)
        val totrinnskontroll = hentTotrinnskontroll(behandlingId)

        val dto = IverksettDtoMapper.map(
            behandling = behandling,
            andelerTilkjentYtelse = finnAndelerTilIverksetting(tilkjentYtelse, iverksettingId, måned),
            totrinnskontroll = totrinnskontroll,
            iverksettingId = iverksettingId,
            forrigeIverksetting = forrigeIverksetting(behandling, tilkjentYtelse),
        )
        opprettHentStatusFraIverksettingTask(behandling, iverksettingId)
        iverksettClient.iverksett(dto)
    }

    /**
     * Henter aktuelle andeler
     * Oppdaterer aktuelle perioder med status og iverksettingId
     * Returnerer andeler som har beløp=0 då vi fortsatt ønsker å iverksette,
     * men filtrer vekk disse i IverksettDtoMapper, for å eventuell opphøre tidligere perioder
     */
    private fun finnAndelerTilIverksetting(
        tilkjentYtelse: TilkjentYtelse,
        iverksettingId: UUID,
        måned: YearMonth,
    ): List<AndelTilkjentYtelse> {
        val sisteDagenIMåneden = måned.atEndOfMonth()
        val iverksetting = Iverksetting(iverksettingId, LocalDateTime.now())
        val aktuelleAndeler = tilkjentYtelse.andelerTilkjentYtelse
            .filter { it.tom <= sisteDagenIMåneden }
            .map {
                if (it.statusIverksetting == StatusIverksetting.UBEHANDLET) {
                    it.copy(
                        statusIverksetting = StatusIverksetting.SENDT,
                        iverksetting = iverksetting,
                    )
                } else {
                    feilHvis(it.statusIverksetting != StatusIverksetting.OK) {
                        "Kan ikke iverksette behandling=${tilkjentYtelse.behandlingId} iverksetting=$iverksettingId " +
                            "når det finnes tidligere andeler med annen status enn OK/UBEHANDLET"
                    }
                    it
                }
            }
        oppdaterAndeler(aktuelleAndeler, iverksetting)
        return aktuelleAndeler
    }

    /**
     * Oppdaterer andeler som ikke tidligere blitt iverksatte med iverksettingId
     */
    private fun oppdaterAndeler(
        aktuelleAndeler: List<AndelTilkjentYtelse>,
        iverksetting: Iverksetting,
    ) {
        val andelerSomSkalOppdateres = aktuelleAndeler.filter { it.iverksetting == iverksetting }

        feilHvis(andelerSomSkalOppdateres.isEmpty()) {
            "Forventet å oppdatere noen andeler"
        }
        andelTilkjentYtelseRepository.updateAll(andelerSomSkalOppdateres)
    }

    private fun hentTotrinnskontroll(behandlingId: UUID): Totrinnskontroll {
        val totrinnskontroll = totrinnskontrollService.hentTotrinnskontroll(behandlingId)
            ?: error("Finner ikke totrinnskontroll for behandling=$behandlingId")
        feilHvis(totrinnskontroll.status != TotrinnInternStatus.GODKJENT) {
            "Totrinnskontroll må være godkjent for å kunne iverksette"
        }
        return totrinnskontroll
    }

    private fun opprettHentStatusFraIverksettingTask(behandling: Saksbehandling, iverksettingId: UUID) {
        taskService.save(
            HentStatusFraIverksettingTask.opprettTask(
                eksternFagsakId = behandling.eksternFagsakId,
                behandlingId = behandling.id,
                iverksettingId = iverksettingId,
            ),
        )
    }

    /**
     * Finner forrigeIverksetting fra denne eller forrige behandling
     * Når man iverksetter behandling 1 første gang: null
     * Når man iverksetter behandling 1 andre gang: (behandling1, forrige iverksettingId for behandling 1)
     * Når man iverksetter behandling 2 første gang: (behandling1, siste iverksetting for behandling 1)
     * Når man iverksetter behandling 2 andre gang: (behandling2, forrigeIverksettingId for behandling 2)
     */
    private fun forrigeIverksetting(
        behandling: Saksbehandling,
        tilkjentYtelse: TilkjentYtelse,
    ): ForrigeIverksettingDto? {
        return tilkjentYtelse.forrigeIverksetting(behandling.id)
            ?: forrigeIverksettingForrigeBehandling(behandling)
    }

    private fun forrigeIverksettingForrigeBehandling(behandling: Saksbehandling): ForrigeIverksettingDto? {
        val forrigeBehandlingId = behandling.forrigeBehandlingId
        return forrigeBehandlingId?.let {
            tilkjentYtelseService.hentForBehandling(forrigeBehandlingId).forrigeIverksetting(forrigeBehandlingId)
        }
    }

    /**
     * Utleder [ForrigeIverksettingDto] ut fra andel med siste tidspunkt for iverksetting
     */
    private fun TilkjentYtelse.forrigeIverksetting(behandlingId: UUID): ForrigeIverksettingDto? =
        andelerTilkjentYtelse
            .mapNotNull { it.iverksetting }
            .maxByOrNull { it.iverksettingTidspunkt }
            ?.iverksettingId
            ?.let { ForrigeIverksettingDto(behandlingId = behandlingId, iverksettingId = it) }
}
