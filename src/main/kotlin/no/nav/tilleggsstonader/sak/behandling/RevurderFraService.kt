package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.skalNullstilleBehandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.RevurderFra
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class RevurderFraService(
    private val behandlingRepository: BehandlingRepository,
    private val nullstillBehandlingService: NullstillBehandlingService,
) {
    @Transactional
    fun oppdaterRevurderFra(
        behandlingId: BehandlingId,
        revurderFra: RevurderFra?,
    ): Saksbehandling {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)

        feilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke oppdatere revurder fra når behandlingen har status ${behandling.status.visningsnavn()}."
        }

        behandlingRepository.update(behandling.copy(revurderFra = revurderFra))
        if (skalNullstilleBehandling(behandling, revurderFra)) {
            nullstillBehandlingService.nullstillBehandling(behandlingId)
        }
        nullstillBehandlingService.slettVilkårperiodegrunnlag(behandlingId)

        return behandlingRepository.finnSaksbehandling(behandlingId)
    }
}
