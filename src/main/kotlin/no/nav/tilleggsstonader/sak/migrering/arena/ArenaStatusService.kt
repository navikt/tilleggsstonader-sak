package no.nav.tilleggsstonader.sak.migrering.arena

import no.nav.tilleggsstonader.kontrakter.felles.Skjematype
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.tilSkjematype
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.migrering.routing.SkjemaRoutingService
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
    private val skjemaRoutingService: SkjemaRoutingService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun finnStatus(request: ArenaFinnesPersonRequest): ArenaFinnesPersonResponse =
        ArenaFinnesPersonResponse(request.ident, finnesPerson(request))

    private fun finnesPerson(request: ArenaFinnesPersonRequest): Boolean {
        val identer = personService.hentFolkeregisterIdenter(request.ident).identer().toSet()

        val fagsak = fagsakService.finnFagsak(identer, request.stønadstype)
        val eksternFagsakId = fagsak?.eksternId?.id

        logger.info("Sjekker om person skal låses i Arena stønadstype=${request.stønadstype} fagsak=$eksternFagsakId")
        secureLogger.info(
            "Sjekker om person skal låses i Arena stønadstype=${request.stønadstype} ident=${request.ident} fagsak=$eksternFagsakId",
        )

        val logPrefix = "Sjekker om person finnes i ny løsning stønadstype=${request.stønadstype}"
        if (harBehandling(fagsak)) {
            logger.info("$logPrefix finnes=true harRouting harBehandling")
            return true
        }
        if (skalBehandlesITsSak(request.stønadstype)) {
            logger.info("$logPrefix finnes=true skalAlltidBehandlesITsSak")
            return true
        }
        // I routingen skiller vi ikke mellom TSO og TSR for daglig reise, men i starten vil alle routinger på daglig reise bare gjelde TSO.
        // Etter hvert som TSR også slipper gjennom i routingen må vi diskutere hvorvidt vi ønsker å låse en person på både TSO og TSR,
        // eller skille dem fra hverandre.
        val requestGjelderDagligReiseTiltaksenheten = request.stønadstype == Stønadstype.DAGLIG_REISE_TSR
        if (harRouting(identer, request.stønadstype.tilSkjematype()) && !requestGjelderDagligReiseTiltaksenheten) {
            logger.info("$logPrefix finnes=true harRouting")
            return true
        }
        return false
    }

    /**
     * Denne håndterer at gitt stønadstype alltid svarer med at personen finnes i ny løsning.
     * Eks for Barnetilsyn er det ønskelig at personen skal håndteres i ny løsning og at det ikke fattes nye vedtak i Arena
     */
    private fun skalBehandlesITsSak(stønadstype: Stønadstype): Boolean =
        when (stønadstype) {
            Stønadstype.BARNETILSYN -> true
            Stønadstype.LÆREMIDLER -> true
            Stønadstype.BOUTGIFTER -> true
            Stønadstype.DAGLIG_REISE_TSO -> false
            Stønadstype.DAGLIG_REISE_TSR -> false
        }

    private fun harBehandling(fagsak: Fagsak?): Boolean =
        fagsak
            ?.let { behandlingService.finnesBehandlingForFagsak(it.id) } == true

    private fun harRouting(
        identer: Set<String>,
        skjematype: Skjematype,
    ): Boolean =
        identer.any { ident ->
            skjemaRoutingService.harLagretRouting(ident, skjematype)
        }
}
