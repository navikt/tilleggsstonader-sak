package no.nav.tilleggsstonader.sak.behandling.vent

import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.sak.behandling.vent.SettPåVent
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.mockBrukerContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class SettPåVentBeskrivelseUtilTest {
    private val tidspunkt = LocalDateTime.of(2024, 3, 5, 18, 0)

    private val settPåVent =
        SettPåVent(
            behandlingId = BehandlingId.random(),
            oppgaveId = 1L,
            årsaker = listOf(ÅrsakSettPåVent.DOKUMENTASJON_FRA_BRUKER),
            kommentar = "En kommentar",
        )

    @BeforeEach
    fun setUp() {
        mockBrukerContext("a100")
    }

    @AfterEach
    fun tearDown() {
        clearBrukerContext()
    }

    @Nested
    inner class SettPåVentFlyt {
        @Test
        fun `skal oppdatere beskrivelse med ny info og beholde eksisterende beskrivelse`() {
            val beskrivelse =
                SettPåVentBeskrivelseUtil.settPåVent(
                    Oppgave(id = 0, versjon = 0, beskrivelse = "tidligere beskrivelse", tilordnetRessurs = "a100"),
                    settPåVent,
                    LocalDate.of(2023, 1, 1),
                    tidspunkt,
                )
            assertThat(beskrivelse).isEqualTo(
                """
                --- 05.03.2024 18:00 a100 (a100) ---
                Oppgave endret frist fra <ingen> til 01.01.2023
                Oppgave flyttet fra saksbehandler a100 til <ingen>
                Kommentar: En kommentar
                
                tidligere beskrivelse
                """.trimIndent(),
            )
        }
    }

    @Nested
    inner class OppdaterSettPåVent {
        @Test
        fun `skal oppdatere beskrivelse med ny info og appende beskrivelse fra forrige oppgave`() {
            val beskrivelse =
                SettPåVentBeskrivelseUtil.oppdaterSettPåVent(
                    Oppgave(id = 0, versjon = 0, beskrivelse = "tidligere beskrivelse"),
                    settPåVent,
                    LocalDate.of(2023, 1, 1),
                    tidspunkt,
                )
            assertThat(beskrivelse).isEqualTo(
                """
                --- 05.03.2024 18:00 a100 (a100) ---
                Oppgave endret frist fra <ingen> til 01.01.2023
                Kommentar: En kommentar

                tidligere beskrivelse
                """.trimIndent(),
            )
        }

        @Test
        fun `uendret frist`() {
            val frist = LocalDate.of(2023, 1, 1)
            val beskrivelse =
                SettPåVentBeskrivelseUtil.oppdaterSettPåVent(
                    Oppgave(id = 0, versjon = 0, beskrivelse = "tidligere beskrivelse", fristFerdigstillelse = frist),
                    settPåVent,
                    frist,
                    tidspunkt,
                )
            assertThat(beskrivelse).isEqualTo(
                """
                tidligere beskrivelse
                """.trimIndent(),
            )
        }

        @Test
        fun `skal fjerne saksbehandler på oppgaven når man oppdaterer sett på vent`() {
            val beskrivelse =
                SettPåVentBeskrivelseUtil.oppdaterSettPåVent(
                    Oppgave(id = 0, versjon = 0, tilordnetRessurs = "a100"),
                    settPåVent,
                    LocalDate.of(2023, 1, 1),
                    tidspunkt,
                )
            assertThat(beskrivelse).isEqualTo(
                """
                --- 05.03.2024 18:00 a100 (a100) ---
                Oppgave endret frist fra <ingen> til 01.01.2023
                Oppgave flyttet fra saksbehandler a100 til <ingen>
                """.trimIndent(),
            )
        }
    }

    @Nested
    inner class TaAvVent {
        @Test
        fun `skal oppdatere beskrivelse med ny info og appende beskrivelse fra forrige oppgave`() {
            val beskrivelse =
                SettPåVentBeskrivelseUtil.taAvVent(
                    Oppgave(id = 0, versjon = 0, beskrivelse = "tidligere beskrivelse", tilordnetRessurs = null),
                    tidspunkt,
                )
            assertThat(beskrivelse).isEqualTo(
                """
                --- 05.03.2024 18:00 a100 (a100) ---
                Tatt av vent
                Oppgave flyttet fra saksbehandler <ingen> til a100
                
                tidligere beskrivelse
                """.trimIndent(),
            )
        }
    }
}
