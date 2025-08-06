package no.nav.tilleggsstonader.sak.infrastruktur.mocks

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnMappeResponseDto
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnOppgaveRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnOppgaveResponseDto
import no.nav.tilleggsstonader.kontrakter.oppgave.IdentGruppe
import no.nav.tilleggsstonader.kontrakter.oppgave.MappeDto
import no.nav.tilleggsstonader.kontrakter.oppgave.OppdatertOppgaveResponse
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveIdentV2
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveMappe
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.StatusEnum
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.OppdaterPåVentRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.SettPåVentRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.SettPåVentResponse
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.TaAvVentRequest
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

@Configuration
@Profile("mock-oppgave")
class OppgaveClientConfig {
    @Bean
    @Primary
    fun oppgaveClient(oppgavelager: Oppgavelager): OppgaveClient {
        val oppgaveClient = mockk<OppgaveClient>()
        resetMock(oppgaveClient, oppgavelager)
        return oppgaveClient
    }

    companion object {
        const val MAPPE_ID_PÅ_VENT = 10L
        const val MAPPE_ID_KLAR = 20L

        fun resetMock(
            oppgaveClient: OppgaveClient,
            oppgavelager: Oppgavelager,
        ) {
            clearMocks(oppgaveClient)
            oppgavelager.clear()

            oppgavelager.leggTilOppgave(journalføringsoppgaveRequest.tilNyOppgave())
            oppgavelager.leggTilOppgave(klageOppgaveRequest.tilNyOppgave())

            every { oppgaveClient.hentOppgaver(any()) } answers {
                val request = firstArg<FinnOppgaveRequest>()
                val oppgaver =
                    oppgavelager
                        .alleOppgaver()
                        .filter { it.status == StatusEnum.OPPRETTET }
                        .filter { oppgave ->
                            request.behandlingstema?.let { oppgave.behandlingstema == it.value } ?: true
                        }.filter { oppgave -> request.oppgavetype?.let { oppgave.oppgavetype == it.value } ?: true }
                        .filter { oppgave ->
                            request.behandlingstype?.let { oppgave.behandlingstype == it.value } ?: true
                        }.filter { oppgave ->
                            request.erUtenMappe?.takeIf { it }?.let { oppgave.mappeId == null } ?: true
                        }.filter { oppgave -> request.mappeId?.let { oppgave.mappeId?.getOrNull() == it } ?: true }
                        .filter { oppgave ->
                            request.aktørId?.let { aktørId ->
                                // [PdlClientConfig] legger til prefix "00" på aktørId lokalt
                                oppgave.identer?.any { it.ident == aktørId || "00${it.ident}" == aktørId } ?: false
                            } ?: true
                        }.toList()
                val toIndex = minOf((request.offset + request.limit).toInt(), oppgaver.size)
                val paginerteOppgaver = oppgaver.subList(request.offset.toInt(), toIndex)
                FinnOppgaveResponseDto(antallTreffTotalt = oppgaver.size.toLong(), oppgaver = paginerteOppgaver)
            }

            every { oppgaveClient.finnOppgaveMedId(any()) } answers {
                val oppgaveId = firstArg<Long>()
                oppgavelager.hentOppgave(oppgaveId)
            }

            val mapper =
                listOf(
                    MappeDto(MAPPE_ID_PÅ_VENT, OppgaveMappe.PÅ_VENT.navn.first(), "4462"),
                    MappeDto(MAPPE_ID_KLAR, OppgaveMappe.KLAR.navn.first(), "4462"),
                )
            every { oppgaveClient.finnMapper(any(), any()) } returns FinnMappeResponseDto(mapper.size, mapper)

            every { oppgaveClient.opprettOppgave(any()) } answers {
                val oppgave = oppgavelager.leggTilOppgave(firstArg<OpprettOppgaveRequest>().tilNyOppgave())
                oppgave.id
            }

            every { oppgaveClient.ferdigstillOppgave(any()) } answers {
                val oppgave = oppgavelager.hentOppgave(firstArg())
                if (oppgave.status == StatusEnum.FERDIGSTILT) {
                    error("Allerede ferdigstilt")
                }
                val oppdatertOppgave =
                    oppgave.copy(
                        versjon = oppgave.versjon + 1,
                        status = StatusEnum.FERDIGSTILT,
                        ferdigstiltTidspunkt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                    )
                oppgavelager.oppdaterOppgave(oppdatertOppgave)
            }

            every { oppgaveClient.oppdaterOppgave(any()) } answers {
                val oppdaterOppgave =
                    firstArg<Oppgave>().let {
                        val eksisterendeOppgave = oppgavelager.hentOppgave(it.id)
                        val versjon = it.versjon
                        feilHvis(versjon != eksisterendeOppgave.versjon, HttpStatus.CONFLICT) {
                            "Oppgaven har endret seg siden du sist hentet oppgaver. versjon=$versjon (${eksisterendeOppgave.versjon}) " +
                                "For å kunne gjøre endringer må du hente oppgaver på nytt."
                        }
                        eksisterendeOppgave.copy(
                            versjon = versjon + 1,
                            beskrivelse = it.beskrivelse ?: eksisterendeOppgave.beskrivelse,
                            tilordnetRessurs =
                                (it.tilordnetRessurs ?: eksisterendeOppgave.tilordnetRessurs)
                                    ?.takeIf { it.isNotBlank() },
                            mappeId = it.mappeId ?: eksisterendeOppgave.mappeId,
                            fristFerdigstillelse = it.fristFerdigstillelse ?: eksisterendeOppgave.fristFerdigstillelse,
                        )
                    }
                oppgavelager.oppdaterOppgave(oppdaterOppgave) // Forenklet, dette er ikke det som skje ri integrasjoner
                OppdatertOppgaveResponse(oppdaterOppgave.id, oppdaterOppgave.versjonEllerFeil())
            }
            mockFordeling(oppgaveClient, oppgavelager)

            every { oppgaveClient.settPåVent(any()) } answers {
                val request = firstArg<SettPåVentRequest>()
                val oppgave = oppgavelager.hentOppgave(request.oppgaveId)
                brukerfeilHvis(oppgave.tilordnetRessurs != SikkerhetContext.hentSaksbehandler()) {
                    "Kan ikke sette behandling på vent når man ikke er eier av oppgaven."
                }
                val versjon = oppgave.versjon + 1
                oppgavelager.oppdaterOppgave(
                    oppgave.copy(
                        versjon = versjon,
                        beskrivelse = request.kommentar + "\n" + oppgave.beskrivelse,
                        fristFerdigstillelse = request.frist,
                        mappeId = Optional.of(MAPPE_ID_PÅ_VENT),
                        tilordnetRessurs = if (request.beholdOppgave) SikkerhetContext.hentSaksbehandler() else null,
                    ),
                )
                SettPåVentResponse(oppgaveId = request.oppgaveId, oppgaveVersjon = versjon)
            }

            every { oppgaveClient.oppdaterPåVent(any()) } answers {
                val request = firstArg<OppdaterPåVentRequest>()
                val oppgave = oppgavelager.hentOppgave(request.oppgaveId)
                brukerfeilHvis(oppgave.tilordnetRessurs != SikkerhetContext.hentSaksbehandler()) {
                    "Kan ikke oppdatere behandling på vent når man ikke er eier av oppgaven."
                }
                brukerfeilHvis(oppgave.versjon != request.oppgaveVersjon) {
                    "Versjon er feil"
                }
                val versjon = oppgave.versjon + 1
                oppgavelager.oppdaterOppgave(
                    oppgave.copy(
                        versjon = versjon,
                        beskrivelse = request.kommentar + "\n" + oppgave.beskrivelse,
                        fristFerdigstillelse = request.frist,
                        tilordnetRessurs = if (request.beholdOppgave) SikkerhetContext.hentSaksbehandler() else null,
                    ),
                )
                SettPåVentResponse(oppgaveId = request.oppgaveId, oppgaveVersjon = versjon)
            }

            every { oppgaveClient.taAvVent(any()) } answers {
                val request = firstArg<TaAvVentRequest>()
                val oppgave = oppgavelager.hentOppgave(request.oppgaveId)
                brukerfeilHvis(oppgave.tilordnetRessurs != SikkerhetContext.hentSaksbehandler()) {
                    "Kan ikke ta behandling av vent når man ikke er eier av oppgaven."
                }

                val versjon = oppgave.versjon + 1
                oppgavelager.oppdaterOppgave(
                    oppgave.copy(
                        versjon = versjon,
                        beskrivelse = request.kommentar + "\n Tatt av vent\n" + oppgave.beskrivelse,
                        fristFerdigstillelse = LocalDate.now(),
                        tilordnetRessurs = if (request.beholdOppgave) SikkerhetContext.hentSaksbehandler() else null,
                        mappeId = Optional.of(MAPPE_ID_KLAR),
                    ),
                )
                SettPåVentResponse(oppgaveId = request.oppgaveId, oppgaveVersjon = versjon)
            }
        }

        private fun mockFordeling(
            oppgaveClient: OppgaveClient,
            oppgavelager: Oppgavelager,
        ) {
            every { oppgaveClient.fordelOppgave(any(), any(), any()) } answers {
                val oppgaveId = firstArg<Long>()
                val oppgave = oppgavelager.hentOppgave(oppgaveId)
                val versjon = oppgave.versjon
                feilHvis(versjon != thirdArg(), HttpStatus.CONFLICT) {
                    "Oppgaven har endret seg siden du sist hentet oppgaver. " +
                        "For å kunne gjøre endringer må du hente oppgaver på nytt."
                }
                val oppdatertOppgave = oppgave.copy(versjon = versjon + 1, tilordnetRessurs = secondArg())
                oppgavelager.oppdaterOppgave(oppdatertOppgave)
                oppdatertOppgave
            }
        }

        var journalPostId = 0L

        private val journalføringsoppgaveRequest =
            OpprettOppgaveRequest(
                tema = Tema.TSO,
                oppgavetype = Oppgavetype.Journalføring,
                fristFerdigstillelse = LocalDate.now().plusDays(14),
                beskrivelse = "Dummy søknad",
                behandlingstema = "ab0300",
                enhetsnummer = "",
                ident = OppgaveIdentV2(ident = "12345678910", gruppe = IdentGruppe.FOLKEREGISTERIDENT),
                journalpostId = (++journalPostId).toString(),
                mappeId = MAPPE_ID_KLAR,
            )

        private val klageOppgaveRequest =
            OpprettOppgaveRequest(
                tema = Tema.TSO,
                oppgavetype = Oppgavetype.BehandleSak,
                fristFerdigstillelse = LocalDate.now().plusDays(14),
                beskrivelse = "Dummy klage",
                behandlingstema = "ab0300",
                behandlingstype = "ae0058",
                enhetsnummer = "",
                ident = OppgaveIdentV2(ident = "12345678910", gruppe = IdentGruppe.FOLKEREGISTERIDENT),
                journalpostId = (++journalPostId).toString(),
                mappeId = MAPPE_ID_KLAR,
            )
    }
}

