package no.nav.tilleggsstonader.sak.hendelser.oppgave

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.felles.tilBehandlingstema
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveDomain
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveRepository
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.oppgave
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.random.Random

class OppgavehendelseHåndtererTest : IntegrationTest() {
    @Autowired
    private lateinit var oppgaveRepository: OppgaveRepository

    @Autowired
    private lateinit var oppgavehendelseHåndterer: OppgavehendelseHåndterer

    lateinit var behandling: Behandling
    lateinit var oppgave: OppgaveDomain

    @BeforeEach
    fun setUp() {
        behandling = behandling()
        testoppsettService.opprettBehandlingMedFagsak(behandling)

        oppgave = oppgave(behandling = behandling, gsakOppgaveId = Random(1000000).nextLong(), tilordnetSaksbehandler = null)
        oppgaveRepository.insert(oppgave)
    }

    @Test
    fun `skal oppdatere intern oppgave om tilhørende oppgave blir oppdatert`() {
        val tilordnetSaksbehandler = "Z999999"
        val oppgaveHendelse = lagOppgavehendelse(tilordnetSaksbehandler, oppgave.gsakOppgaveId)
        oppgavehendelseHåndterer.behandleOppgavehendelser(listOf(oppgaveHendelse))

        assertThat(oppgaveRepository.findByIdOrThrow(oppgave.id).tilordnetSaksbehandler).isEqualTo(tilordnetSaksbehandler)
    }

    @Test
    fun `oppdaterer ikke oppgave om hendelsestype er OPPGAVE_FERDIGSTILT`() {
        val tilordnetSaksbehandler = "Z999999"
        val oppgaveHendelse = lagOppgavehendelse(tilordnetSaksbehandler, oppgave.gsakOppgaveId, Hendelsestype.OPPGAVE_FERDIGSTILT)
        oppgavehendelseHåndterer.behandleOppgavehendelser(listOf(oppgaveHendelse))

        assertThat(oppgaveRepository.findByIdOrThrow(oppgave.id).tilordnetSaksbehandler).isEqualTo(oppgave.tilordnetSaksbehandler)
    }

    private fun lagOppgavehendelse(
        tilordnetSaksbehandler: String?,
        gsakOppgaveId: Long,
        hendelsestype: Hendelsestype = Hendelsestype.OPPGAVE_ENDRET,
    ) = OppgavehendelseRecord(
        hendelse =
            Hendelse(
                hendelsestype = hendelsestype,
                tidspunkt = LocalDateTime.now(),
            ),
        utfortAv = UtfortAv(navIdent = tilordnetSaksbehandler, enhetsnr = "1234"),
        oppgave =
            Oppgave(
                oppgaveId = gsakOppgaveId,
                tilordning = Tilordning(navIdent = "Z999999", enhetsnr = "4462", enhetsmappeId = null),
                kategorisering =
                    Kategorisering(
                        behandlingstema = Stønadstype.LÆREMIDLER.tilBehandlingstema().value,
                        tema = Tema.TSO.name,
                        oppgavetype = Oppgavetype.BehandleSak.value,
                        behandlingstype = null,
                        prioritet = Prioritet.HOY,
                    ),
                versjon = 2,
                behandlingsperiode =
                    Behandlingsperiode(
                        aktiv = LocalDate.now().minusDays(10),
                        frist = LocalDate.now().plusDays(10),
                    ),
                bruker =
                    Bruker(
                        ident = "12345678901",
                        identType = Bruker.IdentType.FOLKEREGISTERIDENT,
                    ),
            ),
    )
}
