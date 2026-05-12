package no.nav.tilleggsstonader.sak.privatbil.varsel

import no.nav.familie.prosessering.internal.TaskService
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.log.mdc.MDCConstants
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class KjørelistevarselService(
    private val behandlingRepository: BehandlingRepository,
    private val mittNavVarselService: MittNavVarselService,
    private val fagsakService: FagsakService,
    private val taskService: TaskService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    fun sendUkentligVarselOmKjørelister() {
        try {
            MDC.put(MDCConstants.MDC_CALL_ID, UUID.randomUUID().toString())
            logger.info("Starter scheduled jobb for sending av kjøreliste-varsler")

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
