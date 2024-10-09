package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettClient
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettDtoMapper
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Simuleringsresultat
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringsresultatRepository
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringRequestDto
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringResponseDto
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.EnvUtil
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SimuleringService(
    private val iverksettClient: IverksettClient,
    private val simuleringsresultatRepository: SimuleringsresultatRepository,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val tilgangService: TilgangService,
    private val iverksettService: IverksettService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentLagretSimulering(behandlingId: BehandlingId): Simuleringsresultat? {
        return simuleringsresultatRepository.findByIdOrNull(behandlingId)
    }

    fun slettSimuleringForBehandling(saksbehandling: Saksbehandling) {
        val behandlingId = saksbehandling.id
        feilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke slette simulering for behandling=$behandlingId då den er låst"
        }
        logger.info("Sletter simulering for behandling=$behandlingId")
        simuleringsresultatRepository.deleteById(behandlingId)
    }

    @Transactional
    fun hentOgLagreSimuleringsresultat(saksbehandling: Saksbehandling): Simuleringsresultat {
        tilgangService.validerHarSaksbehandlerrolle()

        feilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke hente og lagre simuleringsresultat då behandling=${saksbehandling.id} er låst"
        }

        val resultat = simulerMedTilkjentYtelse(saksbehandling)
        feilHvis(EnvUtil.erIProd() && resultat?.oppsummeringer?.any { it.totalFeilutbetaling > 0 } ?: false) {
            /**
             * Kaster foreløpig bare en feil her, då feiler dette steget men vedtaket er lagret.
             * Økonomi har ennå ikke satt opp tilbakekrevingsløypa i prod ennå
             */
            "Vedtak gir tilbakekreving, vi har foreløpig ikke støtte for iverksettinger som gir tilbakekreving"
        }

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
        val forrigeIverksettingDto = iverksettService.forrigeIverksetting(saksbehandling, tilkjentYtelse)

        return iverksettClient.simuler(
            SimuleringRequestDto(
                sakId = saksbehandling.eksternFagsakId.toString(),
                behandlingId = saksbehandling.eksternId.toString(),
                personident = saksbehandling.ident,
                saksbehandlerId = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
                utbetalinger = IverksettDtoMapper.mapUtbetalinger(tilkjentYtelse.andelerTilkjentYtelse),
                forrigeIverksetting = forrigeIverksettingDto,
            ),
        )
    }
}
