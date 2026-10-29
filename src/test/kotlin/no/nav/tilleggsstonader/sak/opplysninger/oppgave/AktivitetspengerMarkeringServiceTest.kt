package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.oppgave.Behandlingstype
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveBruker
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveBrukerType
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.StatusEnum
import no.nav.tilleggsstonader.libs.feil.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockUnleashService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.domain.OppgaveMedMetadata
import no.nav.tilleggsstonader.sak.tilgang.AuditLoggerEvent
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.eksternOppgave
import no.nav.tilleggsstonader.sak.util.oppgave
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class AktivitetspengerMarkeringServiceTest {
    private val oppgaveService = mockk<OppgaveService>()
    private val oppgaveRepository = mockk<OppgaveRepository>()
    private val tilgangService = mockk<TilgangService>(relaxed = true)
    private val unleashService = mockUnleashService()

    private val service =
        AktivitetspengerMarkeringService(
            oppgaveService = oppgaveService,
            oppgaveRepository = oppgaveRepository,
            tilgangService = tilgangService,
            unleashService = unleashService,
        )

    private val behandling = behandling()

    @BeforeEach
    fun setUp() {
        every { oppgaveService.oppdaterBehandlingstype(any(), any()) } answers {
            OppgaveMedMetadata(firstArg(), null)
        }
    }

    @Nested
    inner class BehandlingsoppgaveTest {
        private val eksternOppgave = eksternOppgave(id = OPPGAVE_ID, oppgavetype = Oppgavetype.BehandleSak.value)

        @BeforeEach
        fun setUp() {
            every { oppgaveRepository.findByGsakOppgaveId(OPPGAVE_ID) } returns
                oppgave(behandlingId = behandling.id, gsakOppgaveId = OPPGAVE_ID, type = Oppgavetype.BehandleSak)
        }

        @Test
        fun `skal markere behandlingsoppgave med aktivitetspenger`() {
            every { oppgaveService.hentOppgave(OPPGAVE_ID) } returns eksternOppgave

            service.endreMarkering(OPPGAVE_ID, gjelderAktivitetspenger = true)

            verify { oppgaveService.oppdaterBehandlingstype(eksternOppgave, Behandlingstype.Aktivitetspenger) }
            verify {
                tilgangService.validerSkrivetilgangTilBehandling(
                    behandling.id,
                    AuditLoggerEvent.UPDATE,
                    validerTilordnetOppgave = false,
                )
            }
        }

        @Test
        fun `skal fjerne markering på behandlingsoppgave`() {
            val markertOppgave = eksternOppgave.copy(behandlingstype = Behandlingstype.Aktivitetspenger.value)
            every { oppgaveService.hentOppgave(OPPGAVE_ID) } returns markertOppgave

            service.endreMarkering(OPPGAVE_ID, gjelderAktivitetspenger = false)

            verify { oppgaveService.oppdaterBehandlingstype(markertOppgave, null) }
        }

        @Test
        fun `skal avvise endring når markering ikke er aktivert`() {
            every { unleashService.isEnabled(Toggle.SKAL_MARKERE_AKTIVITETSPENGER_OPPGAVER) } returns false

            assertThatThrownBy { service.endreMarkering(OPPGAVE_ID, gjelderAktivitetspenger = true) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("ikke aktivert")
            verify(exactly = 0) { oppgaveService.oppdaterBehandlingstype(any(), any()) }
        }

        @Test
        fun `skal ikke kunne fjerne markering uten toggle`() {
            every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_AKTIVITETSPENGER) } returns false

            assertThatThrownBy { service.endreMarkering(OPPGAVE_ID, gjelderAktivitetspenger = false) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("ikke tilgang")
            verify(exactly = 0) { oppgaveService.oppdaterBehandlingstype(any(), any()) }
        }

        @Test
        fun `skal kunne legge til markering uten toggle`() {
            every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_AKTIVITETSPENGER) } returns false
            every { oppgaveService.hentOppgave(OPPGAVE_ID) } returns eksternOppgave

            service.endreMarkering(OPPGAVE_ID, gjelderAktivitetspenger = true)

            verify { oppgaveService.oppdaterBehandlingstype(eksternOppgave, Behandlingstype.Aktivitetspenger) }
        }

        @Test
        fun `skal markere klage som aktivitetspenger-klage`() {
            val klage = eksternOppgave.copy(behandlingstype = Behandlingstype.Klage.value)
            every { oppgaveService.hentOppgave(OPPGAVE_ID) } returns klage

            service.endreMarkering(OPPGAVE_ID, gjelderAktivitetspenger = true)

            verify { oppgaveService.oppdaterBehandlingstype(klage, Behandlingstype.AktivitetspengerKlage) }
        }

        @Test
        fun `skal fjerne markering på aktivitetspenger-klage og beholde klage`() {
            val klage = eksternOppgave.copy(behandlingstype = Behandlingstype.AktivitetspengerKlage.value)
            every { oppgaveService.hentOppgave(OPPGAVE_ID) } returns klage

            service.endreMarkering(OPPGAVE_ID, gjelderAktivitetspenger = false)

            verify { oppgaveService.oppdaterBehandlingstype(klage, Behandlingstype.Klage) }
        }

        @Test
        fun `skal ikke overskrive annen behandlingstype`() {
            every { oppgaveService.hentOppgave(OPPGAVE_ID) } returns
                eksternOppgave.copy(behandlingstype = "ae0046")

            assertThatThrownBy { service.endreMarkering(OPPGAVE_ID, gjelderAktivitetspenger = true) }
                .isInstanceOf(ApiFeil::class.java)
                .hasMessageContaining("ae0046")
            verify(exactly = 0) { oppgaveService.oppdaterBehandlingstype(any(), any()) }
        }

        @Test
        fun `skal ikke endre ferdigstilt oppgave`() {
            every { oppgaveService.hentOppgave(OPPGAVE_ID) } returns eksternOppgave(status = StatusEnum.FERDIGSTILT)

            assertThatThrownBy { service.endreMarkering(OPPGAVE_ID, gjelderAktivitetspenger = true) }
                .isInstanceOf(ApiFeil::class.java)
        }

        @Test
        fun `skal ikke endre oppgave for manglende tema`() {
            every { oppgaveService.hentOppgave(OPPGAVE_ID) } returns eksternOppgave().copy(tema = null)

            assertThatThrownBy { service.endreMarkering(OPPGAVE_ID, gjelderAktivitetspenger = true) }
                .isInstanceOf(ApiFeil::class.java)
        }
    }

    @Nested
    inner class OppgaveUtenBehandling {
        private val bruker = OppgaveBruker(ident = "12345678910", type = OppgaveBrukerType.PERSON)

        @BeforeEach
        fun setUp() {
            every { oppgaveRepository.findByGsakOppgaveId(OPPGAVE_ID) } returns null
        }

        @Test
        fun `skal markere oppgaven og validere tilgang til person`() {
            val journalføringsoppgave = eksternOppgave(id = OPPGAVE_ID, bruker = bruker)
            every { oppgaveService.hentOppgave(OPPGAVE_ID) } returns journalføringsoppgave

            service.endreMarkering(OPPGAVE_ID, gjelderAktivitetspenger = true)

            verify { tilgangService.validerTilgangTilPerson("12345678910", AuditLoggerEvent.UPDATE) }
            verify { oppgaveService.oppdaterBehandlingstype(journalføringsoppgave, Behandlingstype.Aktivitetspenger) }
        }

        @Test
        fun `skal kunne markere andre TS-oppgaver uten behandling, som vurder henvendelse`() {
            val oppgave = eksternOppgave(id = OPPGAVE_ID, oppgavetype = Oppgavetype.VurderHenvendelse.value, bruker = bruker)
            every { oppgaveService.hentOppgave(OPPGAVE_ID) } returns oppgave

            service.endreMarkering(OPPGAVE_ID, gjelderAktivitetspenger = true)

            verify { oppgaveService.oppdaterBehandlingstype(oppgave, Behandlingstype.Aktivitetspenger) }
        }

        @Test
        fun `skal ikke endre oppgave uten bruker`() {
            every { oppgaveService.hentOppgave(OPPGAVE_ID) } returns eksternOppgave(id = OPPGAVE_ID)

            assertThatThrownBy { service.endreMarkering(OPPGAVE_ID, gjelderAktivitetspenger = true) }
                .isInstanceOf(ApiFeil::class.java)
        }
    }

    companion object {
        private const val OPPGAVE_ID = 123L
    }
}
