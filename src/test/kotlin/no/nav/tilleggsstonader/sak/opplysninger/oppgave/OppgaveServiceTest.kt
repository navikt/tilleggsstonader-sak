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
import no.nav.tilleggsstonader.kontrakter.felles.Enhet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.felles.Tema
import no.nav.tilleggsstonader.kontrakter.oppgave.Behandlingstype
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnMappeResponseDto
import no.nav.tilleggsstonader.kontrakter.oppgave.FinnOppgaveResponseDto
import no.nav.tilleggsstonader.kontrakter.oppgave.MappeDto
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgave
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveBruker
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveBrukerType
import no.nav.tilleggsstonader.kontrakter.oppgave.OppgaveMappe
import no.nav.tilleggsstonader.kontrakter.oppgave.Oppgavetype
import no.nav.tilleggsstonader.kontrakter.oppgave.OpprettOppgaveRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.StatusEnum
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.OppdaterPåVentRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.SettPåVentRequest
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.SettPåVentResponse
import no.nav.tilleggsstonader.kontrakter.oppgave.vent.TaAvVentRequest
import no.nav.tilleggsstonader.libs.feil.feil
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.fagsak.FagsakService
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakId
import no.nav.tilleggsstonader.sak.fagsak.domain.Fagsak
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FagsakId
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.OppgaveClientMockConfig
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockIsEnabled
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockUnleashService
import no.nav.tilleggsstonader.sak.klage.KlageService
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.domain.OppgaveMedMetadata
import no.nav.tilleggsstonader.sak.opplysninger.oppgave.dto.FinnOppgaveRequestDto
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PersonService
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdent
import no.nav.tilleggsstonader.sak.opplysninger.pdl.dto.PdlIdenter
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseService
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.lagNavn
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.pdlPersonKort
import no.nav.tilleggsstonader.sak.util.eksternOppgave
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
import java.util.UUID
import no.nav.tilleggsstonader.kontrakter.oppgave.PersonIdent as PersonIdentKontrakt

internal class OppgaveServiceTest {
    private val oppgaveClient = mockk<OppgaveClient>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    private val fagsakService = mockk<FagsakService>()
    private val oppgaveRepository = mockk<OppgaveRepository>()
    private val cacheManager = ConcurrentMapCacheManager()
    private val personService = mockk<PersonService>(relaxed = true)
    private val klageService = mockk<KlageService>(relaxed = true)
    private val ytelseService = mockk<YtelseService>()
    private val unleashService = mockUnleashService()

    private val oppgaveService =
        OppgaveService(
            oppgaveClient = oppgaveClient,
            fagsakService = fagsakService,
            oppgaveRepository = oppgaveRepository,
            arbeidsfordelingService = arbeidsfordelingService,
            cacheManager = cacheManager,
            personService = personService,
            klageService = klageService,
            ytelseService = ytelseService,
            unleashService = unleashService,
        )

    val opprettOppgaveDomainSlot = slot<OppgaveDomain>()

    @BeforeEach
    internal fun setUp() {
        opprettOppgaveDomainSlot.clear()
        every { oppgaveClient.finnOppgaveMedId(any()) } returns lagEksternTestOppgave()
        every { oppgaveRepository.insert(capture(opprettOppgaveDomainSlot)) } returns lagTestOppgave()
        every { oppgaveRepository.update(any()) } answers { firstArg() }
        every { oppgaveRepository.finnOppgaveMetadata(any()) } returns emptyList()
        every { oppgaveClient.finnMapper(any(), any()) } returns
            FinnMappeResponseDto(
                2,
                listOf(
                    MappeDto(OppgaveClientMockConfig.MAPPE_ID_KLAR, OppgaveMappe.KLAR.navn.first(), "4462"),
                    MappeDto(OppgaveClientMockConfig.MAPPE_ID_PÅ_VENT, OppgaveMappe.PÅ_VENT.navn.first(), "4462"),
                ),
            )
        mockkObject(OppgaveUtil)
        every { OppgaveUtil.utledBehandlesAvApplikasjon(any<Oppgavetype>()) } answers { callOriginal() }
        every { OppgaveUtil.skalHåndteresAvTSSak(any<Oppgavetype>()) } answers { callOriginal() }
    }

    @AfterEach
    fun tearDown() {
        unmockkObject(OppgaveUtil)
    }

