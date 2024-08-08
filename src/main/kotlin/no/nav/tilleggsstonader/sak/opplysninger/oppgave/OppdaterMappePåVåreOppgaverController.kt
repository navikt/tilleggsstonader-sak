package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.tilleggsstonader.kontrakter.felles.Behandlingstema
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.StatusEnum
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto.FinnOppgaveRequestDto
import org.slf4j.LoggerFactory
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

@RestController
@RequestMapping("/api/oppgave/admin/mappe")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OppdaterMappePåVåreOppgaverController(
    private val oppgaveClient: OppgaveClient,
    private val oppgaveService: OppgaveService,
    private val behandlingRepository: BehandlingRepository,
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

    @GetMapping("/kontroller")
    fun kontroller(): Map<String, Any> {
        utførEndringSomSystem()

        val oppgaver = oppgaveClient.hentOppgaver(
            FinnOppgaveRequestDto(
                behandlingstema = Behandlingstema.TilsynBarn.name,
                ident = null,
                oppgaverPåVent = false,
                limit = 1000,
            ).tilFinnOppgaveRequest(null, oppgaveService.finnVentemappe()),
        )
            .oppgaver
            .filter { oppgave -> oppgave.oppgavetype !in oppgavetyper }

        val oppgaveInfo = oppgaver
            .filter { it.tildeltEnhetsnr != "4462" }
            .groupBy { it.oppgavetype }
            .mapValues { it.value.groupBy { it.tildeltEnhetsnr }.mapValues { it.value.size } }
        return mapOf(
            "oppgaver" to oppgaveInfo,
        )
    }

    /**
     * Finner om det fortsatt finnes noen oppgaver som ikke er flyttet
     */
    @GetMapping("/kontroller-flytting")
    fun kontrollerFlytting(): Map<String, List<Map<String, Any?>>> {
        utførEndringSomSystem()

        return oppgavetyper.associateWith { oppgavetype ->
            val response = oppgaveClient.hentOppgaver(
                FinnOppgaveRequestDto(
                    behandlingstema = Behandlingstema.TilsynBarn.name,
                    ident = null,
                    oppgaverPåVent = false,
                    oppgavetype = oppgavetype,
                    limit = 1000,
                ).tilFinnOppgaveRequest(null, oppgaveService.finnVentemappe()),
            )
            response.oppgaver.map {
                mapOf<String, Any?>(
                    "id" to it.id,
                    "tildeltEnhetsnr" to it.tildeltEnhetsnr,
                    "tema" to it.tema,
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
        utførEndringSomSystem()

        return behandlingRepository.hentUferdigeBehandlingerOpprettetFørDato(
            Stønadstype.BARNETILSYN,
            LocalDateTime.now(),
        )
            .mapNotNull {
                val oppgaveDomain = oppgaveService.finnSisteOppgaveForBehandling(it.id)
                if (oppgaveDomain == null) {
                    logger.warn("Finner ikke oppgave for behandling=${it.id}")
                }
                oppgaveDomain
            }.associate {
                try {
                    val oppgave = oppgaveService.hentOppgave(it.gsakOppgaveId)
                    it.gsakOppgaveId to oppdaterMappe(oppgave)
                } catch (e: Exception) {
                    logger.warn("Feilet henting av oppgave for behandling=${it.behandlingId} oppgave=${it.gsakOppgaveId}")
                    it.gsakOppgaveId to false
                }
            }
    }

    private fun oppdaterMappe(oppgave: Oppgave): Boolean {
        if (oppgave.status !in statuser) {
            logger.warn("Oppgave=${oppgave.id} har status=${oppgave.status}")
            return false
        }

        val tildeltEnhetsnr = oppgave.tildeltEnhetsnr
        if (tildeltEnhetsnr != OppgaveUtil.ENHET_NR_NAY && tildeltEnhetsnr != OppgaveUtil.ENHET_NR_EGEN_ANSATT) {
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

        val mappeId = oppgaveService.finnKlarMappe(tildeltEnhetsnr)
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
