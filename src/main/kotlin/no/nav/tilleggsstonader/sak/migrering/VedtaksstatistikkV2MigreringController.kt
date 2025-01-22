package no.nav.tilleggsstonader.sak.migrering

import no.nav.security.token.support.core.api.Unprotected
import no.nav.tilleggsstonader.sak.behandling.BehandlingUtil.sortertEtterVedtakstidspunkt
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.statistikk.vedtak.VedtaksstatistikkRepositoryV2
import no.nav.tilleggsstonader.sak.statistikk.vedtak.VedtaksstatistikkService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/statistikk/migrering")
@Unprotected
class VedtaksstatistikkV2MigreringController(
    private val behandlingRepository: BehandlingRepository,
    private val vedtaksstatistikkRepositoryV2: VedtaksstatistikkRepositoryV2,
    private val vedtaksstatistikkService: VedtaksstatistikkService,
) {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    @GetMapping
    fun migrerStatistikkData() {
        logger.info("starter vedtakssatistikk v2-migrasjon")

        vedtaksstatistikkRepositoryV2.deleteAll()

        behandlingRepository
            .findAll()
            .filter { it.status == BehandlingStatus.FERDIGSTILT }
            .filter { it.resultat != BehandlingResultat.HENLAGT }
            .sortertEtterVedtakstidspunkt()
            .forEach { behandling ->
                vedtaksstatistikkService.lagreVedtaksstatistikkV2(behandling.id, behandling.fagsakId)
            }

        logger.info("vedtaksstatistikk v2-migrasjon ferdig 🚀")
    }
}
