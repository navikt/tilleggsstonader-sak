package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import java.util.UUID

object BehandlingUtil {

    fun validerBehandlingIdErLik(behandlingIdParam: UUID, behandlingIdRequest: UUID) =
        feilHvis(behandlingIdParam != behandlingIdRequest) {
            "BehandlingId er ikke lik param=$behandlingIdParam request=$behandlingIdRequest"
        }

    fun List<Behandling>.sortertEtterVedtakstidspunkt() =
        this.sortedWith(compareBy(nullsLast()) { it.vedtakstidspunkt })

    fun List<Behandling>.sortertEtterVedtakstidspunktEllerEndretTid() =
        this.sortedBy { it.vedtakstidspunkt ?: it.sporbar.endret.endretTid }

    fun List<Behandling>.sisteFerdigstilteBehandling() =
        this.filter { it.erAvsluttet() }
            .maxByOrNull { it.vedtakstidspunktEllerFeil() }
}
