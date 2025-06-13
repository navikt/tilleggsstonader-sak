package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import java.time.LocalDate

object BehandlingUtil {
    fun utledBehandlingTypeV2(tidligereBehandlinger: List<Behandling>): BehandlingType {
        val skalVæreRevurdering =
            tidligereBehandlinger.any {
                val erFerdigstilt = it.status == BehandlingStatus.FERDIGSTILT
                val ikkeHenlagt = it.resultat != BehandlingResultat.HENLAGT
                erFerdigstilt && ikkeHenlagt
            }
        return if (skalVæreRevurdering) {
            BehandlingType.REVURDERING
        } else {
            BehandlingType.FØRSTEGANGSBEHANDLING
        }
    }

    fun utledBehandlingType(tidligereBehandlinger: List<Behandling>): BehandlingType =
        if (tidligereBehandlinger.any { it.resultat != BehandlingResultat.HENLAGT && it.status != BehandlingStatus.SATT_PÅ_VENT }) {
            BehandlingType.REVURDERING
        } else {
            BehandlingType.FØRSTEGANGSBEHANDLING
        }

    fun skalNullstilleBehandling(
        behandling: Behandling,
        nyRevurderFra: LocalDate?,
    ): Boolean {
        val forrigeRevurdererFra = behandling.revurderFra
        return when {
            nyRevurderFra == null -> false
            forrigeRevurdererFra == null -> true
            else -> nyRevurderFra > forrigeRevurdererFra
        }
    }

    fun validerBehandlingIdErLik(
        behandlingIdParam: BehandlingId,
        behandlingIdRequest: BehandlingId,
    ) = feilHvis(behandlingIdParam != behandlingIdRequest) {
        "BehandlingId er ikke lik param=$behandlingIdParam request=$behandlingIdRequest"
    }

    fun List<Behandling>.sortertEtterVedtakstidspunkt() = this.sortedWith(compareBy(nullsLast()) { it.vedtakstidspunkt })

    fun List<Behandling>.sortertEtterVedtakstidspunktEllerEndretTid() = this.sortedBy { it.vedtakstidspunkt ?: it.sporbar.endret.endretTid }

    fun List<Behandling>.sisteFerdigstilteBehandling() =
        this
            .filter { it.erAvsluttet() }
            .maxByOrNull { it.vedtakstidspunktEllerFeil() }
}
