package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.saksstatistikk.BehandlingDVH
import no.nav.tilleggsstonader.libs.utils.dato.april
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.brev.brevmottaker.BrevmottakerVedtaksbrevRepository
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.JournalpostClientMockConfig
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.KafkaFake
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.finnPåTopic
import no.nav.tilleggsstonader.sak.integrasjonstest.extensions.verdiEllerFeil
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteBehandlingManuelt
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.util.KjørelisteUtil.KjørtDag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

// Bruker for å teste litt "side-effekter" som kommer ut av behandlingsløp. Behandlingsstatistikk, vedtaksstatistikk og internt vedtak
class DagligReisePrivatBilStatistikkIntegrationTest : IntegrationTest() {
    @Autowired
    lateinit var brevmottakerVedtaksbrevRepository: BrevmottakerVedtaksbrevRepository

    @Test
    fun `blir produsert vedtaksstatistikk for rammevedtakbehandling og kjørelistebehandling`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom = 6 april 2026
        val tom = 26 april 2026

        val behandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                aktivitet {
                    opprett {
                        aktivitetTiltakTso(fom, tom)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(fom, tom)
                    }
                }
                vilkår {
                    opprett {
                        privatBil(fom, tom)
                    }
                }

                sendInnKjøreliste {
                    periode = Datoperiode(fom, tom)
                    kjørteDager = listOf(KjørtDag(dato = fom, parkeringsutgift = 50))
                }
            }

        verifiserVedtaksstatistikkFinnesForBehandling(behandlingContext.behandlingId)
        verifiserFinnesBehandlingstatistikkForBehandling(behandlingContext.behandlingId)
        verifiserHarBlittProdusertInterntVedtakForBehandling(behandlingContext.behandlingId)

        val kjørelisteBehandling =
            testoppsettService.hentBehandlinger(behandlingContext.fagsakId).single {
                it.type ==
                    BehandlingType.KJØRELISTE
            }
        gjennomførKjørelisteBehandlingManuelt(kjørelisteBehandling)

        verifiserVedtaksstatistikkFinnesForBehandling(kjørelisteBehandling.id)
        verifiserFinnesBehandlingstatistikkForBehandling(kjørelisteBehandling.id)
        verifiserHarBlittProdusertInterntVedtakForBehandling(kjørelisteBehandling.id)
        verifiserHarBlittProdusertVedtaksbrevForKjørelistebehandling(kjørelisteBehandling.id)

        // TODO - også verifisere at statistikk og internt vedtak blir produsert av en automatisk kjørelistebehandling
    }

    private fun verifiserHarBlittProdusertVedtaksbrevForKjørelistebehandling(behandlingId: BehandlingId) {
        val brevmottakereVedtaksbrev = brevmottakerVedtaksbrevRepository.findByBehandlingId(behandlingId)
        assertThat(brevmottakereVedtaksbrev).hasSize(1)
        assertThat(brevmottakereVedtaksbrev.single().bestillingId).isNotNull
    }

    private fun verifiserFinnesBehandlingstatistikkForBehandling(behandlingId: BehandlingId) {
        val behandlingsstatistikkMeldinger =
            KafkaFake
                .sendteMeldinger()
                .finnPåTopic(kafkaTopics.dvhBehandling)
                .map { it.verdiEllerFeil<BehandlingDVH>() }
                .filter { it.behandlingUuid == behandlingId.toString() }

        assertThat(behandlingsstatistikkMeldinger).isNotEmpty
    }

    private fun verifiserVedtaksstatistikkFinnesForBehandling(behandlingId: BehandlingId) {
        val res =
            jdbcTemplate.query(
                "select * from vedtaksstatistikk_v2 where behandling_id=:id",
                mapOf("id" to behandlingId.id),
            ) { rs, _ ->
                rs.getObject("behandling_id") as UUID
            }

        assertThat(res).hasSize(1)
    }

    private fun verifiserHarBlittProdusertInterntVedtakForBehandling(behandlingId: BehandlingId) {
        assertThat(
            JournalpostClientMockConfig.opprettedeJournalposter
                .filter { it.eksternReferanseId == "$behandlingId-blankett" }, // Settes i InterntVedtakTask ved opprettelse av journalpost
        ).hasSize(1)
    }
}