@Component
class Oppgavelager {
    private var maxOppgaveId = 0L
    private val oppgavelager = mutableMapOf<Long, Oppgave>()

    fun clear() = oppgavelager.clear()

    fun leggTilOppgave(oppgave: Oppgave): Oppgave {
        require(oppgave.id == 0L) { "Oppgave må ha id=0 for å kunne opprettes" }
        val id = ++maxOppgaveId
        val nyOppgave = oppgave.copy(id = id)
        oppgavelager[nyOppgave.id] = nyOppgave
        return nyOppgave
    }

    /**
     * Skal kun brukes fra [OpprettOppgaveConfig] som oppretter oppgaver for å fylle oppgavelageret.
     */
    fun leggTilOppgaveFraRepository(
        opprettOppgaveRequest: OpprettOppgaveRequest,
        id: Long,
    ): Oppgave {
        require(id > 0) { "Oppgave må ha id > 0 for å kunne opprettes" }
        require(!oppgavelager.contains(id)) { "Oppgave med id=$id finnes allerede i oppgavelageret" }

        val oppgave = opprettOppgaveRequest.tilNyOppgave(id = id)
        oppgavelager[id] = oppgave

        return oppgave
    }

    /**
     * Skal ikke skrive over maxOppgaveId hvis id er null.
     * [oppdaterMaxOppgaveId] kjører etter at man har lagt inn 2 dummyoppgaver i [OppgaveClientConfig]
     * så man skal ikke resette den til 0 hvis det ikke finnes noen oppgaver i repository
     */
    fun oppdaterMaxOppgaveId(id: Long?) {
        id?.let { maxOppgaveId = it }
    }

