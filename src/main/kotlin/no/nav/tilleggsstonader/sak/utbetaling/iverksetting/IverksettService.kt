package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.gjelderDagligReise
import no.nav.tilleggsstonader.libs.log.logger
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.utbetaling.id.FagsakUtbetalingId
import no.nav.tilleggsstonader.sak.utbetaling.id.FagsakUtbetalingIdService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Iverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.UtbetalingMessageProducer
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.UtbetalingV3Mapper
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnInternStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.Totrinnskontroll
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
    private val utbetalingMessageProducer: UtbetalingMessageProducer,
    private val utbetalingV3Mapper: UtbetalingV3Mapper,
    private val fagsakUtbetalingIdService: FagsakUtbetalingIdService,
    private val unleashService: UnleashService,
) {
    /**
     * Iverksetter andeler til og med dagens dato. Utbetalinger frem i tid blir plukket opp av en daglig jobb.
     *
     * Notat om synkron iverksetting (aka v2 av utsjekk):
     * Ved første iverksetting av en behandling er det krav på at det gjøres med jwt, dvs med saksbehandler-token
     * Neste iverksettinger kan gjøres med client_credentials. Dersom det ikke finnes utbetalinger som skal iverksettes for forrige måned,
     * så legges det til en nullandel for å kunne sjekke status på iverksetting og for å kunne opphøre andeler fra forrige behandling.
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
        val andelerSomSkalIverksettesNå =
            andelerForFørsteIverksettingAvBehandling(
                behandling,
                tilkjentYtelse,
                utbetalingSkalSendesPåKafka(
                    behandling,
                    fagsakId = behandling.fagsakId,
                    typeAndel = tilkjentYtelse.andelerTilkjentYtelse.map { it.type }.toSet(),
                ),
            )

        val totrinnskontroll = hentTotrinnskontroll(behandling)

        val iverksettingId = behandlingId.id
        sendAndelerTilUtsjekk(
            tilkjentYtelse = tilkjentYtelse,
            andelerTilUtbetaling = andelerSomSkalIverksettesNå,
            behandling = behandling,
            iverksettingId = iverksettingId,
            totrinnskontroll = totrinnskontroll,
            erFørsteIverksettingForBehandling = true,
        )
    }

    fun hentAndelTilkjentYtelse(behandlingId: BehandlingId) =
        andelTilkjentYtelseRepository.findAndelTilkjentYtelsesByKildeBehandlingId(behandlingId)

    private fun andelerForFørsteIverksettingAvBehandling(
        behandling: Saksbehandling,
        tilkjentYtelse: TilkjentYtelse,
        skalSendesPåKafka: Boolean,
    ): Collection<AndelTilkjentYtelse> {
        val måned = YearMonth.now()
        val iverksettingId = tilkjentYtelse.behandlingId.id
        val andelerTilIverksetting =
            finnAndelerTilIverksetting(tilkjentYtelse, iverksettingId, utbetalingsdato = LocalDate.now())

        return if (skalOppretteNullandelForFørsteIverksettingAvBehandling(behandling, skalSendesPåKafka, andelerTilIverksetting)) {
            val iverksetting = Iverksetting(iverksettingId, LocalDateTime.now())
            listOf(tilkjentYtelseService.leggTilNullAndel(tilkjentYtelse, iverksetting, måned))
        } else {
            andelerTilIverksetting
        }
    }

    /**
     * Trenger ikke nullandel ved første iverksetting av førstegangsbehandling om det sendes på kafka,
     * men trenger nullandel over kafka ved opphør av en hel sak for å kunne tracke at økonomi behandler feilutbetalingen
     */
    private fun skalOppretteNullandelForFørsteIverksettingAvBehandling(
        behandling: Saksbehandling,
        skalSendesPåKafka: Boolean,
        andelerTilIverksetting: Collection<AndelTilkjentYtelse>,
    ): Boolean =
        when {
            andelerTilIverksetting.isNotEmpty() -> false
            !skalSendesPåKafka -> true
            behandling.forrigeIverksatteBehandlingId == null -> false
            else -> true
        }

    /**
     * Kalles på av daglig jobb som plukker opp alle andeler som har utbetalingsdato <= dagens dato.
     *
     * Notat om synkron iverksetting (aka v2 av utsjekk):
     * Hvis kallet feiler er det viktig at den samme iverksettingId brukes for å kunne ignorere conflict
     * Ved første iverksetting som gjøres brukes behandlingId for å iverksette
     * for å enkelt kunne gjenbruke samme id vid neste iverksetting.
     *
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

        val totrinnskontroll = hentTotrinnskontroll(behandling)

        val andelerTilkjentYtelse =
            finnAndelerTilIverksetting(tilkjentYtelse, iverksettingId, utbetalingsdato = utbetalingsdato)
        feilHvis(andelerTilkjentYtelse.isEmpty()) {
            "Iverksetting forventer å finne andeler for iverksetting av behandling=$behandlingId utbetalingsdato=$utbetalingsdato"
        }

        sendAndelerTilUtsjekk(
            behandling = behandling,
            iverksettingId = iverksettingId,
            andelerTilUtbetaling = andelerTilkjentYtelse,
            totrinnskontroll = totrinnskontroll,
            tilkjentYtelse = tilkjentYtelse,
            erFørsteIverksettingForBehandling = false,
        )
    }

    private fun sendAndelerTilUtsjekk(
        behandling: Saksbehandling,
        iverksettingId: UUID,
        andelerTilUtbetaling: Collection<AndelTilkjentYtelse>,
        totrinnskontroll: Totrinnskontroll,
        tilkjentYtelse: TilkjentYtelse,
        erFørsteIverksettingForBehandling: Boolean,
    ) {
        if (utbetalingSkalSendesPåKafka(
                behandling = behandling,
                fagsakId = behandling.fagsakId,
                typeAndel = tilkjentYtelse.andelerTilkjentYtelse.map { it.type }.toSet(),
            )
        ) {
            val utbetalingsIderPåFagsak =
                fagsakUtbetalingIdService.hentUtbetalingIderForFagsakId(fagsakId = behandling.fagsakId)
            if (andelerTilUtbetaling.isNotEmpty() || utbetalingsIderPåFagsak.isNotEmpty()) {
                val utbetalingRecords =
                    utbetalingV3Mapper.lagIverksettingDtoer(
                        behandling = behandling,
                        andelerTilkjentYtelse = andelerTilUtbetaling.filterNot { it.erNullandel() },
                        totrinnskontroll = totrinnskontroll,
                        erFørsteIverksettingForBehandling = erFørsteIverksettingForBehandling,
                        vedtakstidspunkt = behandling.vedtakstidspunkt ?: feil("Vedtakstidspunkt er påkrevd"),
                    )
                utbetalingMessageProducer.sendUtbetalinger(iverksettingId, utbetalingRecords)
            } else {
                logger.info("Ingen andeler å iverksette for behandling=${behandling.id} ved iverksettingId=$iverksettingId")
            }
        } else {
            val dto =
                IverksettDtoMapper.map(
                    behandling = behandling,
                    andelerTilkjentYtelse = andelerTilUtbetaling,
                    totrinnskontroll = totrinnskontroll,
                    iverksettingId = iverksettingId,
                    forrigeIverksetting = finnForrigeIverksetting(behandling, tilkjentYtelse),
                )
            opprettHentStatusFraIverksettingTask(behandling, iverksettingId)
            iverksettClient.iverksett(dto)
        }
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
                .filter {
                    it.statusIverksetting == StatusIverksetting.UBEHANDLET ||
                        it.statusIverksetting == StatusIverksetting.VENTER_PÅ_SATS_ENDRING
                }.map { it.copy(statusIverksetting = StatusIverksetting.UAKTUELL) }

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

    private fun hentTotrinnskontroll(saksbehandling: Saksbehandling): Totrinnskontroll {
        val totrinnskontroll =
            if (saksbehandling.erSatsendring) {
                Totrinnskontroll.maskineltBesluttet(saksbehandling.id)
            } else {
                totrinnskontrollService.hentTotrinnskontroll(saksbehandling.id)
                    ?: error("Finner ikke totrinnskontroll for behandling=${saksbehandling.id}")
            }

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
    fun finnForrigeIverksetting(
        behandling: Saksbehandling,
        tilkjentYtelse: TilkjentYtelse,
    ): ForrigeIverksettingDto? =
        tilkjentYtelse.finnForrigeIverksetting(behandling.id)
            ?: forrigeIverksettingForrigeBehandling(behandling)

    private fun forrigeIverksettingForrigeBehandling(behandling: Saksbehandling): ForrigeIverksettingDto? {
        val forrigeIverksatteBehandlingId = behandling.forrigeIverksatteBehandlingId
        return forrigeIverksatteBehandlingId?.let {
            tilkjentYtelseService
                .hentForBehandling(forrigeIverksatteBehandlingId)
                .finnForrigeIverksetting(forrigeIverksatteBehandlingId)
        }
    }

    /**
     * Utleder [ForrigeIverksettingDto] ut fra andel med siste tidspunkt for iverksetting
     */
    private fun TilkjentYtelse.finnForrigeIverksetting(behandlingId: BehandlingId): ForrigeIverksettingDto? =
        andelerTilkjentYtelse
            .mapNotNull { it.iverksetting }
            .maxByOrNull { it.iverksettingTidspunkt }
            ?.iverksettingId
            ?.let {
                val eksternBehandlingId = behandlingService.hentEksternBehandlingId(behandlingId).id
                ForrigeIverksettingDto(behandlingId = eksternBehandlingId.toString(), iverksettingId = it)
            }

    fun utbetalingSkalSendesPåKafka(
        behandling: Saksbehandling,
        fagsakId: FagsakId,
        typeAndel: Set<TypeAndel>,
    ): Boolean {
        val utbetalingIderPåFagsak = fagsakUtbetalingIdService.hentUtbetalingIderForFagsakId(fagsakId)
        val finnesUtbetalingListe = typeAndel.map { fagsakUtbetalingIdService.finnesUtbetalingsId(fagsakId, it) }

        feilHvis(finnesUtbetalingListe.distinct().size > 1) {
            "Kun noen av andelene på behandlingen er migrert fra REST til Kafka"
        }

        return behandling.stønadstype.gjelderDagligReise() ||
            erFørstegangsbehandlingLæremidlerOgSkalIverksetteMotKafka(behandling) ||
            (
                utbetalingIderPåFagsak.isNotEmpty() &&
                    finnesUtbetalingIdForAlleTypeAndeler(
                        typeAndelIBehandling = typeAndel,
                        utbetalingIderPåFagsak = utbetalingIderPåFagsak,
                    )
            )
    }

    private fun erFørstegangsbehandlingLæremidlerOgSkalIverksetteMotKafka(behandling: Saksbehandling): Boolean =
        behandling.stønadstype == Stønadstype.LÆREMIDLER &&
            behandling.forrigeIverksatteBehandlingId == null &&
            unleashService.isEnabled(Toggle.SKAL_IVERKSETT_NYE_BEHANDLINGER_MOT_KAFKA)

    private fun finnesUtbetalingIdForAlleTypeAndeler(
        typeAndelIBehandling: Set<TypeAndel>,
        utbetalingIderPåFagsak: List<FagsakUtbetalingId>,
    ): Boolean =
        typeAndelIBehandling.all { typeAndel ->
            utbetalingIderPåFagsak.any { utbetalingId -> utbetalingId.typeAndel == typeAndel }
        }
}
