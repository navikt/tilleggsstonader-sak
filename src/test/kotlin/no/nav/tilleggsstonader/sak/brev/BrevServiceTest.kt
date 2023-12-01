package no.nav.tilleggsstonader.sak.brev

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
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
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.clearBrukerContext
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.mockBrukerContext
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.norskFormat
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus.BAD_REQUEST
import java.time.LocalDate

internal class BrevServiceTest {

    private val fagsak = fagsak(setOf(PersonIdent("12345678910")))
    private val behandling = behandling(fagsak)

    private val vedtaksbrevRepository = mockk<VedtaksbrevRepository>()
    private val familieDokumentClient = mockk<FamilieDokumentClient>()

    private val brevService = BrevService(vedtaksbrevRepository, familieDokumentClient)

    private val vedtaksbrev: Vedtaksbrev = lagVedtaksbrev("malnavn")
    private val beslutterNavn = "456"

    @BeforeEach
    fun setUp() {
        mockBrukerContext(beslutterNavn)
    }

    @AfterEach
    fun tearDown() {
        clearBrukerContext()
    }

    @Test
    internal fun `lagBeslutterBrev - skal kaste feil hvis behandlingen ikke har riktig steg`() {
        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev

        val feil = catchThrowableOfType<Feil> {
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

        val feilFerdigstilt = catchThrowableOfType<Feil> {
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

        val feilUtredes = catchThrowableOfType<Feil> {
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

        val feil = catchThrowableOfType<Feil> {
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
        every { familieDokumentClient.genererPdf(any()) } returns "brev".toByteArray()

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

    private val behandlingForBeslutter = behandling(
        fagsak,
        status = BehandlingStatus.FATTER_VEDTAK,
        steg = StegType.BESLUTTE_VEDTAK,
    )

    private val behandlingForSaksbehandler = behandling(
        fagsak,
        status = BehandlingStatus.UTREDES,
        steg = StegType.SEND_TIL_BESLUTTER,
    )

    private fun lagVedtaksbrev(brevmal: String, saksbehandlerIdent: String = "123") = Vedtaksbrev(
        behandlingId = behandling.id,
        saksbehandlerHtml = "Brev med $BESLUTTER_SIGNATUR_PLACEHOLDER og $BREVDATO_PLACEHOLDER",
        saksbehandlersignatur = "Saksbehandler Signatur",
        besluttersignatur = null,
        beslutterPdf = null,
        saksbehandlerIdent = saksbehandlerIdent,
        beslutterIdent = null,
    )

    @Test
    fun `skal kaste feil hvis saksbehandlerHtml ikke inneholder placeholder for saksbehandlersignatur`() {
        val feilmelding = catchThrowableOfType<Feil> {
            brevService.lagSaksbehandlerBrev(saksbehandling(fagsak, behandlingForSaksbehandler), "html uten placeholder")
        }.message
        assertThat(feilmelding).isEqualTo("Brev-HTML mangler placeholder for saksbehandlersignatur")
    }

    @Test
    fun `skal kaste feil hvis saksbehandlerHtml ikke inneholder placeholder for besluttersignatur`() {
        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev.copy(saksbehandlerHtml = "html uten placeholder")

        val feilmelding = catchThrowableOfType<Feil> {
            brevService.forhåndsvisBeslutterBrev(saksbehandling(fagsak, behandlingForBeslutter))
        }.message
        assertThat(feilmelding).isEqualTo("Brev-HTML mangler placeholder for besluttersignatur")
    }

    @Test
    fun `Skal erstatte placeholder med besluttersignatur`() {
        val htmlSlot = slot<String>()

        every { vedtaksbrevRepository.findByIdOrThrow(any()) } returns vedtaksbrev.copy(saksbehandlerHtml = "html med placeholder $BESLUTTER_SIGNATUR_PLACEHOLDER, vedtaksdato $BREVDATO_PLACEHOLDER og en liten avslutning")
        every { vedtaksbrevRepository.update(any()) } returns vedtaksbrev
        every { familieDokumentClient.genererPdf(capture(htmlSlot)) } returns "123".toByteArray()

        brevService.forhåndsvisBeslutterBrev(saksbehandling(fagsak, behandlingForBeslutter))

        val vedtaksdato = LocalDate.now().norskFormat()

        assertThat(htmlSlot.captured).isEqualTo("html med placeholder $beslutterNavn, vedtaksdato $vedtaksdato og en liten avslutning")
    }

    @Test
    internal fun `skal oppdatere vedtaksbrev med nytt tidspunkt`() {
        val vedtaksbrevSlot = slot<Vedtaksbrev>()
        every { vedtaksbrevRepository.existsById(any()) } returns true
        every { vedtaksbrevRepository.update(capture(vedtaksbrevSlot)) } answers { firstArg() }
        every { familieDokumentClient.genererPdf(any()) } returns "123".toByteArray()

        val now = SporbarUtils.now()
        brevService.lagSaksbehandlerBrev(saksbehandling(fagsak, behandling), "html med $SAKSBEHANDLER_SIGNATUR_PLACEHOLDER")
        assertThat(vedtaksbrevSlot.captured.opprettetTid).isAfterOrEqualTo(now)
    }
}
