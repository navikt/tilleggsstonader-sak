package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ProblemDetailException
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.BehandlerRolle
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.iverksett.IverksettClient
import no.nav.tilleggsstonader.sak.iverksett.tilTilkjentYtelseMedMetaData
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.BeriketSimuleringsresultat
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.SimuleringDto
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.Simuleringsoppsummering
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class SimuleringService(
    private val iverksettClient: IverksettClient,
    private val simuleringsresultatRepository: SimuleringsresultatRepository,
    private val tilkjentYtelseService: TilkjentYtelseService,
    private val tilgangService: TilgangService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun simuler(saksbehandling: Saksbehandling): Simuleringsoppsummering {
        if (saksbehandling.status.behandlingErLåstForVidereRedigering() ||
            !tilgangService.harTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER)
        ) {
            return hentLagretSimuleringsoppsummering(saksbehandling.id)
        }
        val simuleringsresultat = hentOgLagreSimuleringsresultat(saksbehandling)
        return simuleringsresultat.data.oppsummering
    }

    fun hentLagretSimuleringsoppsummering(behandlingId: UUID): Simuleringsoppsummering {
        return hentLagretSimmuleringsresultat(behandlingId).oppsummering
    }

    fun hentLagretSimmuleringsresultat(behandlingId: UUID): BeriketSimuleringsresultat {
        return simuleringsresultatRepository.findByIdOrThrow(behandlingId).data
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

        val beriketSimuleringsresultat = simulerMedTilkjentYtelse(saksbehandling)
        simuleringsresultatRepository.deleteById(saksbehandling.id)
        return simuleringsresultatRepository.insert(
            Simuleringsresultat(
                behandlingId = saksbehandling.id,
                data = beriketSimuleringsresultat,
            ),
        )
    }

    private fun simulerMedTilkjentYtelse(saksbehandling: Saksbehandling): BeriketSimuleringsresultat {
        val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(saksbehandling.id)

        val tilkjentYtelseMedMedtadata =
            tilkjentYtelse.tilTilkjentYtelseMedMetaData(
                saksbehandlerId = SikkerhetContext.hentSaksbehandlerEllerSystembruker(),
                eksternBehandlingId = saksbehandling.eksternId,
                stønadstype = saksbehandling.stønadstype,
                eksternFagsakId = saksbehandling.eksternFagsakId,
                vedtaksdato = LocalDate.now(),
            )

        try {
            return iverksettClient.simuler(
                SimuleringDto(
                    nyTilkjentYtelseMedMetaData = tilkjentYtelseMedMedtadata,
                    forrigeBehandlingId = saksbehandling.forrigeBehandlingId,
                ),
            )
        } catch (e: Exception) {
            val personFinnesIkkeITps = "Personen finnes ikke i TPS"
            brukerfeilHvis(e is ProblemDetailException && e.detail.detail == personFinnesIkkeITps) {
                personFinnesIkkeITps
            }
            throw Feil(
                message = "Kunne ikke utføre simulering",
                frontendFeilmelding = "Kunne ikke utføre simulering. Vennligst prøv på nytt",
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                throwable = e,
            )
        }
    }
}
