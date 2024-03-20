package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.unleash.UnleashService
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.UnleashUtil.getVariantWithNameOrDefault
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SøknadRoutingService(
    private val søknadRoutingRepository: SøknadRoutingRepository,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val arenaService: ArenaService,
    private val unleashService: UnleashService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun sjekkRoutingForPerson(request: IdentStønadstype): SøknadRoutingResponse {
        return SøknadRoutingResponse(skalBehandlesINyLøsning = skalBehandlesINyLøsning(request))
    }

    fun harLagretRouting(request: IdentStønadstype): Boolean {
        val søknadRouting = søknadRoutingRepository.findByIdentAndType(request.ident, request.stønadstype)
        return søknadRouting != null
    }

    // TODO burde vi sjekke om det finnes oppgave i arena/gosys?
    private fun skalBehandlesINyLøsning(request: IdentStønadstype): Boolean {
        if (harLagretRouting(request)) {
            return true
        }

        val maksAntall = maksAntall(request.stønadstype)
        val antall = søknadRoutingRepository.countByType(request.stønadstype)
        if (antall >= maksAntall) {
            logger.info("skalBehandlesINyLøsning antall=$antall maksAntall=$maksAntall")
            return false
        }

        if (harBehandling(request)) {
            lagreRouting(request, mapOf("harBehandling" to true))
            return true
        }
        val arenaStatus = arenaService.hentStatus(request.ident, request.stønadstype)
        if (harGyldigStateIArena(arenaStatus)) {
            lagreRouting(request, arenaStatus)
            return true
        }
        return false
    }

    private fun maksAntall(stønadstype: Stønadstype) =
        unleashService.getVariantWithNameOrDefault(stønadstype.maksAntallToggle(), "antall", 0)

    private fun Stønadstype.maksAntallToggle() = when (this) {
        Stønadstype.BARNETILSYN -> Toggle.SØKNAD_ROUTING_TILSYN_BARN
    }

    private fun harGyldigStateIArena(arenaStatus: ArenaStatusDto): Boolean {
        return !arenaStatus.sak.harAktivSakUtenVedtak && !arenaStatus.vedtak.harVedtak
    }

    private fun lagreRouting(request: IdentStønadstype, detaljer: Any) {
        søknadRoutingRepository.insert(
            SøknadRouting(
                ident = request.ident,
                type = request.stønadstype,
                detaljer = JsonWrapper(objectMapper.writeValueAsString(detaljer)),
            ),
        )
    }

    private fun harBehandling(
        request: IdentStønadstype,
    ): Boolean = fagsakService.finnFagsak(setOf(request.ident), request.stønadstype)
        ?.let { behandlingService.hentBehandlinger(it.id).isNotEmpty() }
        ?: false
}
