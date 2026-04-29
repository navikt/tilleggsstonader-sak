package no.nav.tilleggsstonader.sak.brev.kjørelistebehandling

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.brev.kjørelistebrev.KjørelisteBehandlingBrevRequest
import no.nav.tilleggsstonader.sak.brev.kjørelistebrev.KjørelisteBehandlingBrevService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.Toggle
import no.nav.tilleggsstonader.sak.integrasjonstest.gjennomførKjørelisteBehandlingManuelt
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.interntVedtak.HtmlifyClient
import no.nav.tilleggsstonader.sak.util.FileUtil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.DagligReiseVedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.finnSatserBruktIBeregning
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.oppsummerBeregningPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.dto.FaktaDelperiodePrivatBilDto
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.TestRestTemplate
import org.springframework.boot.resttestclient.postForEntity
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import java.net.URI
import java.time.LocalDate

class GenererKjørelistebehandlingBrevTest : CleanDatabaseIntegrationTest() {
    val fom = 29 desember 2025
    val tom = 18 januar 2026

    @Autowired
    private lateinit var dagligReiseVedtakService: DagligReiseVedtakService

    @Autowired
    private lateinit var kjørelistebehandlingBrevService: KjørelisteBehandlingBrevService

    @Test
    fun `lag html og pdf`() {
        every { unleashService.isEnabled(Toggle.KAN_BEHANDLE_PRIVAT_BIL) } returns true

        if (!FileUtil.SKRIV_TIL_FIL) return

        val kjørelisteBehandlingId = gjennomførBehandlingsløp()
        val vedtaksdata = dagligReiseVedtakService.hentInnvilgelseEllerOpphørVedtak(kjørelisteBehandlingId).data

        val oppsummertBeregningsresultat =
            oppsummerBeregningPrivatBil(
                beregningsresultatPrivatBil = vedtaksdata.beregningsresultat.privatBil!!,
                rammevedtak = vedtaksdata.rammevedtakPrivatBil!!,
            )

        val req =
            KjørelisteBehandlingBrevRequest(
                beregning = oppsummertBeregningsresultat,
                navn = "Navn",
                ident = "Ident",
                behandlendeEnhet = "NAV Arbeid og ytelser",
                behandletDato = LocalDate.now(),
                satser = oppsummertBeregningsresultat.finnSatserBruktIBeregning(),
            )

        val html = lagHtmlifyClient().genererKjørelisteBehandlingBrev(req)
        FileUtil.skrivTilFil("privatBil/kjoreliste_behandling_brev.html", html)
        generatePdf(html, "privatBil/kjoreliste_behandling_brev.pdf")
    }

    private fun gjennomførBehandlingsløp(): BehandlingId {
        val førstegangsBehandlingContext =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            ) {
                defaultDagligReisePrivatBilTsoTestdata(
                    fom = fom,
                    tom = tom,
                    delperioder =
                        listOf(
                            FaktaDelperiodePrivatBilDto(
                                fom = fom,
                                tom = 4 januar 2026,
                                reisedagerPerUke = 3,
                                bompengerPerDag = 50,
                                fergekostnadPerDag = null,
                            ),
                            FaktaDelperiodePrivatBilDto(
                                fom = 5 januar 2026,
                                tom = 11 januar 2026,
                                reisedagerPerUke = 2,
                                bompengerPerDag = 50,
                                fergekostnadPerDag = null,
                            ),
                            FaktaDelperiodePrivatBilDto(
                                fom = 12 januar 2026,
                                tom = 18 januar 2026,
                                reisedagerPerUke = 2,
                                bompengerPerDag = null,
                                fergekostnadPerDag = 100,
                            ),
                        ),
                )

                sendInnKjøreliste {
                    periode = Datoperiode(fom, tom)
                    kjørteDager =
                        listOf(
                            29 desember 2025 to 50,
                            30 desember 2025 to 50,
                            2 januar 2026 to 50,
                            5 januar 2026 to 80,
                            6 januar 2026 to 80,
                            12 januar 2026 to 20,
                            13 januar 2026 to 20,
                        )
                }
            }

        val førstegangsBehandling = testoppsettService.hentBehandling(førstegangsBehandlingContext.behandlingId)

        val kjørelisteBehandling =
            testoppsettService
                .hentBehandlinger(førstegangsBehandling.fagsakId)
                .single { it.type == BehandlingType.KJØRELISTE }

        gjennomførKjørelisteBehandlingManuelt(kjørelisteBehandling, tilSteg = StegType.SIMULERING)

        return kjørelisteBehandling.id
    }

    private fun lagHtmlifyClient(): HtmlifyClient {
        val restTemplate = TestRestTemplate().restTemplate
        restTemplate.messageConverters =
            listOf(
                StringHttpMessageConverter(),
                JacksonJsonHttpMessageConverter(JsonMapperProvider.jsonMapper),
            )
        return HtmlifyClient(URI.create("http://localhost:8001"), restTemplate)
    }

    private fun generatePdf(
        html: String,
        name: String,
    ) {
        val url = "https://familie-dokument.intern.dev.nav.no/api/html-til-pdf"
        val request =
            HttpEntity(
                html,
                HttpHeaders().apply {
                    accept = listOf(MediaType.APPLICATION_PDF)
                },
            )
        val pdf = TestRestTemplate().postForEntity<ByteArray>(url, request).body!!
        FileUtil.skrivTilFil(name, pdf)
    }
}
