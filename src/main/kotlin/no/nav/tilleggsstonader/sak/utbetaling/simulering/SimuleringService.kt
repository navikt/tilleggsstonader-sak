package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.ForrigeIverksettingDto
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettClient
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettDtoMapper
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.utbetalingSkalSendesPåKafka
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Simuleringsresultat
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringsresultatRepository
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringRequestDto
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringResponseDto
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.UtbetalingV3Mapper
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SimuleringService(
    private val iverksettClient: IverksettClient,
    private val simuleringsresultatRepository: SimuleringsresultatRepository,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val tilgangService: TilgangService,
    private val iverksettService: IverksettService,
    private val utbetalingV3Mapper: UtbetalingV3Mapper,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentLagretSimulering(behandlingId: BehandlingId): Simuleringsresultat? = simuleringsresultatRepository.findByIdOrNull(behandlingId)

    fun slettSimuleringForBehandling(saksbehandling: Saksbehandling) {
        val behandlingId = saksbehandling.id
        feilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke slette simulering for behandling=$behandlingId fordi den har har status ${saksbehandling.status.visningsnavn()}."
        }
        logger.info("Sletter simulering for behandling=$behandlingId")
        simuleringsresultatRepository.deleteById(behandlingId)
    }

    @Transactional
    fun hentOgLagreSimuleringsresultat(saksbehandling: Saksbehandling): Simuleringsresultat {
        tilgangService.validerHarSaksbehandlerrolle()

        feilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke hente og lagre simuleringsresultat for behandling=${saksbehandling.id} fordi den har har status ${saksbehandling.status.visningsnavn()}."
        }

        val resultat = simulerMedTilkjentYtelse(saksbehandling)

        simuleringsresultatRepository.deleteById(saksbehandling.id)
        return simuleringsresultatRepository.insert(
            Simuleringsresultat(
                behandlingId = saksbehandling.id,
                data = resultat?.let { SimuleringKontraktTilDomeneMapper.map(it) },
                ingenEndringIUtbetaling = resultat == null,
            ),
        )
    }

    private fun simulerMedTilkjentYtelse(saksbehandling: Saksbehandling): SimuleringResponseDto? {
        val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(saksbehandling.id)
        val forrigeIverksetting = iverksettService.finnForrigeIverksetting(saksbehandling, tilkjentYtelse)

        if (utbetalingSkalSendesPåKafka(saksbehandling.stønadstype)) {
            return iverksettClient.simulerV3(
                utbetalingV3Mapper.lagUtbetalingRecords(
                    behandling = saksbehandling,
                    andelerTilkjentYtelse = tilkjentYtelse.andelerTilkjentYtelse,
                    totrinnskontroll = null,
                    erSimulering = true,
                    vedtakstidspunkt = LocalDateTime.now(),
                    erFørsteIverksettingForBehandling = true,
                ),
            )
        } else {
            return iverksettClient.simulerV2(
                SimuleringRequestDto(
                    sakId = saksbehandling.eksternFagsakId.toString(),
                    behandlingId = saksbehandling.eksternId.toString(),
                    personident = saksbehandling.ident,
                    saksbehandlerId = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
                    utbetalinger = IverksettDtoMapper.mapUtbetalinger(tilkjentYtelse.andelerTilkjentYtelse),
                    forrigeIverksetting = forrigeIverksetting?.let { ForrigeIverksettingDto(it.behandlingId, it.iverksettingId) },
                ),
            )
        }
    }
}
