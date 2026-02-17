package no.nav.tilleggsstonader.sak.ekstern.journalføring

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.søknad.KjørelisteSkjema
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingÅrsak
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.integrasjonstest.sendInnKjøreliste
import no.nav.tilleggsstonader.sak.privatbil.Kjøreliste
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteRepository
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil.KjørtDag
import no.nav.tilleggsstonader.sak.util.KjørelisteSkjemaUtil.kjørelisteSkjema
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MottaKjørelisteIntegrationTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var kjørelisteRepository: KjørelisteRepository

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Test
    fun `daglig-reise tso sak med innvilget rammevedtak, mottar kjøreliste, verifiser blir journalført og lagret`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        val fom = 1 januar 2026
        val tom = 14 januar 2026
        val behandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(fom, tom)
            }

        val saksbehandling = testoppsettService.hentSaksbehandling(behandlingId)

        val rammevedtak = kall.privatBil.hentRammevedtak("12345678910")
        val reiseId = rammevedtak.single().reiseId

        assertThat(rammevedtak).hasSize(1)
        assertThat(rammevedtak.single().fom).isEqualTo(fom)
        assertThat(rammevedtak.single().tom).isEqualTo(tom)

        val dagerKjørt =
            listOf(
                KjørtDag(1 januar 2026, 50),
                KjørtDag(4 januar 2026, 70),
                KjørtDag(9 januar 2026, 40),
                KjørtDag(11 januar 2026, 90),
            )
        val kjøreliste =
            kjørelisteSkjema(
                reiseId = reiseId.toString(),
                periode = Datoperiode(fom, tom),
                dagerKjørt = dagerKjørt,
            )

        val journalpostId = sendInnKjøreliste(kjøreliste)

        val lagredeKjørelister = kjørelisteRepository.findByFagsakId(saksbehandling.fagsakId)
        assertThat(lagredeKjørelister).hasSize(1)
        val lagretKjøreliste = lagredeKjørelister.single()
        assertThat(lagretKjøreliste.fagsakId).isEqualTo(saksbehandling.fagsakId)
        assertThat(lagretKjøreliste.journalpostId).isEqualTo(journalpostId)
        assertThat(lagretKjøreliste.data.reiseId).isEqualTo(reiseId)
        assertLagretKjørelisteInneholderDager(innsendtKjøreliste = kjøreliste, lagretKjøreliste = lagretKjøreliste)

        val behandlingerPåFagsak = behandlingRepository.findByFagsakId(saksbehandling.fagsakId)
        val kjørelisteBehandling = behandlingerPåFagsak.single { it.årsak == BehandlingÅrsak.KJØRELISTE }
        assertThat(oppgaveRepository.findByBehandlingId(kjørelisteBehandling.id)).hasSize(1)
    }

    private fun assertLagretKjørelisteInneholderDager(
        innsendtKjøreliste: KjørelisteSkjema,
        lagretKjøreliste: Kjøreliste,
    ) {
        val innsendteReisedager = innsendtKjøreliste.reisedagerPerUkeAvsnitt.flatMap { it.reisedager }
        val lagredeReisedager = lagretKjøreliste.data.reisedager
        assertThat(innsendteReisedager).hasSameSizeAs(lagredeReisedager)

        innsendteReisedager
            .forEach { innsendtReisedag ->
                val lagretDag = lagredeReisedager.singleOrNull { it.dato == innsendtReisedag.dato.verdi }
                assertThat(lagretDag).isNotNull
                assertThat(lagretDag!!.parkeringsutgift).isEqualTo(innsendtReisedag.parkeringsutgift.verdi)
                assertThat(lagretDag.harKjørt).isEqualTo(innsendtReisedag.harKjørt)
            }
    }
}