    @Test
    fun `Opprett oppgave skal kunne opprettes uten behandlingId`() {
        val slot = slot<OpprettOppgaveRequest>()
        mockOpprettOppgave(slot)
        every { ytelseService.harAktivitetspengerIDagEllerFremITidEllerFeil(FNR) } returns false

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
        assertThat(slot.captured.personident).isEqualTo(PersonIdentKontrakt(ident = FNR))
        assertThat(slot.captured.behandlingstema).isEqualTo(Behandlingstema.PassAvBarn.value)
        assertThat(slot.captured.fristFerdigstillelse).isAfterOrEqualTo(LocalDate.now().plusDays(1))
        assertThat(slot.captured.aktivDato).isEqualTo(LocalDate.now())
        assertThat(slot.captured.tema).isEqualTo(Tema.TSO)
        assertThat(opprettOppgaveDomainSlot.captured.behandlingId).isEqualTo(BEHANDLING_ID)
    }

    @Nested
    inner class BehandlingstypeAktivitetspenger {
        @Test
        fun `skal sette behandlingstype aktivitetspenger når personen har aktivitetspenger`() {
            val slot = slot<OpprettOppgaveRequest>()
            mockOpprettOppgave(slot, gjelderAktivitetspenger = true)

            oppgaveService.opprettOppgave(BEHANDLING_ID, OpprettOppgave(oppgavetype = Oppgavetype.BehandleSak))

            assertThat(slot.captured.behandlingstype).isEqualTo(Behandlingstype.Aktivitetspenger.value)
        }

        @Test
        fun `skal ikke sette behandlingstype når personen ikke har aktivitetspenger`() {
            val slot = slot<OpprettOppgaveRequest>()
            mockOpprettOppgave(slot, gjelderAktivitetspenger = false)

            oppgaveService.opprettOppgave(BEHANDLING_ID, OpprettOppgave(oppgavetype = Oppgavetype.BehandleSak))

            assertThat(slot.captured.behandlingstype).isNull()
        }

        @Test
        fun `eksplisitt behandlingstype skal overstyre aktivitetspenger`() {
            val slot = slot<OpprettOppgaveRequest>()
            mockOpprettOppgave(slot, gjelderAktivitetspenger = true)

            oppgaveService.opprettOppgave(
                personIdent = FNR,
                stønadstype = Stønadstype.BARNETILSYN,
                behandlingId = BEHANDLING_ID,
                behandlingstype = Behandlingstype.Tilbakekreving,
                oppgave = OpprettOppgave(oppgavetype = Oppgavetype.BehandleSak),
            )

            assertThat(slot.captured.behandlingstype).isEqualTo(Behandlingstype.Tilbakekreving.value)
        }

        @Test
        fun `skal ikke slå opp ung-sak eller sette behandlingstype når toggle er av`() {
            unleashService.mockIsEnabled(Toggle.SKAL_MARKERE_AKTIVITETSPENGER_OPPGAVER, false)
            val slot = slot<OpprettOppgaveRequest>()
            mockOpprettOppgave(slot, gjelderAktivitetspenger = true)

            oppgaveService.opprettOppgave(BEHANDLING_ID, OpprettOppgave(oppgavetype = Oppgavetype.BehandleSak))

            assertThat(slot.captured.behandlingstype).isNull()
            verify(exactly = 0) { ytelseService.harAktivitetspengerIDagEllerFremITid(any()) }
        }

        @Test
        fun `skal bruke tolerant oppslag mot ung-sak når oppgaven har behandling`() {
            val slot = slot<OpprettOppgaveRequest>()
            mockOpprettOppgave(slot, gjelderAktivitetspenger = true)

            oppgaveService.opprettOppgave(BEHANDLING_ID, OpprettOppgave(oppgavetype = Oppgavetype.BehandleSak))

            verify(exactly = 1) { ytelseService.harAktivitetspengerIDagEllerFremITid(FNR) }
            verify(exactly = 0) { ytelseService.harAktivitetspengerIDagEllerFremITidEllerFeil(any()) }
        }
    }

