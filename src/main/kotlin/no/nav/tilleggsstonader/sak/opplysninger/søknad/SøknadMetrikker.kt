package no.nav.tilleggsstonader.sak.opplysninger.søknad

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Metrics
import no.nav.tilleggsstonader.kontrakter.søknad.JaNei
import no.nav.tilleggsstonader.sak.opplysninger.søknad.dagligReise.Reise
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.Søknad
import no.nav.tilleggsstonader.sak.opplysninger.søknad.domain.SøknadDagligReise
import org.springframework.stereotype.Component

@Component
class SøknadMetrikker {
    private val offentligTransportCounter: Counter =
        Metrics.counter(
            "søknad.dagligreise.transport.metode",
            "type",
            "offentlig",
        )
    private val bilCounter: Counter =
        Metrics.counter(
            "søknad.dagligreise.transport.metode",
            "type",
            "bil",
        )
    private val taxiCounter: Counter =
        Metrics.counter(
            "søknad.dagligreise.transport.metode",
            "type",
            "taxi",
        )

    fun registrerSøknadMetrikker(søknad: Søknad<out Any>) {
        when (søknad) {
            is SøknadDagligReise -> registrerDagligReiseTransportmetoder(søknad)
            else -> {}
        }
    }

    private fun registrerDagligReiseTransportmetoder(søknad: SøknadDagligReise) {
        val reiser = søknad.data.reiser

        if (reiser.harSøktOmOffentligTransport()) {
            offentligTransportCounter.increment()
        }
        if (reiser.harSøktOmBil()) {
            bilCounter.increment()
        }
        if (reiser.harSøktOmTaxi()) {
            taxiCounter.increment()
        }
    }
}

private fun List<Reise>.harSøktOmOffentligTransport(): Boolean = any { it.kanReiseMedOffentligTransport == JaNei.JA }

private fun List<Reise>.harSøktOmBil(): Boolean = any { it.privatTransport?.utgifterBil != null }

private fun List<Reise>.harSøktOmTaxi(): Boolean = any { it.privatTransport?.taxi?.ønskerSøkeOmTaxi == JaNei.JA }
