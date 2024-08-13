package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.tilleggsstonader.kontrakter.felles.Behandlingstema
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnOppgaveRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.StatusEnum
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

@RestController
@RequestMapping("/api/oppgave/admin/mappe")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OppdaterMappePåVåreOppgaverController(
    private val oppgaveClient: OppgaveClient,
    private val oppgaveService: OppgaveService,
    private val oppgaveRepository: OppgaveRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private fun utførEndringSomSystem() {
        SpringTokenValidationContextHolder().setTokenValidationContext(null)
    }

    private val oppgavetyper = setOf(
        Oppgavetype.Journalføring,
        Oppgavetype.BehandleSak,
        Oppgavetype.BehandleUnderkjentVedtak,
        Oppgavetype.GodkjenneVedtak,
    ).map { it.value }.toSet()

    private val statuser = setOf(
        StatusEnum.OPPRETTET,
        StatusEnum.AAPNET,
        StatusEnum.UNDER_BEHANDLING,
    )

    private val enheter = setOf(
        OppgaveUtil.ENHET_NR_NAY,
        OppgaveUtil.ENHET_NR_EGEN_ANSATT,
        OppgaveUtil.ENHET_NR_STRENGT_FORTROLIG,
    )

    /**
     * Finner om det fortsatt finnes noen oppgaver som ikke er flyttet
     */
    @GetMapping("/kontroller")
    fun kontroller(): Map<String, List<Map<String, Any?>>> {
        utførEndringSomSystem()

        return oppgavetyper.associateWith { oppgavetype ->
            val response = oppgaveClient.hentOppgaver(
                FinnOppgaveRequest(
                    tema = Tema.TSO,
                    behandlingstema = Behandlingstema.TilsynBarn,
                    oppgavetype = Oppgavetype.entries.single { it.value == oppgavetype },
                    limit = 1000,
                ),
            )
            response.oppgaver.map {
                mapOf(
                    "id" to it.id,
                    "tildeltEnhetsnr" to it.tildeltEnhetsnr,
                    "mappeId" to it.mappeId,
                )
            }
        }
    }

    /**
     * Skal vi kun oppdatere de oppgaver vi har opprettet? Eller også andra typer som ligger hos andre enheter?
     */
    @PostMapping("/oppdater")
    fun oppdaterOppgaveIder(@RequestBody oppgaveIder: List<Long>): Map<Long, Boolean> {
        utførEndringSomSystem()

        return oppgaveIder
            .map { oppgaveService.hentOppgave(it) }
            .associate { it.id to oppdaterMappe(it) }
    }

    @PostMapping("/oppdater-alle")
    fun oppdaterAlle(): Map<Long, Boolean> {
        utførEndringSomSystem() // Skal patche oppgaver som systembruker

        return oppgaveRepository.finnOppgaverSomIkkeErFerdigstilte().associate { (oppgaveId, behandlingId) ->
            try {
                val oppgave = oppgaveService.hentOppgave(oppgaveId)
                oppgaveId to oppdaterMappe(oppgave)
            } catch (e: Exception) {
                val feilmelding =
                    "Feilet oppdatering av oppgave for behandling=$behandlingId oppgave=$oppgaveId"
                logger.warn(feilmelding)
                secureLogger.warn(feilmelding, e)
                oppgaveId to false
            }
        }
    }

    private fun oppdaterMappe(oppgave: Oppgave): Boolean {
        if (oppgave.status !in statuser) {
            logger.warn("Oppgave=${oppgave.id} har status=${oppgave.status}")
            return false
        }

        val tildeltEnhetsnr = oppgave.tildeltEnhetsnr ?: error("Oppgave=${oppgave.id} mangler tildeltEnhetsnr")
        if (tildeltEnhetsnr !in enheter) {
            logger.warn("Oppgave=${oppgave.id} har enhet=${oppgave.tildeltEnhetsnr}")
            return false
        }

        if (oppgave.tema != Tema.TSO) {
            logger.warn("Oppgave=${oppgave.id} har feil tema=${oppgave.tema}")
            return false
        }

        if (oppgave.mappeId?.getOrNull() != null) {
            logger.warn("Oppgave=${oppgave.id} har mappe id=${oppgave.mappeId}")
            return false
        }

        if (oppgave.oppgavetype !in oppgavetyper) {
            logger.warn("Oppgave=${oppgave.id} har oppgavetype=${oppgave.oppgavetype}")
            return false
        }

        val mappeId = oppgaveService.finnMappe(tildeltEnhetsnr, OppgaveMappe.KLAR).id
        oppgaveService.oppdaterOppgave(
            Oppgave(
                id = oppgave.id,
                versjon = oppgave.versjon,
                mappeId = Optional.of(mappeId),
            ),
        )
        logger.info("Oppdatert oppgave=${oppgave.id} med mappe=$mappeId")
        return true
    }
}
