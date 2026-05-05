package no.nav.tilleggsstonader.sak.utbetaling.iverksetting

import io.micrometer.core.instrument.Metrics
import no.nav.tilleggsstonader.libs.log.logger
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelseRepository
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.Iverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.UtbetalingMessageProducer
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.UtbetalingV3Mapper
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnInternStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

@Service
class IverksettService(
    private val behandlingService: BehandlingService,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val totrinnskontrollService: TotrinnskontrollService,
    private val andelTilkjentYtelseRepository: AndelTilkjentYtelseRepository,
    private val utbetalingMessageProducer: UtbetalingMessageProducer,
    private val utbetalingV3Mapper: UtbetalingV3Mapper,
) {
    private val iverksettingerOverKafkaCounter = Metrics.counter("iverksettinger.til.helved", "type", "kafka")

    /**
     * Iverksetter andeler til og med dagens dato. Utbetalinger frem i tid blir plukket opp av en daglig jobb.
     *
     * Ved fullstendig opphør legges det til en nullandel for å få tilbake status fra økonomi om opphøret
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
            )

        val iverksettingId = behandlingId.id
        sendAndelerTilUtsjekk(
            andelerTilUtbetaling = andelerSomSkalIverksettesNå,
            behandling = behandling,
            iverksettingId = iverksettingId,
            erFørsteIverksettingForBehandling = true,
        )
    }

    private fun andelerForFørsteIverksettingAvBehandling(
        behandling: Saksbehandling,
        tilkjentYtelse: TilkjentYtelse,
    ): Collection<AndelTilkjentYtelse> {
        val måned = YearMonth.now()
        val iverksettingId = tilkjentYtelse.behandlingId.id
        val andelerTilIverksetting =
            finnAndelerTilIverksetting(tilkjentYtelse, iverksettingId, utbetalingsdato = LocalDate.now())

        return if (skalOppretteNullandelForFørsteIverksettingAvBehandling(
                behandling,
                andelerTilIverksetting,
            )
        ) {
            val iverksetting = Iverksetting(iverksettingId, LocalDateTime.now())
            listOf(tilkjentYtelseService.leggTilNullAndel(tilkjentYtelse, iverksetting, måned))
        } else {
            andelerTilIverksetting
        }
    }

    /**
     * Trenger bare nullandel ved opphør av en hel sak for å kunne tracke at økonomi behandler feilutbetalingen
     */
    private fun skalOppretteNullandelForFørsteIverksettingAvBehandling(
        behandling: Saksbehandling,
        andelerTilIverksetting: Collection<AndelTilkjentYtelse>,
    ): Boolean = andelerTilIverksetting.isEmpty() && finnesIverksatteAndelerPåForrigeIverksatteBehandling(behandling)

    private fun finnesIverksatteAndelerPåForrigeIverksatteBehandling(behandling: Saksbehandling): Boolean =
        behandling.forrigeIverksatteBehandlingId != null &&
            tilkjentYtelseService
                .hentForBehandling(behandling.forrigeIverksatteBehandlingId)
                .andelerTilkjentYtelse
                .filterNot { it.erNullandel() }
                .any { it.iverksetting != null }

    /**
     * Kalles på av daglig jobb som plukker opp alle andeler som har utbetalingsdato <= dagens dato.
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

        val andelerTilkjentYtelse =
            finnAndelerTilIverksetting(tilkjentYtelse, iverksettingId, utbetalingsdato = utbetalingsdato)
        feilHvis(andelerTilkjentYtelse.isEmpty()) {
            "Iverksetting forventer å finne andeler for iverksetting av behandling=$behandlingId utbetalingsdato=$utbetalingsdato"
        }

        sendAndelerTilUtsjekk(
            behandling = behandling,
            iverksettingId = iverksettingId,
            andelerTilUtbetaling = andelerTilkjentYtelse,
            erFørsteIverksettingForBehandling = false,
        )
    }

    private fun sendAndelerTilUtsjekk(
        behandling: Saksbehandling,
        iverksettingId: UUID,
        andelerTilUtbetaling: Collection<AndelTilkjentYtelse>,
        erFørsteIverksettingForBehandling: Boolean,
    ) {
        if (andelerTilUtbetaling.isNotEmpty() || finnesIverksatteAndelerPåForrigeIverksatteBehandling(behandling)) {
            iverksettingerOverKafkaCounter.increment()
            val saksbehandlerOgBeslutter = hentSaksbehandlerOgBeslutter(behandling)
            val utbetalingRecords =
                utbetalingV3Mapper.lagIverksettingDtoer(
                    behandling = behandling,
                    andelerTilkjentYtelse = andelerTilUtbetaling.filterNot { it.erNullandel() },
                    saksbehandler = saksbehandlerOgBeslutter.saksbehandler,
                    beslutter = saksbehandlerOgBeslutter.beslutter,
                    erFørsteIverksettingForBehandling = erFørsteIverksettingForBehandling,
                    vedtakstidspunkt = behandling.vedtakstidspunkt ?: feil("Vedtakstidspunkt er påkrevd"),
                )
            utbetalingMessageProducer.sendUtbetalinger(iverksettingId, utbetalingRecords)
        } else {
            logger.info("Ingen andeler å iverksette for behandling=${behandling.id} ved iverksettingId=$iverksettingId")
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

    private fun hentSaksbehandlerOgBeslutter(saksbehandling: Saksbehandling): SaksbehandlerOgBeslutter =
        if (saksbehandling.erSatsendring) {
            SaksbehandlerOgBeslutter(SikkerhetContext.SYSTEM_FORKORTELSE)
        } else if (saksbehandling.erKjørelisteBehandling()) {
            SaksbehandlerOgBeslutter(
                saksbehandler = saksbehandling.endretAv,
                beslutter = SikkerhetContext.SYSTEM_FORKORTELSE,
            )
        } else {
            saksbehandlerOgBeslutterFraTotrinnskontroll(saksbehandling)
        }

    private fun saksbehandlerOgBeslutterFraTotrinnskontroll(saksbehandling: Saksbehandling): SaksbehandlerOgBeslutter {
        val totrinnskontroll =
            totrinnskontrollService.hentTotrinnskontroll(saksbehandling.id)
                ?: error("Finner ikke totrinnskontroll for behandling=${saksbehandling.id}")

        feilHvis(totrinnskontroll.beslutter == null) {
            "Beslutter er ikke satt på totrinnskontroll"
        }
        feilHvis(totrinnskontroll.status != TotrinnInternStatus.GODKJENT) {
            "Totrinnskontroll må være godkjent for å kunne iverksette"
        }
        return SaksbehandlerOgBeslutter(
            saksbehandler = totrinnskontroll.saksbehandler,
            beslutter = totrinnskontroll.beslutter,
        )
    }

    private data class SaksbehandlerOgBeslutter(
        val saksbehandler: String,
        val beslutter: String,
    ) {
        constructor(saksbehandler: String) : this(saksbehandler = saksbehandler, beslutter = saksbehandler)
    }
}
