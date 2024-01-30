package no.nav.tilleggsstonader.sak.migrering.routing

import no.nav.tilleggsstonader.kontrakter.arena.ArenaStatusDto
import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import no.nav.tilleggsstonader.kontrakter.felles.IdenterStønadstype
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider.objectMapper
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.infrastruktur.database.JsonWrapper
import no.nav.tilleggsstonader.sak.opplysninger.arena.ArenaClient
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.identer
import no.nav.tilleggsstonader.sak.util.EnvUtil.erIDev
import org.springframework.stereotype.Service

@Service
class SøknadRoutingService(
    private val søknadRoutingRepository: SøknadRoutingRepository,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val arenaClient: ArenaClient,
    private val personService: PersonService,
) {

    fun sjekkRoutingForPerson(request: IdentStønadstype): SøknadRoutingResponse {
        return SøknadRoutingResponse(skalBehandlesINyLøsning = skalBehandlesINyLøsning(request))
    }

    // TODO burde vi sjekke om det finnes oppgave i arena/gosys?
    private fun skalBehandlesINyLøsning(request: IdentStønadstype): Boolean {
        val søknadRouting = søknadRoutingRepository.findByIdentAndType(request.ident, request.stønadstype)
        if (søknadRouting != null) {
            return true
        }

        val maksAntall = maksAntall(request.stønadstype)
        val antall = søknadRoutingRepository.countByType(request.stønadstype)
        if (antall >= maksAntall) {
            return false
        }

        if (harBehandling(request)) {
            lagreRouting(request, mapOf("harBehandling" to true))
            return true
        }
        val arenaStatus = arenaClient.hentStatus(tilArenaRequest(request))
        if (harGyldigStateIArena(arenaStatus)) {
            lagreRouting(request, arenaStatus)
            return true
        }
        return false
    }

    private fun maksAntall(stønadstype: Stønadstype) = when (stønadstype) {
        Stønadstype.BARNETILSYN -> if (erIDev()) 1000 else 0 // erstatt med unleash
    }

    private fun tilArenaRequest(request: IdentStønadstype) =
        IdenterStønadstype(
            identer = personService.hentPersonIdenter(request.ident).identer(),
            stønadstype = request.stønadstype,
        )

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
