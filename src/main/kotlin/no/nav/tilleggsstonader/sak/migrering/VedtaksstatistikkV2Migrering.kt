package no.nav.tilleggsstonader.sak.migrering

import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.statistikk.vedtak.VedtaksstatistikkRepositoryV2
import no.nav.tilleggsstonader.sak.statistikk.vedtak.VedtaksstatistikkService
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class VedtaksstatistikkV2Migrering(
    private val vedtaksRepository: VedtakRepository,
    private val behandlingRepository: BehandlingRepository,
    private val vedtaksstatistikkRepositoryV2: VedtaksstatistikkRepositoryV2,
    private val vedtaksstatistikkService: VedtaksstatistikkService,
) {

    val logger = LoggerFactory.getLogger(javaClass)

    @Scheduled(initialDelay = 5000)
    fun migrerStatistikkData() {
        logger.info("starter vedtakssatistikk v2 migrasjon")

        vedtaksstatistikkRepositoryV2.deleteAll()

        vedtaksRepository.findAll().sortedBy { it.sporbar.opprettetTid }.forEach {
            val behandlingId = it.behandlingId
            val behandling = behandlingRepository.findByIdOrThrow(behandlingId)

            if (behandling.resultat == BehandlingResultat.HENLAGT) {
                return
            }
            if (behandling.status != BehandlingStatus.FERDIGSTILT) {
                return
            }

            val fagsakId = behandling.fagsakId

            vedtaksstatistikkService.lagreVedtaksstatistikkV2(behandlingId, fagsakId)
        }

        logger.info("vedtaksstatistikk v2 migrasjon ferdig 🚀")
    }
}
