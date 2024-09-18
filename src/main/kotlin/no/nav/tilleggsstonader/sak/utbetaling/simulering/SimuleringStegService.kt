package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.BehandlerRolle
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SimuleringStegService(
    private val stegService: StegService,
    private val simuleringService: SimuleringService,
    private val tilgangService: TilgangService,
) {

    @Transactional
    fun hentEllerOpprettSimuleringsresultat(saksbehandling: Saksbehandling): SimuleringDto? {
        if (saksbehandling.status.behandlingErLåstForVidereRedigering() ||
            !tilgangService.harTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER)
        ) {
            return simuleringService.hentLagretSimulering(saksbehandling.id)?.tilDto()
        } else {
            if (saksbehandling.steg == StegType.SIMULERING) {
                stegService.håndterSteg(saksbehandling.id, StegType.SIMULERING)
            }

            return simuleringService.hentLagretSimulering(saksbehandling.id)?.tilDto()
        }
    }

    private fun Simuleringsresultat.tilDto(): SimuleringDto {
        return SimuleringDto(
            perioder = this.data?.oppsummeringer,
            ingenEndringIUtbetaling = this.ingenEndringIUtbetaling,
            oppsummering = lagSimuleringOppsummering(this),
        )
    }

    private fun lagSimuleringOppsummering(simulering: Simuleringsresultat): SimuleringOppsummering? {
        if (simulering.data == null) {
            return null
        }

        return SimuleringOppsummering(
            fom = simulering.data.oppsummeringer.minOf { it.fom },
            tom = simulering.data.oppsummeringer.maxOf { it.tom },
            etterbetaling = simulering.data.oppsummeringer.sumOf { it.totalEtterbetaling },
            feilutbetaling = simulering.data.oppsummeringer.sumOf { it.totalFeilutbetaling },
        )
    }
}
