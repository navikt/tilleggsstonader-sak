package no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import org.junit.jupiter.api.Test

class TotrinnskontrollIntegrationTest : CleanDatabaseIntegrationTest() {
    @Test
    fun `kan angre sende til beslutter`() {
        val behandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
                tilSteg = StegType.BESLUTTE_VEDTAK,
            ) {
                defaultDagligReiseTsoTestdata()
            }

        // Skal ikke kaste feil
        kall.totrinnskontroll.angreSendTilBeslutter(behandlingId)
    }
}