    @Nested
    inner class ArvAvMarkeringFraForrigeOppgave {
        private val forrigeOppgave = lagTestOppgave().copy(gsakOppgaveId = 999L, type = Oppgavetype.BehandleSak)
        private val slot = slot<OpprettOppgaveRequest>()

        @BeforeEach
        fun setUp() {
            mockOpprettOppgave(slot, gjelderAktivitetspenger = false)
            every { oppgaveRepository.findByBehandlingId(BEHANDLING_ID) } returns listOf(forrigeOppgave)
        }

        @Test
        fun `skal arve markering fra forrige oppgave også når ung-sak sier nei`() {
            every { oppgaveClient.finnOppgaveMedId(999L) } returns
                lagEksternTestOppgave().copy(behandlingstype = Behandlingstype.Aktivitetspenger.value)

            oppgaveService.opprettOppgave(BEHANDLING_ID, OpprettOppgave(oppgavetype = Oppgavetype.GodkjenneVedtak))

            assertThat(slot.captured.behandlingstype).isEqualTo(Behandlingstype.Aktivitetspenger.value)
            verify(exactly = 0) { ytelseService.harAktivitetspengerIDagEllerFremITid(any()) }
        }

        @Test
        fun `skal ikke markere når forrige oppgave ble rettet til å ikke gjelde aktivitetspenger`() {
            every { ytelseService.harAktivitetspengerIDagEllerFremITid(FNR) } returns true
            every { oppgaveClient.finnOppgaveMedId(999L) } returns lagEksternTestOppgave().copy(behandlingstype = null)

            oppgaveService.opprettOppgave(BEHANDLING_ID, OpprettOppgave(oppgavetype = Oppgavetype.GodkjenneVedtak))

            assertThat(slot.captured.behandlingstype).isNull()
            verify(exactly = 0) { ytelseService.harAktivitetspengerIDagEllerFremITid(any()) }
        }

        @Test
        fun `skal ikke opprette oppgave eller slå opp ung-sak når forrige oppgave ikke kan hentes`() {
            every { oppgaveClient.finnOppgaveMedId(999L) } throws RuntimeException("Gosys feilet")

            assertThrows<RuntimeException> {
                oppgaveService.opprettOppgave(BEHANDLING_ID, OpprettOppgave(oppgavetype = Oppgavetype.GodkjenneVedtak))
            }

            verify(exactly = 0) { oppgaveClient.opprettOppgave(any()) }
            verify(exactly = 0) { ytelseService.harAktivitetspengerIDagEllerFremITid(any()) }
        }
    }

    @Nested
    inner class BehandlingstypeAktivitetspengerUtenBehandling {
        private val request = slot<OpprettOppgaveRequest>()

        @BeforeEach
        fun setUp() {
            every { arbeidsfordelingService.hentNavEnhetId(any(), any(), any()) } returns ENHETSNUMMER
            every { oppgaveClient.opprettOppgave(capture(request)) } returns GSAK_OPPGAVE_ID
        }

        private fun opprettJournalføringsoppgave() =
            oppgaveService.opprettOppgave(
                personIdent = FNR,
                stønadstype = Stønadstype.BARNETILSYN,
                behandlingId = null,
                oppgave = OpprettOppgave(oppgavetype = Oppgavetype.Journalføring),
            )

        @Test
        fun `skal sette behandlingstype aktivitetspenger ut fra person når oppgaven ikke har behandling`() {
            every { ytelseService.harAktivitetspengerIDagEllerFremITidEllerFeil(FNR) } returns true

            opprettJournalføringsoppgave()

            assertThat(request.captured.behandlingstype).isEqualTo(Behandlingstype.Aktivitetspenger.value)
        }

        @Test
        fun `skal ikke sette behandlingstype når ung-sak bekrefter at personen ikke har aktivitetspenger`() {
            every { ytelseService.harAktivitetspengerIDagEllerFremITidEllerFeil(FNR) } returns false

            opprettJournalføringsoppgave()

            assertThat(request.captured.behandlingstype).isNull()
        }

        @Test
        fun `skal ikke opprette oppgave når oppslag mot ung-sak feiler`() {
            every { ytelseService.harAktivitetspengerIDagEllerFremITidEllerFeil(FNR) } throws
                RuntimeException("ung-sak feilet")

            assertThrows<RuntimeException> { opprettJournalføringsoppgave() }

            verify(exactly = 0) { oppgaveClient.opprettOppgave(any()) }
        }
    }

