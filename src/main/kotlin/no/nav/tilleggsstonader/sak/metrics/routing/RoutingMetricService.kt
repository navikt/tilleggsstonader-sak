package no.nav.tilleggsstonader.sak.metrics.routing

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import no.nav.tilleggsstonader.sak.metrics.MetricUtil
import no.nav.tilleggsstonader.sak.migrering.routing.SøknadRoutingRepository
import no.nav.tilleggsstonader.sak.migrering.routing.Søknadstype
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class RoutingMetricService(
    private val søknadRoutingRepository: SøknadRoutingRepository,
) {
    private val antallRoutingsGauge = MultiGauge.builder("routing_antall").register(Metrics.globalRegistry)

    @Scheduled(initialDelay = MetricUtil.FREKVENS_30_SEC, fixedDelay = MetricUtil.FREKVENS_30_MIN)
    fun antallRoutings() {
        val rows =
            Søknadstype.entries.map {
                val antall = søknadRoutingRepository.countByType(it)
                MultiGauge.Row.of(Tags.of(Tag.of("ytelse", it.name)), antall)
            }
        antallRoutingsGauge.register(rows, true)
    }
}