    fun oppdaterOppgave(oppgave: Oppgave) {
        require(oppgavelager.contains(oppgave.id)) { "Oppgave med id=${oppgave.id} finnes ikke i oppgavelageret" }
        oppgavelager[oppgave.id] = oppgave
    }

    fun alleOppgaver(): List<Oppgave> = oppgavelager.values.toList()

    fun hentOppgave(oppgaveId: Long): Oppgave = oppgavelager[oppgaveId] ?: error("Finner ikke oppgave=$oppgaveId")
}

private fun OpprettOppgaveRequest.tilNyOppgave(id: Long = 0) =
    Oppgave(
        id = id,
        versjon = 1,
        status = StatusEnum.OPPRETTET,
        identer = this.ident!!.let { listOf(OppgaveIdentV2(it.ident!!, it.gruppe!!)) },
        tildeltEnhetsnr = this.enhetsnummer,
        saksreferanse = null,
        journalpostId = this.journalpostId,
        tema = this.tema,
        oppgavetype = this.oppgavetype.value,
        behandlingstema = this.behandlingstema,
        tilordnetRessurs = this.tilordnetRessurs,
        fristFerdigstillelse = this.fristFerdigstillelse,
        aktivDato = this.aktivFra,
        beskrivelse = this.beskrivelse,
        prioritet = this.prioritet,
        behandlingstype = this.behandlingstype,
        behandlesAvApplikasjon = this.behandlesAvApplikasjon,
        mappeId = this.mappeId?.let { Optional.of(it) },
        opprettetTidspunkt = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
    )
