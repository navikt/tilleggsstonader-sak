package no.nav.tilleggsstonader.sak.integrasjonstest.extensions.kall

import no.nav.tilleggsstonader.sak.IntegrationTest

val IntegrationTest.kall: Kall
    get() = Kall(this)

class Kall(
    private val test: IntegrationTest,
) {
    val totrinnskontroll: TotrinnskontrollKall
        get() = TotrinnskontrollKall(test)
}
