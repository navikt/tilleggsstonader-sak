package no.nav.tilleggsstonader.sak.brev.mellomlager

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

class MellomlagringBrevIntegrationTest : IntegrationTest() {
    @Test
    fun `samtidige kall skal ikke kaste feil pga race-condition`() {
        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
                tilSteg = StegType.SEND_TIL_BESLUTTER,
            ) {
                defaultDagligReiseTsoTestdata()
            }

        val routingKall =
            (1..5).map {
                CompletableFuture.supplyAsync {
                    kall.brev.apiRespons.mellomlagre(
                        behandlingContext.behandlingId,
                        MellomlagreBrevDto(
                            brevverdier = "tralala",
                            brevmal = "trololol",
                        ),
                    )
                }
            }

        routingKall.forEach {
            it.get().expectStatus().isOk
        }
    }
}
