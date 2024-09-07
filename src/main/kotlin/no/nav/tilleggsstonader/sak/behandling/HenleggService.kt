package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.dto.HenlagtDto
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class HenleggService(
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
) {

    @Transactional
    fun henleggBehandling(behandlingId: BehandlingId, henlagt: HenlagtDto): Behandling {
        val behandling = behandlingService.henleggBehandling(behandlingId, henlagt)
        ferdigstillOppgaveTask(behandling)
        return behandling
    }

    private fun ferdigstillOppgaveTask(behandling: Behandling) {
        oppgaveService.ferdigstillOppgaveOgsåHvisFeilregistrert(
            behandlingId = behandling.id,
            Oppgavetype.BehandleSak,
        )
        oppgaveService.ferdigstillOppgaveOgsåHvisFeilregistrert(
            behandlingId = behandling.id,
            Oppgavetype.BehandleUnderkjentVedtak,
        )
    }
}
