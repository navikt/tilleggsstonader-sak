package no.nav.tilleggsstonader.sak.migrering.arena

import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.migrering.routing.SøknadRoutingService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.identer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ArenaStatusService(
    private val personService: PersonService,
    private val fagsakService: FagsakService,
    private val behandlingService: BehandlingService,
    private val søknadRoutingService: SøknadRoutingService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun finnStatus(request: ArenaFinnesPersonRequest): ArenaFinnesPersonResponse {
        return ArenaFinnesPersonResponse(request.ident, finnesPerson(request))
    }

    private fun finnesPerson(request: ArenaFinnesPersonRequest): Boolean {
        val identer = personService.hentPersonIdenter(request.ident).identer().toSet()
        if (harBehandling(identer, request.stønadstype)) {
            logger.info("Sjekker om person finnes i ny løsning finnes=true harBehandling")
            return true
        }
        if (harRouting(identer, request.stønadstype)) {
            logger.info("Sjekker om person finnes i ny løsning finnes=true harRouting")
            return true
        }
        return false
    }

    private fun harBehandling(
        identer: Set<String>,
        stønadstype: Stønadstype,
    ): Boolean {
        return fagsakService.finnFagsak(identer, stønadstype)
            ?.let { behandlingService.finnesBehandlingForFagsak(it.id) }
            ?: false
    }

    private fun harRouting(
        identer: Set<String>,
        stønadstype: Stønadstype,
    ): Boolean {
        return identer.any {
            søknadRoutingService.harLagretRouting(IdentStønadstype(it, stønadstype))
        }
    }
}
