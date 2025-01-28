package no.nav.tilleggsstonader.sak.brev.vedtaksbrev

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.BrevUtil.BESLUTTER_SIGNATUR_PLACEHOLDER
import no.nav.tilleggsstonader.sak.brev.BrevUtil.BREVDATO_PLACEHOLDER
import no.nav.tilleggsstonader.sak.brev.BrevUtil.SAKSBEHANDLER_SIGNATUR_PLACEHOLDER
import no.nav.tilleggsstonader.sak.fagsak.domain.PersonIdent
import no.nav.tilleggsstonader.sak.infrastruktur.database.Fil
import no.nav.tilleggsstonader.sak.infrastruktur.database.SporbarUtils
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.BehandlerRolle
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.journalføring.FamilieDokumentClient
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.mockBrukerContext
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.testWithBrukerContext
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST

internal class BrevServiceTest {
    private val fagsak = fagsak(setOf(PersonIdent("12345678910")))
    private val behandling = behandling(fagsak)

    private val vedtaksbrevRepository = mockk<VedtaksbrevRepository>()
    private val familieDokumentClient = mockk<FamilieDokumentClient>()
    private val tilgangService = mockk<TilgangService>()

    private val brevService = BrevService(vedtaksbrevRepository, familieDokumentClient, tilgangService)

    private val vedtaksbrev: Vedtaksbrev = lagVedtaksbrev()
    private val beslutterNavn = "456"
    private val pdfHtmlSlot = slot<String>()

    @BeforeEach
    fun setUp() {
        mockBrukerContext(beslutterNavn)
        pdfHtmlSlot.clear()
        every { tilgangService.harTilgangTilRolle(any()) } returns true
        every { familieDokumentClient.genererPdf(capture(pdfHtmlSlot)) } returns "brev".toByteArray()
    }

    @AfterEach
    fun tearDown() {
        clearBrukerContext()
    }

    @Test
    internal fun `lagBeslutterBrev - skal kaste feil hvis behandlingen ikke har riktig steg`() {
        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev

        val feil =
            catchThrowableOfType<Feil> {
                brevService.lagEndeligBeslutterbrev(
                    saksbehandling(
                        fagsak,
                        behandlingForBeslutter.copy(steg = StegType.VILKÅR),
                    ),
                )
            }
        assertThat(feil.message).contains("Behandling er i feil steg")
        assertThat(feil.httpStatus).isEqualTo(BAD_REQUEST)
    }

    @Test
    internal fun `lagBeslutterBrev - skal kaste feil hvis behandlingen ikke har riktig status`() {
        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev

        val feilFerdigstilt =
            catchThrowableOfType<Feil> {
                brevService.lagEndeligBeslutterbrev(
                    saksbehandling(
                        fagsak,
                        behandlingForBeslutter.copy(
                            status =
                                BehandlingStatus.FERDIGSTILT,
                        ),
                    ),
                )
            }
        assertThat(feilFerdigstilt.httpStatus).isEqualTo(BAD_REQUEST)
        assertThat(feilFerdigstilt.message).contains("Behandling er i feil steg")

        val feilUtredes =
            catchThrowableOfType<Feil> {
                brevService.lagEndeligBeslutterbrev(
                    saksbehandling(
                        fagsak,
                        behandling.copy(status = BehandlingStatus.UTREDES),
                    ),
                )
            }
        assertThat(feilUtredes.httpStatus).isEqualTo(BAD_REQUEST)
        assertThat(feilUtredes.message).contains("Behandling er i feil steg")
    }

    @Test
    internal fun `skal kaste feil når det finnes beslutterpdf i forveien`() {
        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev.copy(beslutterPdf = Fil("123".toByteArray()))

        val feil =
            catchThrowableOfType<Feil> {
                brevService.lagEndeligBeslutterbrev(
                    saksbehandling(
                        fagsak,
                        behandlingForBeslutter,
                    ),
                )
            }
        assertThat(feil.message).isEqualTo("Det finnes allerede et beslutterbrev")
    }

    @Test
    internal fun `skal lage brev med innlogget beslutterident beslutterident `() {
        val beslutterIdent = SikkerhetContext.hentSaksbehandler()
        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev.copy(beslutterIdent = "tilfeldigvisFeilIdent")
        val brevSlot = slot<Vedtaksbrev>()
        every { vedtaksbrevRepository.update(capture(brevSlot)) } returns mockk()

        // Når
        brevService.lagEndeligBeslutterbrev(saksbehandling(fagsak, behandlingForBeslutter))

        assertThat(beslutterIdent).isNotNull()
        assertThat(brevSlot.captured.beslutterIdent).isEqualTo(beslutterIdent)
    }

    @Test
    internal fun `lagSaksbehandlerBrev skal kaste feil når behandling er låst for videre behandling`() {
        catchThrowableOfType<Feil> {
            brevService
                .lagSaksbehandlerBrev(
                    saksbehandling(
                        fagsak,
                        behandlingForBeslutter.copy(
                            status =
                                BehandlingStatus.FERDIGSTILT,
                        ),
                    ),
                    "html",
                )
        }
    }

