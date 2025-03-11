import no.nav.familie.prosessering.util.MDCConstants
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tilleggsstonader.libs.log.SecureLogger.secureLogger
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.sortertEtterVedtakstidspunkt
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.infrastruktur.felles.TransactionHandler
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.statistikk.vedtak.VedtaksstatistikkRepositoryV2
import no.nav.tilleggsstonader.sak.statistikk.vedtak.VedtaksstatistikkService
import org.jboss.logging.MDC
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.Executors

@RestController
@RequestMapping("/admin/statistikk/migrering")
@ProtectedWithClaims("azuread")
class VedtaksstatistikkV2MigreringController(
    private val behandlingRepository: BehandlingRepository,
    private val vedtaksstatistikkRepositoryV2: VedtaksstatistikkRepositoryV2,
    private val vedtaksstatistikkService: VedtaksstatistikkService,
    private val personService: PersonService,
    private val behandlingService: BehandlingService,
    private val transactionHandler: TransactionHandler,
) {
    val logger: Logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun migrering() {
        logger.info("Starter migrasjon av aktivitet læremidler i vedtaksstatistikk v2")

        val callId = MDC.get(MDCConstants.MDC_CALL_ID)
        Executors.newVirtualThreadPerTaskExecutor().submit {
            MDC.put(MDCConstants.MDC_CALL_ID, callId)
            try {
                transactionHandler.runInNewTransaction {
                    migrerStatistikkData()
                }
            } catch (e: Exception) {
                secureLogger.error("Feilet jobb", e)
            } finally {
                MDC.remove(MDCConstants.MDC_CALL_ID)
            }
        }
    }

    fun migrerStatistikkData() {
        vedtaksstatistikkRepositoryV2.deleteAll()

        val behandinger =
            behandlingRepository
                .findAll()
                .filter { it.status == BehandlingStatus.FERDIGSTILT }
                .filter { it.resultat != BehandlingResultat.HENLAGT }

        // Bygger opp PDL-cache så vi slipper å gjøre ett kall per person
        personService
            .hentPersonKortBolk(
                behandinger.map { behandlingService.hentAktivIdent(it.id) },
            )

        behandinger
            .sortertEtterVedtakstidspunkt()
            .forEach { behandling ->
                vedtaksstatistikkService.lagreVedtaksstatistikkV2(behandling.id, behandling.fagsakId)
            }

        logger.info("vedtaksstatistikk-migrasjon ferdig 🚀")
    }
}
