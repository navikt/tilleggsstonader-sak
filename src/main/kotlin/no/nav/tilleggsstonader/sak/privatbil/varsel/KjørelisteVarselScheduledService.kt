package no.nav.tilleggsstonader.sak.privatbil.varsel

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.log.mdc.MDCConstants
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Scheduled service som sender varsler om tilgjengelige kjørelister til brukere.
 * Kjører mandag kl 10 og varsler brukere som har rammevedtak gjeldende for forrige kalenderuke.
 */
@Service
class KjørelisteVarselScheduledService(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakService: FagsakService,
    private val mittNavVarselService: MittNavVarselService,
    private val taskService: TaskService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Kjører mandag kl 10
     * Cron format: <sekund> <minutt> <time> <dag i måned> <måned> <dag i uke>
     */
    @Transactional
    @Scheduled(cron = "0 10 0 * * MON")
    fun sendVarselOmKjørelister() {
        try {
            MDC.put(MDCConstants.MDC_CALL_ID, UUID.randomUUID().toString())
            logger.info("Starter scheduled jobb for sending av kjøreliste-varsler")

            // Optimalisert databasespørring: henter kun behandlinger med aktuelle rammevedtak
            // Dato-validering gjøres i applikasjonslaget for fleksibilitet
            val behandlingerMedRammevedtak =
                behandlingRepository.finnGjeldendeIverksatteBehandlingerMedRammevedtakPrivatBil(
                    stønadstyper = listOf(Stønadstype.DAGLIG_REISE_TSO, Stønadstype.DAGLIG_REISE_TSR),
                )

            logger.info("Fant ${behandlingerMedRammevedtak.size} behandlinger med aktuelle rammevedtak for kjørelister")

            val varselTasker =
                behandlingerMedRammevedtak
                    .filter { mittNavVarselService.skalSendeKjørelisteVarselForForrigeUke(it.id) }
                    .map { fagsakService.hentFagsak(it.fagsakId).fagsakPersonId }
                    .distinct()
                    .map { SendKjorelistevarselTilBrukerTask.opprett(it) }

            logger.info("Skal sende ${varselTasker.size} varsler om tilgjengelige kjørelister")

            if (varselTasker.isNotEmpty()) {
                taskService.saveAll(varselTasker)
            }

            logger.info("Fullført opprettelse av ${varselTasker.size} tasker for sending av varsel")
        } finally {
            MDC.clear()
        }
    }
}
