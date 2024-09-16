package no.nav.tilleggsstonader.sak.metrics

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class LøpendeBehandlingerMetricService(private val behandlingRepository: BehandlingRepository) {

    private val antallGjeldendeIverksatteBehandlingerGauge = MultiGauge.builder("løpende_iverksatte_behandlinger").register(Metrics.globalRegistry)

    @Scheduled(initialDelay = MetricUtil.FREKVENS_30_SEC, fixedDelay = MetricUtil.FREKVENS_30_MIN)
    fun hentAntallGjeldendeIverksatteBehandlinger() {
        val rows = Stønadstype.entries.map {
            val antallGjeldendeIverksatteBehandlinger = behandlingRepository.antallGjeldendeIverksatteBehandlinger(stønadstype = it)
            MultiGauge.Row.of(Tags.of(Tag.of("ytelse", it.name)), antallGjeldendeIverksatteBehandlinger)
        }
        antallGjeldendeIverksatteBehandlingerGauge.register(rows, true)
    }
}
