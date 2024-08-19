package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SøknadRoutingService(
    private val søknadRoutingRepository: SøknadRoutingRepository,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val arenaService: ArenaService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun sjekkRoutingForPerson(request: IdentStønadstype): SøknadRoutingResponse {
        val skalBehandlesINyLøsning = skalBehandlesINyLøsning(request)
        logger.info("routing - skalBehandlesINyLøsning=$skalBehandlesINyLøsning")
        return SøknadRoutingResponse(skalBehandlesINyLøsning = skalBehandlesINyLøsning)
    }

    fun harLagretRouting(request: IdentStønadstype): Boolean {
        val søknadRouting = søknadRoutingRepository.findByIdentAndType(request.ident, request.stønadstype)
        return søknadRouting != null
    }

    // TODO burde vi sjekke om det finnes oppgave i arena/gosys?
    private fun skalBehandlesINyLøsning(request: IdentStønadstype): Boolean {
        if (harLagretRouting(request)) {
            logger.info("routing - harLagretRouting=true")
            return true
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

    private fun harGyldigStateIArena(arenaStatus: ArenaStatusDto): Boolean {
        val harAktivtVedtak = arenaStatus.vedtak.harAktivtVedtak
        val harVedtakUtenUtfall = arenaStatus.vedtak.harVedtakUtenUtfall
        val harGyldigStatus = !harAktivtVedtak

        val harAktivSakUtenVedtak = arenaStatus.sak.harAktivSakUtenVedtak
        val harVedtak = arenaStatus.vedtak.harVedtak
        logger.info("routing - harGyldigStatusArena=$harGyldigStatus - harAktivSakUtenVedtak=$harAktivSakUtenVedtak harVedtak=$harVedtak harAktivtVedtak=$harAktivtVedtak harVedtakUtenUtfall=$harVedtakUtenUtfall")
        return harGyldigStatus
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
    ): Boolean {
        val harBehandling = (
            fagsakService.finnFagsak(setOf(request.ident), request.stønadstype)
                ?.let { behandlingService.hentBehandlinger(it.id).isNotEmpty() }
                ?: false
            )
        logger.info("routing - harBehandling=$harBehandling")
        return harBehandling
    }
}
