package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
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
import java.time.LocalDate
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
     * Skal brukes når man iverksetter en behandling første gang, uansett om det er behandling 1 eller behandling 2 på en fagsak
     * Dette fordi man skal iverksette alle andeler tom forrige måned.
     * Dvs denne skal brukes fra [no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.BeslutteVedtakSteg]
     *
     * Ved første iverksetting av en behandling er det krav på at det gjøres med jwt, dvs med saksbehandler-token
     * Neste iverksettinger kan gjøres med client_credentials.
     *
     * Dersom det ikke finnes utbetalinger som skal iverksettes for forrige måned, så legges det til en nullandel
     * for å kunne sjekke status på iverksetting og for å kunne opphøre andeler fra forrige behandling.
     */
    @Transactional
    fun iverksettBehandlingFørsteGang(behandlingId: BehandlingId) {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        if (!behandling.resultat.skalIverksettes) {
            logger.info("Iverksetter ikke behandling=$behandlingId med status=${behandling.status}")
            return
        }
        markerAndelerFraForrigeBehandlingSomUaktuelle(behandling)

        val tilkjentYtelse = tilkjentYtelseService.hentForBehandlingMedLås(behandlingId)
        val andeler = andelerForFørsteIverksettingAvBehandling(tilkjentYtelse)

        val totrinnskontroll = hentTotrinnskontroll(behandlingId)

        val iverksettingId = behandlingId.id
        val dto =
            IverksettDtoMapper.map(
                behandling = behandling,
                andelerTilkjentYtelse = andeler,
                totrinnskontroll = totrinnskontroll,
                iverksettingId = iverksettingId,
                forrigeIverksetting = forrigeIverksetting(behandling, tilkjentYtelse),
            )
        opprettHentStatusFraIverksettingTask(behandling, iverksettingId)
        iverksettClient.iverksett(dto)
    }

    fun hentAndelTilkjentYtelse(behandlingId: BehandlingId) =
        andelTilkjentYtelseRepository.findAndelTilkjentYtelsesByKildeBehandlingId(behandlingId)

    private fun andelerForFørsteIverksettingAvBehandling(tilkjentYtelse: TilkjentYtelse): Collection<AndelTilkjentYtelse> {
        val måned = YearMonth.now()
        val iverksettingId = tilkjentYtelse.behandlingId.id
        val andelerTilIverksetting =
            finnAndelerTilIverksetting(tilkjentYtelse, iverksettingId, utbetalingsdato = LocalDate.now())

        return andelerTilIverksetting.ifEmpty {
            val iverksetting = Iverksetting(iverksettingId, LocalDateTime.now())
            listOf(tilkjentYtelseService.leggTilNullAndel(tilkjentYtelse, iverksetting, måned))
        }
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
    fun iverksett(
        behandlingId: BehandlingId,
        iverksettingId: UUID,
        utbetalingsdato: LocalDate,
    ) {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        if (!behandling.resultat.skalIverksettes) {
            logger.info("Iverksetter ikke behandling=$behandlingId med status=${behandling.status}")
            return
        }
        val tilkjentYtelse = tilkjentYtelseService.hentForBehandlingMedLås(behandlingId)

        val totrinnskontroll = hentTotrinnskontroll(behandlingId)

        val andelerTilkjentYtelse =
            finnAndelerTilIverksetting(tilkjentYtelse, iverksettingId, utbetalingsdato = utbetalingsdato)
        feilHvis(andelerTilkjentYtelse.isEmpty()) {
            "Iverksetting forventer å finne andeler for iverksetting av behandling=$behandlingId utbetalingsdato=$utbetalingsdato"
        }
        val dto =
            IverksettDtoMapper.map(
                behandling = behandling,
                andelerTilkjentYtelse = andelerTilkjentYtelse,
                totrinnskontroll = totrinnskontroll,
                iverksettingId = iverksettingId,
                forrigeIverksetting = forrigeIverksetting(behandling, tilkjentYtelse),
            )
        opprettHentStatusFraIverksettingTask(behandling, iverksettingId)
        iverksettClient.iverksett(dto)
    }

    /**
     * Når man iverksetter en revurdering markeres andeler for forrige behandling som uaktuelle
     * Dette gjøres sånn at de andelene ikke skal plukkes opp og iverksettes,
     * når revurderingen er den nye gjeldende behandlingen
     *
     * Hvis forrige behandling har en andel som har blitt sendt til iverksetting men ikke fått kvittering
     * får man ikke iverksatt revurderingen, man må vente på en OK kvittering
     */
    private fun markerAndelerFraForrigeBehandlingSomUaktuelle(behandling: Saksbehandling) {
        if (behandling.forrigeIverksatteBehandlingId == null) {
            return
        }

        val forrigeBehandling = behandlingService.hentSaksbehandling(behandling.forrigeIverksatteBehandlingId)

        val andelerTilkjentYtelse =
            tilkjentYtelseService.hentForBehandling(forrigeBehandling.id).andelerTilkjentYtelse

        feilHvis(andelerTilkjentYtelse.any { it.statusIverksetting == StatusIverksetting.SENDT }) {
            "Andeler fra forrige behandling er sendt til iverksetting men ikke kvittert OK. Prøv igjen senere."
        }

        val uaktuelleAndeler =
            andelerTilkjentYtelse
                .filter { it.statusIverksetting == StatusIverksetting.UBEHANDLET }
                .map { it.copy(statusIverksetting = StatusIverksetting.UAKTUELL) }

        andelTilkjentYtelseRepository.updateAll(uaktuelleAndeler)
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
        utbetalingsdato: LocalDate,
    ): List<AndelTilkjentYtelse> {
        val iverksetting = Iverksetting(iverksettingId, LocalDateTime.now())
        val aktuelleAndeler =
            tilkjentYtelse.andelerTilkjentYtelse
                .filter { it.utbetalingsdato <= utbetalingsdato }
                .filter { it.statusIverksetting != StatusIverksetting.VENTER_PÅ_SATS_ENDRING }
                .map {
                    if (it.statusIverksetting == StatusIverksetting.UBEHANDLET) {
                        it.copy(
                            statusIverksetting = StatusIverksetting.SENDT,
                            iverksetting = iverksetting,
                        )
                    } else {
                        feilHvisIkke(it.statusIverksetting.erOk()) {
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
        andelTilkjentYtelseRepository.updateAll(andelerSomSkalOppdateres)
    }

    private fun hentTotrinnskontroll(behandlingId: BehandlingId): Totrinnskontroll {
        val totrinnskontroll =
            totrinnskontrollService.hentTotrinnskontroll(behandlingId)
                ?: error("Finner ikke totrinnskontroll for behandling=$behandlingId")
        feilHvis(totrinnskontroll.status != TotrinnInternStatus.GODKJENT) {
            "Totrinnskontroll må være godkjent for å kunne iverksette"
        }
        return totrinnskontroll
    }

    private fun opprettHentStatusFraIverksettingTask(
        behandling: Saksbehandling,
        iverksettingId: UUID,
    ) {
        taskService.save(
            HentStatusFraIverksettingTask.opprettTask(
                eksternFagsakId = behandling.eksternFagsakId,
                behandlingId = behandling.id,
                eksternBehandlingId = behandling.eksternId,
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
    fun forrigeIverksetting(
        behandling: Saksbehandling,
        tilkjentYtelse: TilkjentYtelse,
    ): ForrigeIverksettingDto? =
        tilkjentYtelse.forrigeIverksetting(behandling.id)
            ?: forrigeIverksettingForrigeBehandling(behandling)

    private fun forrigeIverksettingForrigeBehandling(behandling: Saksbehandling): ForrigeIverksettingDto? {
        val forrigeIverksatteBehandlingId = behandling.forrigeIverksatteBehandlingId
        return forrigeIverksatteBehandlingId?.let {
            tilkjentYtelseService.hentForBehandling(forrigeIverksatteBehandlingId).forrigeIverksetting(forrigeIverksatteBehandlingId)
        }
    }

    /**
     * Utleder [ForrigeIverksettingDto] ut fra andel med siste tidspunkt for iverksetting
     */
    private fun TilkjentYtelse.forrigeIverksetting(behandlingId: BehandlingId): ForrigeIverksettingDto? =
        andelerTilkjentYtelse
            .mapNotNull { it.iverksetting }
            .maxByOrNull { it.iverksettingTidspunkt }
            ?.iverksettingId
            ?.let {
                val eksternBehandlingId = behandlingService.hentEksternBehandlingId(behandlingId).id
                ForrigeIverksettingDto(behandlingId = eksternBehandlingId.toString(), iverksettingId = it)
            }
}