    @Nested
    inner class OpprettOppgaveIMappe {
        @Test
        fun `Skal opprette behandle-sak-oppgave i Klar-mappe når ikke annet er spesifisert`() {
            val slot = slot<OpprettOppgaveRequest>()
            mockOpprettOppgave(slot)

            oppgaveService.opprettOppgave(BEHANDLING_ID, OpprettOppgave(oppgavetype = Oppgavetype.BehandleSak))

            assertThat(slot.captured.mappeId).isEqualTo(OppgaveClientMockConfig.MAPPE_ID_KLAR)
        }

        @Test
        fun `Skal opprette behandle-sak-oppgave i På vent-mappe når spesifisert`() {
            val slot = slot<OpprettOppgaveRequest>()
            mockOpprettOppgave(slot)

            oppgaveService.opprettOppgave(
                BEHANDLING_ID,
                OpprettOppgave(oppgavetype = Oppgavetype.BehandleSak, opprettIMappe = OppgaveMappe.PÅ_VENT),
            )

            assertThat(slot.captured.mappeId).isEqualTo(OppgaveClientMockConfig.MAPPE_ID_PÅ_VENT)
        }

        @Test
        fun `Skal opprette vurder henvendelse-oppgave uten mappe`() {
            val slot = slot<OpprettOppgaveRequest>()
            mockOpprettOppgave(slot)
            every { OppgaveUtil.utledBehandlesAvApplikasjon(Oppgavetype.VurderHenvendelse) } returns ""
            every { OppgaveUtil.skalHåndteresAvTSSak(Oppgavetype.VurderHenvendelse) } returns false

            oppgaveService.opprettOppgave(BEHANDLING_ID, OpprettOppgave(oppgavetype = Oppgavetype.VurderHenvendelse))

            assertThat(slot.captured.mappeId).isNull()
        }

        @Test
        fun `Skal ikke opprette i mappe når opprettIMappe er null`() {
            val slot = slot<OpprettOppgaveRequest>()
            mockOpprettOppgave(slot)

            oppgaveService.opprettOppgave(
                BEHANDLING_ID,
                OpprettOppgave(oppgavetype = Oppgavetype.BehandleSak, opprettIMappe = null),
            )

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
        val respons =
            oppgaveService.hentOppgaver(
                FinnOppgaveRequestDto(ident = null, enhet = Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD.enhetsnr),
            )

        assertThat(respons.antallTreffTotalt).isEqualTo(1)
        assertThat(
            respons.oppgaver
                .first()
                .oppgave.id,
        ).isEqualTo(GSAK_OPPGAVE_ID)
    }

    @Test
    fun `Ferdigstill oppgave`() {
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndStatus(any(), any(), any())
        } returns lagTestOppgave()
        every { oppgaveRepository.update(any()) } returns lagTestOppgave()
        val slot = slot<Long>()
        every { oppgaveClient.ferdigstillOppgave(capture(slot), any()) } just runs

        oppgaveService.ferdigstillBehandleOppgave(BEHANDLING_ID, Oppgavetype.BehandleSak)
        assertThat(slot.captured).isEqualTo(GSAK_OPPGAVE_ID)
    }

    @Test
    fun `Ferdigstill oppgave feiler fordi den ikke finner oppgave på behandlingen`() {
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndStatus(any(), any(), any())
        } returns null
        every { oppgaveRepository.insert(any()) } returns lagTestOppgave()

