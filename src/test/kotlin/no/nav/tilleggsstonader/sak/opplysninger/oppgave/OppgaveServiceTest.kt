package no.nav.tilleggsstonader.sak.opplysninger.oppgave

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Behandlingstema
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnMappeResponseDto
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnOppgaveResponseDto
import no.nav.tilleggsstonader.kontrakter.oppgave.IdentGruppe
import no.nav.tilleggsstonader.kontrakter.oppgave.MappeDto
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveIdentV2
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.StatusEnum
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakId
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.exception.IntegrasjonException
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.OppgaveClientConfig
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.OppgaveUtil.ENHET_NR_NAY
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto.FinnOppgaveRequestDto
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.lagNavn
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.pdlPersonKort
import no.nav.tilleggsstonader.sak.util.fagsak
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class OppgaveServiceTest {

    private val oppgaveClient = mockk<OppgaveClient>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    private val fagsakService = mockk<FagsakService>()
    private val oppgaveRepository = mockk<OppgaveRepository>()
    private val cacheManager = ConcurrentMapCacheManager()
    private val personService = mockk<PersonService>(relaxed = true)

    private val oppgaveService =
        OppgaveService(
            oppgaveClient = oppgaveClient,
            fagsakService = fagsakService,
            oppgaveRepository = oppgaveRepository,
            arbeidsfordelingService = arbeidsfordelingService,
            cacheManager = cacheManager,
            personService = personService,
        )

    val opprettOppgaveDomainSlot = slot<OppgaveDomain>()

    @BeforeEach
    internal fun setUp() {
        opprettOppgaveDomainSlot.clear()
        every { oppgaveClient.finnOppgaveMedId(any()) } returns lagEksternTestOppgave()
        every { oppgaveRepository.insert(capture(opprettOppgaveDomainSlot)) } returns lagTestOppgave()
        every { oppgaveRepository.update(any()) } answers { firstArg() }
        every { oppgaveRepository.finnOppgaveMetadata(any()) } returns emptyList()
        every { oppgaveClient.finnMapper(any(), any()) } returns FinnMappeResponseDto(
            2,
            listOf(
                MappeDto(OppgaveClientConfig.MAPPE_ID_KLAR, OppgaveMappe.KLAR.navn, "4462"),
                MappeDto(OppgaveClientConfig.MAPPE_ID_PÅ_VENT, OppgaveMappe.PÅ_VENT.navn, "4462"),
            ),
        )
        mockkObject(OppgaveUtil)
        every { OppgaveUtil.utledBehandlesAvApplikasjon(any<Oppgavetype>()) } answers { callOriginal() }
        every { OppgaveUtil.skalPlasseresIKlarMappe(any<Oppgavetype>()) } answers { callOriginal() }
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(OppgaveUtil)
    }

    @Test
    fun `Opprett oppgave skal kunne opprettes uten behandlingId`() {
        val slot = slot<OpprettOppgaveRequest>()
        mockOpprettOppgave(slot)

        oppgaveService.opprettOppgave(
            personIdent = FNR,
            stønadstype = Stønadstype.BARNETILSYN,
            behandlingId = null,
            oppgave = OpprettOppgave(oppgavetype = Oppgavetype.Journalføring),
        )
        assertThat(opprettOppgaveDomainSlot.captured.behandlingId).isNull()
    }

    @Test
    fun `Opprett oppgave må ha kobling til behandling når man oppretter en BehandleSak-oppgave`() {
        assertThatThrownBy {
            oppgaveService.opprettOppgave(
                personIdent = FNR,
                stønadstype = Stønadstype.BARNETILSYN,
                behandlingId = null,
                oppgave = OpprettOppgave(oppgavetype = Oppgavetype.BehandleSak),
            )
        }.hasMessage("Må ha behandlingId når man oppretter oppgave for behandle sak")
    }

    @Test
    fun `Opprett oppgave skal samle data og opprette en ny oppgave basert på fagsak, behandling, fnr og enhet`() {
        val slot = slot<OpprettOppgaveRequest>()
        mockOpprettOppgave(slot)

        oppgaveService.opprettOppgave(BEHANDLING_ID, OpprettOppgave(oppgavetype = Oppgavetype.BehandleSak))

        assertThat(slot.captured.enhetsnummer).isEqualTo(ENHETSNUMMER)
        assertThat(slot.captured.ident).isEqualTo(OppgaveIdentV2(ident = FNR, gruppe = IdentGruppe.FOLKEREGISTERIDENT))
        assertThat(slot.captured.behandlingstema).isEqualTo(Behandlingstema.TilsynBarn.value)
        assertThat(slot.captured.fristFerdigstillelse).isAfterOrEqualTo(osloDateNow().plusDays(1))
        assertThat(slot.captured.aktivFra).isEqualTo(osloDateNow())
        assertThat(slot.captured.tema).isEqualTo(Tema.TSO)
        assertThat(opprettOppgaveDomainSlot.captured.behandlingId).isEqualTo(BEHANDLING_ID)
    }

    @Test
    fun `Opprett oppgave som feiler på en ukjent måte skal bare kaste feil videre`() {
        val slot = slot<OpprettOppgaveRequest>()
        mockOpprettOppgave(slot)
        every { oppgaveClient.opprettOppgave(any()) } throws IntegrasjonException("En merkelig feil vi ikke kjenner til")
        assertThrows<IntegrasjonException> {
            oppgaveService.opprettOppgave(BEHANDLING_ID, OpprettOppgave(oppgavetype = Oppgavetype.BehandleSak))
        }
    }

    @Nested
    inner class OpprettOppgaveIMappe {

        @Test
        fun `Skal opprette behandle-sak-oppgave i Klar-mappe`() {
            val slot = slot<OpprettOppgaveRequest>()
            mockOpprettOppgave(slot)

            oppgaveService.opprettOppgave(BEHANDLING_ID, OpprettOppgave(oppgavetype = Oppgavetype.BehandleSak))

            assertThat(slot.captured.mappeId).isEqualTo(OppgaveClientConfig.MAPPE_ID_KLAR)
        }

        @Test
        fun `Skal opprette vurder henvendelse-oppgave uten mappe`() {
            val slot = slot<OpprettOppgaveRequest>()
            mockOpprettOppgave(slot)
            every { OppgaveUtil.utledBehandlesAvApplikasjon(Oppgavetype.VurderHenvendelse) } returns ""
            every { OppgaveUtil.skalPlasseresIKlarMappe(Oppgavetype.VurderHenvendelse) } returns false

            oppgaveService.opprettOppgave(BEHANDLING_ID, OpprettOppgave(oppgavetype = Oppgavetype.VurderHenvendelse))

            assertThat(slot.captured.mappeId).isNull()
        }
    }

    @Test
    fun `Skal kunne hente oppgave gitt en ID`() {
        every { oppgaveClient.finnOppgaveMedId(any()) } returns lagEksternTestOppgave()
        val oppgave = oppgaveService.hentOppgave(GSAK_OPPGAVE_ID)

        assertThat(oppgave.id).isEqualTo(GSAK_OPPGAVE_ID)
    }

    @Test
    fun `Skal hente oppgaver gitt en filtrering`() {
        every { oppgaveClient.hentOppgaver(any()) } returns lagFinnOppgaveResponseDto()
        val respons = oppgaveService.hentOppgaver(FinnOppgaveRequestDto(ident = null, enhet = ENHET_NR_NAY))

        assertThat(respons.antallTreffTotalt).isEqualTo(1)
        assertThat(respons.oppgaver.first().id).isEqualTo(GSAK_OPPGAVE_ID)
    }

    @Test
    fun `Ferdigstill oppgave`() {
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any())
        } returns lagTestOppgave()
        every { oppgaveRepository.update(any()) } returns lagTestOppgave()
        val slot = slot<Long>()
        every { oppgaveClient.ferdigstillOppgave(capture(slot)) } just runs

        oppgaveService.ferdigstillBehandleOppgave(BEHANDLING_ID, Oppgavetype.BehandleSak)
        assertThat(slot.captured).isEqualTo(GSAK_OPPGAVE_ID)
    }

    @Test
    fun `Ferdigstill oppgave feiler fordi den ikke finner oppgave på behandlingen`() {
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any())
        } returns null
        every { oppgaveRepository.insert(any()) } returns lagTestOppgave()

        assertThatThrownBy {
            oppgaveService.ferdigstillBehandleOppgave(
                BEHANDLING_ID,
                Oppgavetype.BehandleSak,
            )
        }
            .hasMessage("Finner ikke oppgave for behandling $BEHANDLING_ID type=BehandleSak")
            .isInstanceOf(java.lang.IllegalStateException::class.java)
    }

    @Test
    fun `Ferdigstill oppgave hvis oppgave ikke finnes - kaster ikke feil`() {
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any())
        } returns null
        oppgaveService.ferdigstillOppgaveHvisOppgaveFinnes(BEHANDLING_ID, Oppgavetype.BehandleSak)
    }

    @Test
    fun `Ferdigstill oppgave - hvis oppgave finnes`() {
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any())
        } returns lagTestOppgave()
        every { oppgaveRepository.update(any()) } returns lagTestOppgave()
        val slot = slot<Long>()
        every { oppgaveClient.ferdigstillOppgave(capture(slot)) } just runs

        oppgaveService.ferdigstillOppgaveHvisOppgaveFinnes(BEHANDLING_ID, Oppgavetype.BehandleSak)
        assertThat(slot.captured).isEqualTo(GSAK_OPPGAVE_ID)
    }

    @Test
    fun `Ferdigstill oppgave - oppgaven er feilregistrert`() {
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any())
        } returns lagTestOppgave()

        every { oppgaveClient.ferdigstillOppgave(any()) } throws Exception("Oppgaven er allerede ferdigstilt")
        every { oppgaveClient.finnOppgaveMedId(any()) } returns lagEksternTestOppgave().copy(status = StatusEnum.FEILREGISTRERT)

        val slot = slot<OppgaveDomain>()

        every { oppgaveRepository.update(capture(slot)) } answers { firstArg() }

        oppgaveService.ferdigstillOppgaveOgsåHvisFeilregistrert(BEHANDLING_ID, Oppgavetype.BehandleSak)

        assertThat(slot.captured.erFerdigstilt).isTrue()
    }

    @Test
    fun `Fordel oppgave skal tildele oppgave til saksbehandler`() {
        val oppgaveSlot = slot<Long>()
        val saksbehandlerSlot = slot<String>()

        every { oppgaveClient.fordelOppgave(capture(oppgaveSlot), capture(saksbehandlerSlot), any()) } answers {
            lagEksternTestOppgave().copy(tilordnetRessurs = secondArg(), versjon = thirdArg<Int>() + 1)
        }

        oppgaveService.fordelOppgave(GSAK_OPPGAVE_ID, SAKSBEHANDLER_ID, 1)

        assertThat(GSAK_OPPGAVE_ID).isEqualTo(oppgaveSlot.captured)
        assertThat(SAKSBEHANDLER_ID).isEqualTo(saksbehandlerSlot.captured)
    }

    @Test
    fun `Tilbakestill oppgave skal nullstille tildeling på oppgave`() {
        val oppgaveSlot = slot<Long>()
        every { oppgaveClient.fordelOppgave(capture(oppgaveSlot), any(), any()) } returns mockk()

        oppgaveService.tilbakestillFordelingPåOppgave(GSAK_OPPGAVE_ID, 1)

        assertThat(GSAK_OPPGAVE_ID).isEqualTo(oppgaveSlot.captured)
        verify(exactly = 1) { oppgaveClient.fordelOppgave(any(), null, 1) }
    }

    @Test
    fun `Skal sette frist for oppgave`() {
        val frister = listOf<Pair<LocalDateTime, LocalDate>>(
            Pair(torsdag.morgen(), fredagFrist),
            Pair(torsdag.kveld(), mandagFrist),
            Pair(fredag.morgen(), mandagFrist),
            Pair(fredag.kveld(), tirsdagFrist),
            Pair(lørdag.morgen(), tirsdagFrist),
            Pair(lørdag.kveld(), tirsdagFrist),
            Pair(søndag.morgen(), tirsdagFrist),
            Pair(søndag.kveld(), tirsdagFrist),
            Pair(mandag.morgen(), tirsdagFrist),
            Pair(mandag.kveld(), onsdagFrist),
        )

        frister.forEach {
            assertThat(oppgaveService.lagFristForOppgave(it.first)).isEqualTo(it.second)
        }
    }

    @Test
    fun `skal legge til navn på oppgave hvis oppgaven har folkeregisterident`() {
        val oppgaveIdMedNavn = 1L

        every { personService.hentAktørIder(any()) } returns PdlIdenter(listOf(PdlIdent("1", false)))
        every { personService.hentPersonKortBolk(any()) } answers {
            firstArg<List<String>>().associateWith { pdlPersonKort(navn = lagNavn(fornavn = "fornavn$it")) }
        }
        every { oppgaveClient.hentOppgaver(any()) } returns FinnOppgaveResponseDto(
            2,
            listOf(
                lagEksternTestOppgave().copy(
                    id = oppgaveIdMedNavn,
                    identer = listOf(OppgaveIdentV2("1", gruppe = IdentGruppe.FOLKEREGISTERIDENT)),
                ),
                lagEksternTestOppgave().copy(id = 2, identer = listOf(OppgaveIdentV2("2", gruppe = IdentGruppe.ORGNR))),
                lagEksternTestOppgave().copy(id = 3),
            ),
        )
        val oppgaver =
            oppgaveService.hentOppgaver(FinnOppgaveRequestDto(ident = "01010172272", enhet = ENHET_NR_NAY)).oppgaver

        verify(exactly = 1) { personService.hentPersonKortBolk(eq(listOf("1"))) }
        assertThat(oppgaver.single { it.id == oppgaveIdMedNavn }.navn).contains("fornavn1 ")
        assertThat(oppgaver.filterNot { it.id == oppgaveIdMedNavn }.map { it.navn }).containsOnly("Mangler navn")
    }

    @Test
    fun `skal legge til behandlingId på oppgaver for å enklere kunne gå til behandling fra frontend`() {
        val oppgaveIdMedBehandling = 1L
        val behandlingId = UUID.randomUUID()

        every { oppgaveClient.hentOppgaver(any()) } returns FinnOppgaveResponseDto(
            2,
            listOf(
                lagEksternTestOppgave().copy(id = oppgaveIdMedBehandling),
                lagEksternTestOppgave().copy(id = 2),
            ),
        )
        every { oppgaveRepository.finnOppgaveMetadata(any()) } answers {
            firstArg<List<Long>>()
                .filter { it == oppgaveIdMedBehandling }
                .map { OppgaveMetadata(it, behandlingId, null) }
        }

        val oppgaver = oppgaveService.hentOppgaver(FinnOppgaveRequestDto(ident = null, enhet = ENHET_NR_NAY)).oppgaver

        assertThat(oppgaver.single { it.id == oppgaveIdMedBehandling }.behandlingId).isEqualTo(behandlingId)
        assertThat(oppgaver.single { it.id != oppgaveIdMedBehandling }.behandlingId).isNull()
    }

    @Test
    fun `skal finne oppgaver på vent når`() {
        every { oppgaveClient.hentOppgaver(any()) } returns FinnOppgaveResponseDto(0, emptyList())

        oppgaveService.hentOppgaver(FinnOppgaveRequestDto(ident = null, oppgaverPåVent = true, enhet = ENHET_NR_NAY))

        verify { oppgaveClient.hentOppgaver(match { it.erUtenMappe == false && it.mappeId == OppgaveClientConfig.MAPPE_ID_PÅ_VENT }) }
    }

    @Test
    fun `skal bruke klarmappe når oppgaverPåVent er false`() {
        every { oppgaveClient.hentOppgaver(any()) } returns FinnOppgaveResponseDto(0, emptyList())

        oppgaveService.hentOppgaver(FinnOppgaveRequestDto(ident = null, oppgaverPåVent = false, enhet = ENHET_NR_NAY))

        verify { oppgaveClient.hentOppgaver(match { it.erUtenMappe == false }) }
    }

    private fun mockOpprettOppgave(slot: CapturingSlot<OpprettOppgaveRequest>) {
        every { fagsakService.hentFagsakForBehandling(BEHANDLING_ID) } returns lagTestFagsak()

        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndErFerdigstiltIsFalse(any(), any())
        } returns null
        every { arbeidsfordelingService.hentNavEnhetId(any(), any()) } returns ENHETSNUMMER
        every { oppgaveClient.opprettOppgave(capture(slot)) } answers {
            val oppgaveRequest: OpprettOppgaveRequest = firstArg()
            if (oppgaveRequest.enhetsnummer == null) {
                throw IntegrasjonException("Fant ingen gyldig arbeidsfordeling for oppgaven")
            } else {
                GSAK_OPPGAVE_ID
            }
        }
    }

    private fun lagTestFagsak(): Fagsak {
        return fagsak(
            id = FAGSAK_ID,
            stønadstype = Stønadstype.BARNETILSYN,
            eksternId = EksternFagsakId(FAGSAK_EKSTERN_ID, FAGSAK_ID),
            identer = setOf(PersonIdent(ident = FNR)),
        )
    }

    private fun lagTestOppgave(): OppgaveDomain {
        return OppgaveDomain(
            behandlingId = BEHANDLING_ID,
            type = Oppgavetype.BehandleSak,
            gsakOppgaveId = GSAK_OPPGAVE_ID,
        )
    }

    private fun lagEksternTestOppgave(): Oppgave {
        return Oppgave(id = GSAK_OPPGAVE_ID, versjon = 0)
    }

    private fun lagFinnOppgaveResponseDto(): FinnOppgaveResponseDto {
        return FinnOppgaveResponseDto(
            antallTreffTotalt = 1,
            oppgaver = listOf(lagEksternTestOppgave()),
        )
    }

    companion object {

        private val FAGSAK_ID = UUID.fromString("1242f220-cad3-4640-95c1-190ec814c91e")
        private const val FAGSAK_EKSTERN_ID = 98765L
        private const val GSAK_OPPGAVE_ID = 12345L
        private val BEHANDLING_ID = UUID.fromString("1c4209bd-3217-4130-8316-8658fe300a84")
        private const val ENHETSNUMMER = "4462"
        private const val FNR = "11223312345"
        private const val SAKSBEHANDLER_ID = "Z999999"
    }
}

private fun LocalDateTime.kveld(): LocalDateTime {
    return this.withHour(20)
}

private fun LocalDateTime.morgen(): LocalDateTime {
    return this.withHour(8)
}

private val torsdag = LocalDateTime.of(2021, 4, 1, 12, 0)
private val fredag = LocalDateTime.of(2021, 4, 2, 12, 0)
private val lørdag = LocalDateTime.of(2021, 4, 3, 12, 0)
private val søndag = LocalDateTime.of(2021, 4, 4, 12, 0)
private val mandag = LocalDateTime.of(2021, 4, 5, 12, 0)

private val fredagFrist = LocalDate.of(2021, 4, 2)
private val mandagFrist = LocalDate.of(2021, 4, 5)
private val tirsdagFrist = LocalDate.of(2021, 4, 6)
private val onsdagFrist = LocalDate.of(2021, 4, 7)
