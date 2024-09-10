package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettClient
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettDtoMapper
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringRequestDto
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringResponseDto
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class SimuleringService(
    private val iverksettClient: IverksettClient,
    private val simuleringsresultatRepository: SimuleringsresultatRepository,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val tilgangService: TilgangService,
    private val iverksettService: IverksettService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun hentLagretSimulering(behandlingId: UUID): Simuleringsresultat? {
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

        simuleringsresultatRepository.deleteById(saksbehandling.id)
        return simuleringsresultatRepository.insert(
            Simuleringsresultat(
                behandlingId = saksbehandling.id,
                data = SimuleringResponseMapper.map(resultat),
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
