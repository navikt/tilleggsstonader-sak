package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.BehandlerRolle
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettClient
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettDtoMapper
import no.nav.tilleggsstonader.sak.utbetaling.iverksetting.IverksettService
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.OppsummeringForPeriode
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringRequestDto
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringResponseDto
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
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

    @Transactional
    fun simuler(saksbehandling: Saksbehandling): List<OppsummeringForPeriode>? {
        if (saksbehandling.status.behandlingErLåstForVidereRedigering() ||
            !tilgangService.harTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER)
        ) {
            return hentLagretSimuleringsoppsummering(saksbehandling.id)
        }
        val simuleringsresultat = hentOgLagreSimuleringsresultat(saksbehandling)
        return simuleringsresultat.data.oppsummeringer
    }

    fun hentLagretSimuleringsoppsummering(behandlingId: UUID): List<OppsummeringForPeriode>? {
        return hentLagretSimmuleringsresultat(behandlingId)?.oppsummeringer
    }

    fun hentLagretSimmuleringsresultat(behandlingId: UUID): SimuleringResponse? {
        return simuleringsresultatRepository.findByIdOrNull(behandlingId)?.data
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
            ),
        )
    }

    private fun simulerMedTilkjentYtelse(saksbehandling: Saksbehandling): SimuleringResponseDto {
        val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(saksbehandling.id)
        val forrigeIverksettingDto = iverksettService.forrigeIverksetting(saksbehandling, tilkjentYtelse)

        return iverksettClient.simuler(
            SimuleringRequestDto(
                sakId = saksbehandling.eksternFagsakId.toString(),
                behandlingId = saksbehandling.eksternId.toString(),
                personident = saksbehandling.ident,
                saksbehandler = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
                vedtakstidspunkt = LocalDateTime.now(),
                utbetalinger = IverksettDtoMapper.mapUtbetalinger(tilkjentYtelse.andelerTilkjentYtelse),
                forrigeIverksetting = forrigeIverksettingDto,
            ),
        )
    }
}
