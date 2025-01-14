package no.nav.tilleggsstonader.sak.migrering.arena

import no.nav.tilleggsstonader.kontrakter.felles.IdentStønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.migrering.routing.SøknadRoutingService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.identer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Arena kaller på oss for å sjekke om personen finnes i ny løsning.
 * Hvis personen finnes i ny løsning skal man ikke kunne utvide perioder i Arena.
 * De kan fortsatt stanse, og håndtere klager i Arena.
 */
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

        val fagsak = fagsakService.finnFagsak(identer, request.stønadstype)
        if (skalKunneOppretteSakIArenaForPerson(fagsak)) {
            return false
        }
        if (harBehandling(fagsak)) {
            logger.info("Sjekker om person finnes i ny løsning finnes=true harBehandling")
            return true
        }
        if (skalBehandlesITsSak(request.stønadstype)) {
            logger.info("Skal ikke behandle ${request.stønadstype} i Arena")
            return true
        }
        if (harRouting(identer, request.stønadstype)) {
            logger.info("Sjekker om person finnes i ny løsning finnes=true harRouting")
            return true
        }
        return false
    }

    private fun skalKunneOppretteSakIArenaForPerson(fagsak: Fagsak?): Boolean {
        val fagsakId = fagsak?.id
        return fagsakId in setOf(FagsakId.fromString("cae26abb-19c7-41b0-88c1-021d7312b55f"))
    }

    /**
     * Denne håndterer at gitt stønadstype alltid svarer med at personen finnes i ny løsning.
     * Eks for Barnetilsyn er det ønskelig at personen skal håndteres i ny løsning og at det ikke fattes nye vedtak i Arena
     */
    private fun skalBehandlesITsSak(stønadstype: Stønadstype): Boolean = when (stønadstype) {
        Stønadstype.BARNETILSYN -> true
        else -> false
    }

    private fun harBehandling(
        fagsak: Fagsak?,
    ): Boolean {
        return fagsak
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
