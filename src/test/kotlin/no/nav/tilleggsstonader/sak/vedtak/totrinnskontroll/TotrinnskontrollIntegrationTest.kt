package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll

import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførBehandlingsløp
import org.junit.jupiter.api.Test

class TotrinnskontrollIntegrationTest : CleanDatabaseIntegrationTest() {
    @Test
    fun `kan angre sende til beslutter`() {
        val behandlingId = gjennomførBehandlingsløp(tilSteg = StegType.BESLUTTE_VEDTAK)

        // Skal ikke kaste feil
        kall.totrinnskontroll.angreSendTilBeslutter(behandlingId)
    }
}
