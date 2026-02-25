package no.nav.tilleggsstonader.sak.migrering.arena

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakId
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
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

        if (fagsak != null && harBehandlingSomIkkeErHenlagt(fagsak)) {
            logger.info("$logPrefix finnes=true harBehandling")

            if (fagsakHarUnntakOgKanBehandlesIArena(fagsak.eksternId)) {
                logger.info("$logPrefix finnes=true harUnntak")
                return false
            }

            return true
        }

        if (skalBehandlesITsSak(request.stønadstype)) {
            logger.info("$logPrefix finnes=true skalAlltidBehandlesITsSak")
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

    private fun harBehandlingSomIkkeErHenlagt(fagsak: Fagsak): Boolean {
        val behandlinger = behandlingService.hentBehandlinger(fagsak.id)

        return behandlinger.any { it.resultat != BehandlingResultat.HENLAGT }
    }

    private fun fagsakHarUnntakOgKanBehandlesIArena(eksternFagsakId: EksternFagsakId): Boolean =
        saksnummerSomKanBehandlesIArena.contains(eksternFagsakId.id)

    /**
     * Liste med saksnummere som har fått unntak slik at de kan behandles i arena
     */
    private val saksnummerSomKanBehandlesIArena: List<Long> =
        listOf(
            9807,
        )
}
