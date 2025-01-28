package no.nav.tilleggsstonader.sak.behandlingsflyt

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics

class StegMetrics(
    private val behandlingSteg: List<BehandlingSteg<*>>,
) {
    private val stegSuksessMetrics: Map<StegType, Counter> = initStegMetrikker("suksess")

    private val stegFeiletMetrics: Map<StegType, Counter> = initStegMetrikker("feil")

    fun success(stegType: StegType) {
        oppdaterMetrikk(stegType, stegSuksessMetrics)
    }

    fun failure(stegType: StegType) {
        oppdaterMetrikk(stegType, stegFeiletMetrics)
    }

    private fun oppdaterMetrikk(
        stegType: StegType,
        metrikk: Map<StegType, Counter>,
    ) {
        metrikk[stegType]?.increment()
    }

    private fun initStegMetrikker(type: String): Map<StegType, Counter> =
        behandlingSteg.associate {
            it.stegType() to
                Metrics.counter(
                    "behandling.steg.$type",
                    "steg",
                    it.stegType().name,
                    "beskrivelse",
                    "${it.stegType().rekkefølge} ${it.stegType().displayName()}",
                )
        }
}