        assertThatThrownBy {
            oppgaveService.ferdigstillBehandleOppgave(
                BEHANDLING_ID,
                Oppgavetype.BehandleSak,
            )
        }.hasMessage("Finner ikke oppgave for behandling $BEHANDLING_ID type=BehandleSak")
            .isInstanceOf(java.lang.IllegalStateException::class.java)
    }

    @Test
    fun `Ferdigstill oppgave hvis oppgave ikke finnes - kaster ikke feil`() {
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndStatus(any(), any(), any())
        } returns null
        oppgaveService.ferdigstillOppgaveOgsåHvisFeilregistrert(
            BEHANDLING_ID,
            Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD.enhetsnr,
            Oppgavetype.BehandleSak,
        )
    }

    @Test
    fun `Ferdigstill oppgave - hvis oppgave finnes`() {
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndStatus(any(), any(), any())
        } returns lagTestOppgave()
        every { oppgaveRepository.update(any()) } returns lagTestOppgave()
        val slot = slot<Long>()
        every { oppgaveClient.ferdigstillOppgave(capture(slot), any()) } just runs

        oppgaveService.ferdigstillOppgaveOgsåHvisFeilregistrert(
            BEHANDLING_ID,
            Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD.enhetsnr,
            Oppgavetype.BehandleSak,
        )
        assertThat(slot.captured).isEqualTo(GSAK_OPPGAVE_ID)
    }

    @Test
    fun `Ferdigstill oppgave - oppgaven er feilregistrert`() {
        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndStatus(any(), any(), any())
        } returns lagTestOppgave()

        every { oppgaveClient.ferdigstillOppgave(any(), any()) } throws Exception("Oppgaven er allerede ferdigstilt")
        every { oppgaveClient.finnOppgaveMedId(any()) } returns lagEksternTestOppgave().copy(status = StatusEnum.FEILREGISTRERT)

        val slot = slot<OppgaveDomain>()

        every { oppgaveRepository.update(capture(slot)) } answers { firstArg() }

        oppgaveService.ferdigstillOppgaveOgsåHvisFeilregistrert(
            BEHANDLING_ID,
            Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD.enhetsnr,
            Oppgavetype.BehandleSak,
        )

        assertThat(slot.captured.status).isEqualTo(Oppgavestatus.FEILREGISTRERT)
    }

    @Test
    fun `Fordel oppgave skal tildele oppgave til saksbehandler`() {
        val oppgaveSlot = slot<Long>()
        val saksbehandlerSlot = slot<String>()
        val testOppgave = lagTestOppgave()

        every { oppgaveRepository.findByGsakOppgaveId(GSAK_OPPGAVE_ID) } returns testOppgave

        every {
            oppgaveClient.fordelOppgave(capture(oppgaveSlot), capture(saksbehandlerSlot), any(), any())
        } answers {
            lagEksternTestOppgave().copy(
                tilordnetRessurs = secondArg(),
                versjon = thirdArg<Int>() + 1,
            )
        }
        every {
            oppgaveRepository.update(testOppgave)
        } answers { firstArg() }

        oppgaveService.fordelOppgave(GSAK_OPPGAVE_ID, SAKSBEHANDLER_ID, 1)

        assertThat(oppgaveSlot.captured).isEqualTo(GSAK_OPPGAVE_ID)
        assertThat(saksbehandlerSlot.captured).isEqualTo(SAKSBEHANDLER_ID)

        verify(exactly = 1) {
            oppgaveRepository.update(
                match {
                    it.tilordnetSaksbehandler == SAKSBEHANDLER_ID
                },
            )
        }
    }

    @Nested
    inner class FordelAktivitetspengerOppgave {
        private val aktivitetspengerOppgave =
            eksternOppgave(
                id = GSAK_OPPGAVE_ID,
                versjon = 0,
                behandlingstype = Behandlingstype.Aktivitetspenger.value,
            )

        @BeforeEach
        fun setUpFordel() {
            every { oppgaveRepository.findByGsakOppgaveId(GSAK_OPPGAVE_ID) } returns lagTestOppgave()
            every { oppgaveClient.fordelOppgave(any(), any(), any(), any()) } returns aktivitetspengerOppgave
        }

        @Test
        fun `skal ikke tildele aktivitetspenger-oppgave til saksbehandler uten tilgang til aktivitetspenger`() {
            every { oppgaveClient.finnOppgaveMedId(GSAK_OPPGAVE_ID) } returns aktivitetspengerOppgave
            unleashService.mockIsEnabled(Toggle.KAN_BEHANDLE_AKTIVITETSPENGER, false)

            assertThatThrownBy {
                oppgaveService.fordelOppgave(GSAK_OPPGAVE_ID, SAKSBEHANDLER_ID, 1)
            }.hasMessageContaining("aktivitetspenger")

            verify(exactly = 0) { oppgaveRepository.update(any()) }
            verify(exactly = 0) { oppgaveClient.fordelOppgave(any(), any(), any(), any()) }
        }

        @Test
        fun `skal ikke tildele aktivitetspenger-klage til saksbehandler uten tilgang til aktivitetspenger`() {
            every { oppgaveClient.finnOppgaveMedId(GSAK_OPPGAVE_ID) } returns
                aktivitetspengerOppgave.copy(behandlingstype = Behandlingstype.AktivitetspengerKlage.value)
            unleashService.mockIsEnabled(Toggle.KAN_BEHANDLE_AKTIVITETSPENGER, false)

            assertThatThrownBy {
                oppgaveService.fordelOppgave(GSAK_OPPGAVE_ID, SAKSBEHANDLER_ID, 1)
            }.hasMessageContaining("aktivitetspenger")
        }

        @Test
        fun `skal tildele aktivitetspenger-oppgave til saksbehandler med tilgang til aktivitetspenger`() {
            every { oppgaveClient.finnOppgaveMedId(GSAK_OPPGAVE_ID) } returns aktivitetspengerOppgave
            unleashService.mockIsEnabled(Toggle.KAN_BEHANDLE_AKTIVITETSPENGER, true)

            oppgaveService.fordelOppgave(GSAK_OPPGAVE_ID, SAKSBEHANDLER_ID, 1)

            verify(exactly = 1) { oppgaveClient.fordelOppgave(GSAK_OPPGAVE_ID, SAKSBEHANDLER_ID, 1, any()) }
        }

        @Test
        fun `skal tildele vanlig oppgave til saksbehandler uten tilgang til aktivitetspenger`() {
            every { oppgaveClient.finnOppgaveMedId(GSAK_OPPGAVE_ID) } returns lagEksternTestOppgave()
            unleashService.mockIsEnabled(Toggle.KAN_BEHANDLE_AKTIVITETSPENGER, false)

            oppgaveService.fordelOppgave(GSAK_OPPGAVE_ID, SAKSBEHANDLER_ID, 1)

            verify(exactly = 1) { oppgaveClient.fordelOppgave(GSAK_OPPGAVE_ID, SAKSBEHANDLER_ID, 1, any()) }
        }

        @Test
        fun `skal alltid kunne tilbakestille fordeling av aktivitetspenger-oppgave`() {
            every { oppgaveClient.finnOppgaveMedId(GSAK_OPPGAVE_ID) } returns aktivitetspengerOppgave
            unleashService.mockIsEnabled(Toggle.KAN_BEHANDLE_AKTIVITETSPENGER, false)

            oppgaveService.fordelOppgave(GSAK_OPPGAVE_ID, null, 1)

            verify(exactly = 1) { oppgaveClient.fordelOppgave(GSAK_OPPGAVE_ID, null, 1, any()) }
        }

        @Test
        fun `skal ikke ta aktivitetspenger-oppgave av vent og tildele til saksbehandler uten tilgang`() {
            every { oppgaveClient.finnOppgaveMedId(GSAK_OPPGAVE_ID) } returns aktivitetspengerOppgave
            unleashService.mockIsEnabled(Toggle.KAN_BEHANDLE_AKTIVITETSPENGER, false)

            assertThatThrownBy {
                oppgaveService.taAvVent(taAvVentRequest(beholdOppgave = true))
            }.hasMessageContaining("aktivitetspenger")

            verify(exactly = 0) { oppgaveClient.taAvVent(any()) }
        }

        @Test
        fun `skal ikke sette aktivitetspenger-oppgave på vent og beholde den uten tilgang`() {
            every { oppgaveClient.finnOppgaveMedId(GSAK_OPPGAVE_ID) } returns aktivitetspengerOppgave
            unleashService.mockIsEnabled(Toggle.KAN_BEHANDLE_AKTIVITETSPENGER, false)

            assertThatThrownBy {
                oppgaveService.settPåVent(
                    SettPåVentRequest(GSAK_OPPGAVE_ID, null, 1 januar 2026, beholdOppgave = true, endretAvEnhetsnr = null),
                )
            }.hasMessageContaining("aktivitetspenger")
            assertThatThrownBy {
                oppgaveService.oppdaterPåVent(
                    OppdaterPåVentRequest(GSAK_OPPGAVE_ID, 1, null, 1 januar 2026, beholdOppgave = true, endretAvEnhetsnr = null),
                )
            }.hasMessageContaining("aktivitetspenger")

            verify(exactly = 0) { oppgaveClient.settPåVent(any()) }
            verify(exactly = 0) { oppgaveClient.oppdaterPåVent(any()) }
        }

        @Test
        fun `skal kunne ta aktivitetspenger-oppgave av vent uten å beholde den`() {
            every { oppgaveClient.finnOppgaveMedId(GSAK_OPPGAVE_ID) } returns aktivitetspengerOppgave
            every { oppgaveClient.taAvVent(any()) } returns SettPåVentResponse(GSAK_OPPGAVE_ID, 2)
            unleashService.mockIsEnabled(Toggle.KAN_BEHANDLE_AKTIVITETSPENGER, false)

            oppgaveService.taAvVent(taAvVentRequest(beholdOppgave = false))

            verify(exactly = 1) { oppgaveClient.taAvVent(any()) }
        }

        @Test
        fun `skal kunne ta aktivitetspenger-oppgave av vent og beholde den med tilgang`() {
            every { oppgaveClient.finnOppgaveMedId(GSAK_OPPGAVE_ID) } returns aktivitetspengerOppgave
            every { oppgaveClient.taAvVent(any()) } returns SettPåVentResponse(GSAK_OPPGAVE_ID, 2)
            unleashService.mockIsEnabled(Toggle.KAN_BEHANDLE_AKTIVITETSPENGER, true)

            oppgaveService.taAvVent(taAvVentRequest(beholdOppgave = true))

            verify(exactly = 1) { oppgaveClient.taAvVent(any()) }
        }

        private fun taAvVentRequest(beholdOppgave: Boolean) = TaAvVentRequest(GSAK_OPPGAVE_ID, null, beholdOppgave, 1 januar 2026, null)
    }

    @Test
    fun `skal legge til navn på oppgave hvis oppgaven har folkeregisterident`() {
        val oppgaveIdMedNavn = 1L

        every { personService.hentAktørIder(any()) } returns
            PdlIdenter(
                listOf(
                    PdlIdent(
                        "1",
                        false,
                        "FOLKEREGISTERIDENT",
                    ),
                ),
            )
        every { personService.hentPersonKortBolk(any()) } answers {
            firstArg<List<String>>().associateWith { pdlPersonKort(navn = lagNavn(fornavn = "fornavn$it")) }
        }
        every { oppgaveClient.hentOppgaver(any()) } returns
            FinnOppgaveResponseDto(
                2,
                listOf(
                    lagEksternTestOppgave().copy(
                        id = oppgaveIdMedNavn,
                        bruker = OppgaveBruker(ident = "1", type = OppgaveBrukerType.PERSON),
                    ),
                    lagEksternTestOppgave().copy(id = 3),
                ),
            )
        val oppgaver =
            oppgaveService
                .hentOppgaver(
                    FinnOppgaveRequestDto(
                        ident = "01010172272",
                        enhet = Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD.enhetsnr,
                    ),
                ).oppgaver

        verify(exactly = 1) { personService.hentPersonKortBolk(eq(listOf("1"))) }
        assertThat(oppgaver.single { it.oppgave.id == oppgaveIdMedNavn }.navn).contains("fornavn1 ")
        assertThat(
            oppgaver
                .filterNot { it.oppgave.id == oppgaveIdMedNavn }
                .map { it.navn },
        ).containsOnly("Mangler navn")
    }

    @Test
    fun `skal legge til behandlingId på oppgaver for å enklere kunne gå til behandling fra frontend`() {
        val oppgaveIdMedBehandling = 1L
        val behandlingId = BehandlingId.random()

        every { oppgaveClient.hentOppgaver(any()) } returns
            FinnOppgaveResponseDto(
                2,
                listOf(
                    lagEksternTestOppgave().copy(id = oppgaveIdMedBehandling),
                    lagEksternTestOppgave().copy(id = 2),
                ),
            )
        every { oppgaveRepository.finnOppgaveMetadata(any()) } answers {
            firstArg<List<Long>>()
                .filter { it == oppgaveIdMedBehandling }
                .map { OppgaveBehandlingMetadata(it, behandlingId.id, null) }
        }

        val oppgaver =
            oppgaveService
                .hentOppgaver(
                    FinnOppgaveRequestDto(ident = null, enhet = Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD.enhetsnr),
                ).oppgaver

        assertThat(oppgaver.single { it.oppgave.id == oppgaveIdMedBehandling }.behandlingId).isEqualTo(behandlingId.id)
        assertThat(oppgaver.single { it.oppgave.id != oppgaveIdMedBehandling }.behandlingId).isNull()
    }

    @Test
    fun `skal legge til behandlingId på klageoppgaver for å enklere kunne gå til behandling fra frontend`() {
        val oppgaveIdMedBehandling = 1L
        val behandlingIdKlage = UUID.randomUUID()

        every { oppgaveClient.hentOppgaver(any()) } returns
            FinnOppgaveResponseDto(
                1,
                listOf(
                    lagEksternTestOppgave().copy(
                        id = oppgaveIdMedBehandling,
                        behandlingstype = Behandlingstype.Klage.value,
                    ),
                ),
            )
        every { klageService.hentBehandlingIderForOppgaveIder(any()) } returns mapOf(oppgaveIdMedBehandling to behandlingIdKlage)

        val oppgaver =
            oppgaveService
                .hentOppgaver(
                    FinnOppgaveRequestDto(ident = null, enhet = Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD.enhetsnr),
                ).oppgaver

        assertThat(oppgaver.single { it.oppgave.id == oppgaveIdMedBehandling }.behandlingId).isEqualTo(behandlingIdKlage)
    }

    @Test
    fun `skal finne oppgaver på vent når`() {
        every { oppgaveClient.hentOppgaver(any()) } returns FinnOppgaveResponseDto(0, emptyList())

        oppgaveService.hentOppgaver(
            FinnOppgaveRequestDto(
                ident = null,
                oppgaverPåVent = true,
                enhet = Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD.enhetsnr,
            ),
        )

        verify { oppgaveClient.hentOppgaver(match { it.erUtenMappe == false && it.mappeId == OppgaveClientMockConfig.MAPPE_ID_PÅ_VENT }) }
    }

    @Test
    fun `skal bruke klarmappe når oppgaverPåVent er false`() {
        every { oppgaveClient.hentOppgaver(any()) } returns FinnOppgaveResponseDto(0, emptyList())

        oppgaveService.hentOppgaver(
            FinnOppgaveRequestDto(
                ident = null,
                oppgaverPåVent = false,
                enhet = Enhet.NAV_ARBEID_OG_YTELSER_TILLEGGSSTØNAD.enhetsnr,
            ),
        )

        verify { oppgaveClient.hentOppgaver(match { it.erUtenMappe == false }) }
    }

    @Test
    fun `lagBeskrivelseMelding uten eksisterende beskrivelse returnerer kun ny tekst`() {
        val endring = "Endring gjort i oppgaven"
        val nåværendeBeskrivelse = null

        val formattertBeskrivelse = oppgaveService.lagBeskrivelseMelding(endring, nåværendeBeskrivelse)
        assertThat(formattertBeskrivelse)
            .startsWith("---")
            .endsWith(endring)
    }

    @Test
    fun `lagBeskrivelseMelding med eksisterende beskrivelse konkatinerer beskrivelsene`() {
        val endring = "Endring gjort i oppgaven"
        val nåværendeBeskrivelse = "Tidligere beskrivelse"

        val formattertBeskrivelse = oppgaveService.lagBeskrivelseMelding(endring, nåværendeBeskrivelse)
        assertThat(formattertBeskrivelse)
            .startsWith("---")
            .endsWith("$endring\n\n$nåværendeBeskrivelse")
    }

    @Test
    fun `finnSisteBehandlingsoppgaveForBehandling returnerer siste BehandleSak oppgave`() {
        val behandleSakOppgave = lagTestOppgave().copy(type = Oppgavetype.BehandleSak)
        every { oppgaveRepository.findByBehandlingId(any()) } returns
            listOf(
                behandleSakOppgave,
                lagTestOppgave().copy(type = Oppgavetype.VurderHenvendelse),
            )

        assertThat(oppgaveService.finnSisteBehandlingsoppgaveForBehandling(BEHANDLING_ID))
            .isEqualTo(behandleSakOppgave)
    }

    private fun mockOpprettOppgave(
        slot: CapturingSlot<OpprettOppgaveRequest>,
        gjelderAktivitetspenger: Boolean = false,
    ) {
        every { fagsakService.hentFagsakForBehandling(BEHANDLING_ID) } returns lagTestFagsak()
        every { ytelseService.harAktivitetspengerIDagEllerFremITid(FNR) } returns gjelderAktivitetspenger
        every { oppgaveRepository.findByBehandlingId(any()) } returns emptyList()

        every {
            oppgaveRepository.findByBehandlingIdAndTypeAndStatus(any(), any(), any())
        } returns null
        every { arbeidsfordelingService.hentNavEnhetId(any(), any(), any()) } returns ENHETSNUMMER
        every { oppgaveClient.opprettOppgave(capture(slot)) } answers {
            val oppgaveRequest: OpprettOppgaveRequest = firstArg()
            if (oppgaveRequest.enhetsnummer == null) {
                feil("Fant ingen gyldig arbeidsfordeling for oppgaven")
            } else {
                GSAK_OPPGAVE_ID
            }
        }
    }

    private fun lagTestFagsak(): Fagsak =
        fagsak(
            id = FAGSAK_ID,
            stønadstype = Stønadstype.BARNETILSYN,
            eksternId = EksternFagsakId(FAGSAK_EKSTERN_ID, FAGSAK_ID),
            identer = setOf(PersonIdent(ident = FNR)),
        )

    private fun lagTestOppgave(): OppgaveDomain =
        OppgaveDomain(
            behandlingId = BEHANDLING_ID,
            tilbakekrevingBehandlingId = null,
            type = Oppgavetype.BehandleSak,
            gsakOppgaveId = GSAK_OPPGAVE_ID,
            tilordnetSaksbehandler = null,
            status = Oppgavestatus.ÅPEN,
            tildeltEnhetsnummer = ENHETSNUMMER,
            enhetsmappeId = OppgaveClientMockConfig.MAPPE_ID_KLAR,
        )

    private fun lagEksternTestOppgave(): Oppgave = eksternOppgave(id = GSAK_OPPGAVE_ID, versjon = 0)

    private fun lagFinnOppgaveResponseDto(): FinnOppgaveResponseDto =
        FinnOppgaveResponseDto(
            antallTreffTotalt = 1,
            oppgaver = listOf(lagEksternTestOppgave()),
        )

    private val OppgaveMedMetadata.behandlingId get() = this.metadata?.behandlingMetadata?.behandlingId
    private val OppgaveMedMetadata.navn get() = this.metadata?.navn

    companion object {
        private val FAGSAK_ID = FagsakId.fromString("1242f220-cad3-4640-95c1-190ec814c91e")
        private const val FAGSAK_EKSTERN_ID = 98765L
        private const val GSAK_OPPGAVE_ID = 12345L
        private val BEHANDLING_ID = BehandlingId.fromString("1c4209bd-3217-4130-8316-8658fe300a84")
        private const val ENHETSNUMMER = "4462"
        private const val FNR = "11223312345"
        private const val SAKSBEHANDLER_ID = "Z999999"
    }
}
