package no.nav.tilleggsstonader.sak.utbetaling.simulering

import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.BehandlerRolle
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.utbetaling.simulering.kontrakt.OppsummeringForPeriode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SimuleringStegService(
    private val stegService: StegService,
    private val simuleringService: SimuleringService,
    private val tilgangService: TilgangService,
) {

    @Transactional
    fun hentEllerOpprettSimuleringsresultat(saksbehandling: Saksbehandling): List<OppsummeringForPeriode>? {
        if (saksbehandling.status.behandlingErLåstForVidereRedigering() ||
            !tilgangService.harTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER)
        ) {
            return simuleringService.hentLagretSimuleringsoppsummering(saksbehandling.id)
        } else {
            if (saksbehandling.steg == StegType.SIMULERING) {
                stegService.håndterSteg(saksbehandling.id, StegType.SIMULERING)
            }

            return simuleringService.hentLagretSimuleringsoppsummering(saksbehandling.id)
        }
    }
}