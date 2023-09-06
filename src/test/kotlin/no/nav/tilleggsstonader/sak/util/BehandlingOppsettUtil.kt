package no.nav.tilleggsstonader.sak.util

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus.IVERKSETTER_VEDTAK
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus.UTREDES
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import java.time.LocalDateTime

object BehandlingOppsettUtil {

    private val fagsak = fagsak(setOf(PersonIdent("1")))

    val henlagtFørstegangsbehandling = behandling(fagsak)
        .copy(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            status = BehandlingStatus.FERDIGSTILT,
            resultat = BehandlingResultat.HENLAGT,
            vedtakstidspunkt = SporbarUtils.now(),
            sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusDays(4)),
        )

    val iverksattFørstegangsbehandling = behandling(fagsak)
        .copy(
            type = BehandlingType.FØRSTEGANGSBEHANDLING,
            status = BehandlingStatus.FERDIGSTILT,
            resultat = BehandlingResultat.INNVILGET,
            vedtakstidspunkt = SporbarUtils.now(),
            sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusDays(3)),
        )

    val henlagtRevurdering = behandling(fagsak)
        .copy(
            type = BehandlingType.REVURDERING,
            status = BehandlingStatus.FERDIGSTILT,
            resultat = BehandlingResultat.HENLAGT,
            vedtakstidspunkt = SporbarUtils.now(),
            sporbar = Sporbar(opprettetTid = LocalDateTime.now().minusDays(1)),
        )

    private val revurderingUnderArbeid = behandling(fagsak)
        .copy(
            type = BehandlingType.REVURDERING,
            status = IVERKSETTER_VEDTAK,
            resultat = BehandlingResultat.INNVILGET,
            vedtakstidspunkt = SporbarUtils.now(),
        )

    val iverksattRevurdering = behandling(fagsak)
        .copy(
            type = BehandlingType.REVURDERING,
            status = BehandlingStatus.FERDIGSTILT,
            resultat = BehandlingResultat.INNVILGET,
            vedtakstidspunkt = SporbarUtils.now(),
        )

    val revurdering = behandling(fagsak)
        .copy(
            type = BehandlingType.REVURDERING,
            status = UTREDES,
            resultat = BehandlingResultat.IKKE_SATT,
        )

    fun lagBehandlingerForSisteIverksatte() = listOf(
        henlagtFørstegangsbehandling,
        iverksattFørstegangsbehandling,
        henlagtRevurdering,
        revurderingUnderArbeid,
    )
}
