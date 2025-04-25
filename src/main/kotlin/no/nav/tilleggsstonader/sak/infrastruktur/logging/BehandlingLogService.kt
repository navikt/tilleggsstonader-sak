package no.nav.tilleggsstonader.sak.infrastruktur.logging

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.spring.cache.getValue
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import org.slf4j.MDC
import org.springframework.cache.CacheManager
import org.springframework.stereotype.Service

@Service
class BehandlingLogService(
    private val behandlingService: BehandlingService,
    private val cacheManager: CacheManager,
) {
    fun settBehandlingsdetaljerForRequest(behandlingId: BehandlingId) {
        val behandlingsinformasjon = hentBehandlingsinformasjon(behandlingId)
        MDC.put(TypeBehandlingLogging.BEHANDLING_ID.key, behandlingsinformasjon.behandlingId.toString())
        MDC.put(TypeBehandlingLogging.FAGSAK_ID.key, behandlingsinformasjon.fagsakId.toString())
        MDC.put(TypeBehandlingLogging.STØNADSTYPE.key, behandlingsinformasjon.stønadstype.name)
    }

    private fun hentBehandlingsinformasjon(behandlingId: BehandlingId): Behandlingsinformasjon =
        cacheManager.getValue("request_logging_behandlingsinfo", behandlingId, {
            val behandling = behandlingService.hentSaksbehandling(behandlingId)
            Behandlingsinformasjon(behandling)
        })
}

private data class Behandlingsinformasjon(
    val fagsakId: FagsakId,
    val behandlingId: BehandlingId,
    val stønadstype: Stønadstype,
) {
    constructor(behandling: Saksbehandling) : this(
        fagsakId = behandling.fagsakId,
        behandlingId = behandling.id,
        stønadstype = behandling.stønadstype,
    )
}