    @Test
    internal fun `hentBesluttetBrev skal feile dersom pdf mangler`() {
        every { vedtaksbrevRepository.findByIdOrThrow(behandling.id) } returns vedtaksbrev.copy(beslutterPdf = null)

        assertThatThrownBy {
            brevService.hentBesluttetBrev(behandling.id)
        }.hasMessage("Fant ikke besluttet pdf")
    }

    private val behandlingForBeslutter =
        behandling(
            fagsak,
            status = BehandlingStatus.FATTER_VEDTAK,
            steg = StegType.BESLUTTE_VEDTAK,
        )

    private val behandlingForSaksbehandler =
        behandling(
            fagsak,
            status = BehandlingStatus.UTREDES,
            steg = StegType.SEND_TIL_BESLUTTER,
        )

    private fun lagVedtaksbrev(saksbehandlerIdent: String = "123") =
        Vedtaksbrev(
            behandlingId = behandling.id,
            saksbehandlerHtml = "Brev med $BESLUTTER_SIGNATUR_PLACEHOLDER og $BREVDATO_PLACEHOLDER",
            saksbehandlersignatur = "Saksbehandler Signatur",
            besluttersignatur = null,
            beslutterPdf = null,
            saksbehandlerIdent = saksbehandlerIdent,
            beslutterIdent = null,
        )

    @Nested
    inner class ForhåndsvisBeslutterBrev {
        /*
         * skal generere brev med tom besluttersignatur hvis man ikke har beslutterrolle - man skal kunne se
         * brevet som saksbehandler opprettet
         */
        @Test
        fun `skal generere brev med tom besluttersignatur hvis man ikke har beslutterrolle`() {
            every { tilgangService.harTilgangTilRolle(BehandlerRolle.BESLUTTER) } returns false
            every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev

            testWithBrukerContext("sak001") {
                brevService.forhåndsvisBeslutterBrev(saksbehandling())
            }

            assertThat(pdfHtmlSlot.captured).doesNotContain("sak001")
        }

        @Test
        fun `skal generere brev besluttersignatur hvis man har beslutterrolle`() {
            every { tilgangService.harTilgangTilRolle(BehandlerRolle.BESLUTTER) } returns true
            every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev

            testWithBrukerContext("sak001") {
                brevService.forhåndsvisBeslutterBrev(saksbehandling())
            }

            assertThat(pdfHtmlSlot.captured).contains("sak001")
        }
    }

    @Test
    fun `skal kaste feil hvis saksbehandlerHtml ikke inneholder placeholder for saksbehandlersignatur`() {
        val feilmelding =
            catchThrowableOfType<Feil> {
                brevService.lagSaksbehandlerBrev(
                    saksbehandling(fagsak, behandlingForSaksbehandler),
                    "html uten placeholder",
                )
            }.message
        assertThat(feilmelding).isEqualTo("Brev-HTML mangler placeholder for saksbehandlersignatur")
    }

    @Test
    fun `skal kaste feil hvis saksbehandlerHtml ikke inneholder placeholder for besluttersignatur`() {
        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev.copy(saksbehandlerHtml = "html uten placeholder")

        val feilmelding =
            catchThrowableOfType<Feil> {
                brevService.forhåndsvisBeslutterBrev(saksbehandling(fagsak, behandlingForBeslutter))
            }.message
        assertThat(feilmelding).isEqualTo("Brev-HTML mangler placeholder for besluttersignatur")
    }

    @Test
    fun `Skal erstatte placeholder med besluttersignatur`() {
        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns
            vedtaksbrev.copy(
                saksbehandlerHtml =
                    """
                    html med placeholder $BESLUTTER_SIGNATUR_PLACEHOLDER, 
                    vedtaksdato $BREVDATO_PLACEHOLDER 
                    og en liten avslutning
                    """.trimIndent(),
            )
        every { vedtaksbrevRepository.update(any()) } returns vedtaksbrev

        brevService.forhåndsvisBeslutterBrev(saksbehandling(fagsak, behandlingForBeslutter))

        val vedtaksdato = osloDateNow().norskFormat()

        assertThat(pdfHtmlSlot.captured)
            .isEqualTo(
                """
                html med placeholder $beslutterNavn, 
                vedtaksdato $vedtaksdato 
                og en liten avslutning
                """.trimIndent(),
            )
    }

    @Test
    internal fun `skal oppdatere vedtaksbrev med nytt tidspunkt`() {
        val vedtaksbrevSlot = slot<Vedtaksbrev>()
        every { vedtaksbrevRepository.existsById(any()) } returns true
        every { vedtaksbrevRepository.update(capture(vedtaksbrevSlot)) } answers { firstArg() }

        val now = SporbarUtils.now()
        brevService.lagSaksbehandlerBrev(
            saksbehandling(fagsak, behandling),
            "html med $SAKSBEHANDLER_SIGNATUR_PLACEHOLDER",
        )
        assertThat(vedtaksbrevSlot.captured.opprettetTid).isAfterOrEqualTo(now)
    }
}
