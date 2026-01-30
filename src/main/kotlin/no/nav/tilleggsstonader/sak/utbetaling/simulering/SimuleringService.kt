package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.SimuleringClient
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.Simuleringsresultat
import no.nav.tilleggsstonader.sak.utbetaling.simulering.domain.SimuleringsresultatRepository
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringResponseDto
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.utbetaling.utsjekk.utbetaling.UtbetalingV3Mapper
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SimuleringService(
    private val simuleringClient: SimuleringClient,
    private val simuleringsresultatRepository: SimuleringsresultatRepository,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val tilgangService: TilgangService,
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
                ingenEndringIUtbetaling = resultat == null || resultat.oppsummeringer.isEmpty(),
            ),
        )
    }

    private fun simulerMedTilkjentYtelse(saksbehandling: Saksbehandling): SimuleringResponseDto? {
        val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(saksbehandling.id)

        return simuleringClient.simuler(
            utbetalingV3Mapper.lagSimuleringDtoer(
                saksbehandling,
                tilkjentYtelse.andelerTilkjentYtelse,
            ),
        )
    }
}
