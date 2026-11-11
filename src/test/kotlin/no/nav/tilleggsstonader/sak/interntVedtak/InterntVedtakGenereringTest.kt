package no.nav.tilleggsstonader.sak.interntVedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.JsonMapperProvider
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.interntVedtak.InterntVedtakTestdata.behandlingId
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlagService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.util.FileUtil
import no.nav.tilleggsstonader.sak.util.FileUtil.SKRIV_TIL_FIL
import no.nav.tilleggsstonader.sak.util.FileUtil.assertFileJsonIsEqual
import no.nav.tilleggsstonader.sak.util.FileUtil.skrivTilFil
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI

class InterntVedtakGenereringTest {
    private val behandlingService = mockk<BehandlingService>()
    private val totrinnskontrollService = mockk<TotrinnskontrollService>()
    private val vilkårperiodeService = mockk<VilkårperiodeService>()
    private val søknadService = mockk<SøknadService>()
    private val faktaGrunnlagService = mockk<FaktaGrunnlagService>()
    private val barnService = mockk<BarnService>()
    private val vilkårService = mockk<VilkårService>()
    private val vedtakService = mockk<VedtakService>()

    val service =
        InterntVedtakService(
            behandlingService = behandlingService,
            totrinnskontrollService = totrinnskontrollService,
            vilkårperiodeService = vilkårperiodeService,
            søknadService = søknadService,
            faktaGrunnlagService = faktaGrunnlagService,
            barnService = barnService,
            vilkårService = vilkårService,
            vedtakService = vedtakService,
        )

    @BeforeEach
    fun setUp() {
        every { totrinnskontrollService.hentTotrinnskontroll(behandlingId) } returns InterntVedtakTestdata.totrinnskontroll
        every { søknadService.hentSøknadMetadata(behandlingId) } returns InterntVedtakTestdata.søknadMetadata
    }

    @ParameterizedTest
    @EnumSource(
        value = Stønadstype::class,
        names = [
            "BARNETILSYN",
            "LÆREMIDLER",
            "BOUTGIFTER",
            "DAGLIG_REISE_TSO",
            "DAGLIG_REISE_TSR",
            "REISE_TIL_SAMLING_TSO",
            "REISE_TIL_SAMLING_TSR",
            "FLYTTING_TSO",
            "FLYTTING_TSR",
        ],
    )
    fun `json til htmlify er riktig`(stønadstype: Stønadstype) {
        mock(stønadstype)
        val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)
        assertFileJsonIsEqual("interntVedtak/$stønadstype/internt_vedtak.json", interntVedtak)
    }

    /**
     * Bruk syntetiske testdata når du genererer HTML og PDF.
     * Start htmlify lokalt, og kjør:
     *     SKRIV_TIL_FIL=true HTMLIFY_URI=http://localhost:8001 ./gradlew test --tests 'no.nav.tilleggsstonader.sak.interntVedtak.InterntVedtakGenereringTest.lag html og pdf'
     * Eller bruk htmlify i dev:
     *     SKRIV_TIL_FIL=true HTMLIFY_URI=https://tilleggsstonader-htmlify.intern.dev.nav.no ./gradlew test --tests 'no.nav.tilleggsstonader.sak.interntVedtak.InterntVedtakGenereringTest.lag html og pdf'
     * PDF-genereringen kaller eksisterende PDF-tjeneste i dev og krever intern nettverkstilgang.
     * Formatter HTML-filen etter generering for å unngå stor diff.
     */
    @ParameterizedTest
    @EnumSource(
        value = Stønadstype::class,
        names = [
            // Kommenter ut stønadstypene du ikke ønsker å generere internt vedtak for
            "BARNETILSYN",
            "LÆREMIDLER",
            "BOUTGIFTER",
            "DAGLIG_REISE_TSO",
            "DAGLIG_REISE_TSR",
            "REISE_TIL_SAMLING_TSO",
            "REISE_TIL_SAMLING_TSR",
            "FLYTTING_TSO",
            "FLYTTING_TSR",
        ],
    )
    fun `lag html og pdf`(stønadstype: Stønadstype) {
        if (!SKRIV_TIL_FIL) {
            return // Skal ikke generere opp interne vedtak hvis SKRIV_TIL_FIL er false
        }
        mock(stønadstype)
        val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)
        val html = lagHtmlifyClient().generateHtml(interntVedtak)
        skrivTilFil("interntVedtak/$stønadstype/internt_vedtak.html", html)
        generatePdf(html, "interntVedtak/$stønadstype/internt_vedtak.pdf")
    }

    private fun mock(type: Stønadstype) {
        when (type) {
            Stønadstype.BARNETILSYN -> mockPassAvBarn()
            Stønadstype.LÆREMIDLER -> mockLæremidler()
            Stønadstype.BOUTGIFTER -> mockBoutgifter()
            Stønadstype.DAGLIG_REISE_TSO -> mockDagligReiseTso()
            Stønadstype.DAGLIG_REISE_TSR -> mockDagligReiseTsr()
            Stønadstype.REISE_TIL_SAMLING_TSO -> mockReiseTilSamlingTso()
            Stønadstype.REISE_TIL_SAMLING_TSR -> mockReiseTilSamlingTsr()
            Stønadstype.FLYTTING_TSO,
            Stønadstype.FLYTTING_TSR,
            -> mockFlytting(type)

            Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO,
            Stønadstype.REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR,
            -> TODO("mockReiseOppstartAvslutningHjemreise")
        }
    }

    private fun mockPassAvBarn() {
        every { behandlingService.hentSaksbehandling(behandlingId) } returns InterntVedtakTestdata.PassAvBarn.behandling
        every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns InterntVedtakTestdata.PassAvBarn.vilkårperioder
        every { faktaGrunnlagService.hentGrunnlagsdata(behandlingId) } returns InterntVedtakTestdata.PassAvBarn.grunnlagsdata
        every { barnService.finnBarnPåBehandling(behandlingId) } returns InterntVedtakTestdata.PassAvBarn.behandlingBarn
        every { vilkårService.hentVilkår(behandlingId) } returns InterntVedtakTestdata.PassAvBarn.vilkår
        every { vedtakService.hentVedtak(behandlingId) } returns InterntVedtakTestdata.PassAvBarn.vedtak
    }

    private fun mockLæremidler() {
        every { behandlingService.hentSaksbehandling(behandlingId) } returns InterntVedtakTestdata.Læremidler.behandling
        every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns InterntVedtakTestdata.Læremidler.vilkårperioder
        every { faktaGrunnlagService.hentGrunnlagsdata(behandlingId) } returns InterntVedtakTestdata.Læremidler.grunnlagsdata
        every { barnService.finnBarnPåBehandling(behandlingId) } returns emptyList()
        every { vilkårService.hentVilkår(behandlingId) } returns emptyList()
        every { vedtakService.hentVedtak(behandlingId) } returns InterntVedtakTestdata.Læremidler.innvilgetVedtak
    }

    private fun mockBoutgifter() {
        every { behandlingService.hentSaksbehandling(behandlingId) } returns InterntVedtakTestdata.Boutgifter.behandling
        every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns InterntVedtakTestdata.Boutgifter.vilkårperioder
        every { faktaGrunnlagService.hentGrunnlagsdata(behandlingId) } returns InterntVedtakTestdata.Boutgifter.grunnlagsdata
        every { barnService.finnBarnPåBehandling(behandlingId) } returns emptyList()
        every { vilkårService.hentVilkår(behandlingId) } returns InterntVedtakTestdata.Boutgifter.vilkår
        every { vedtakService.hentVedtak(behandlingId) } returns InterntVedtakTestdata.Boutgifter.innvilgetVedtak
    }

    private fun mockDagligReiseTso() {
        every { behandlingService.hentSaksbehandling(behandlingId) } returns InterntVedtakTestdata.DagligReise.behandling
        every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns InterntVedtakTestdata.DagligReise.vilkårperioderTso
        every { faktaGrunnlagService.hentGrunnlagsdata(behandlingId) } returns InterntVedtakTestdata.DagligReise.grunnlagsdata
        every { barnService.finnBarnPåBehandling(behandlingId) } returns emptyList()
        every { vilkårService.hentVilkår(behandlingId) } returns
            InterntVedtakTestdata.DagligReise.vilkårOffentligTransport +
            InterntVedtakTestdata.DagligReise.vilkårPrivatBil
        every { vedtakService.hentVedtak(behandlingId) } returns InterntVedtakTestdata.DagligReise.innvilgetVedtakTso()
    }

    private fun mockDagligReiseTsr() {
        every { behandlingService.hentSaksbehandling(behandlingId) } returns InterntVedtakTestdata.DagligReise.behandlingTsr
        every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns InterntVedtakTestdata.DagligReise.vilkårperioderTsr
        every { faktaGrunnlagService.hentGrunnlagsdata(behandlingId) } returns InterntVedtakTestdata.DagligReise.grunnlagsdata
        every { barnService.finnBarnPåBehandling(behandlingId) } returns emptyList()
        every { vilkårService.hentVilkår(behandlingId) } returns
            InterntVedtakTestdata.DagligReise.vilkårOffentligTransport +
            InterntVedtakTestdata.DagligReise.vilkårPrivatBil
        every { vedtakService.hentVedtak(behandlingId) } returns InterntVedtakTestdata.DagligReise.innvilgetVedtakTsr()
    }

    private fun mockReiseTilSamlingTso() {
        every { behandlingService.hentSaksbehandling(behandlingId) } returns InterntVedtakTestdata.ReiseTilSamling.behandling
        every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns InterntVedtakTestdata.ReiseTilSamling.vilkårperioderTso
        every { faktaGrunnlagService.hentGrunnlagsdata(behandlingId) } returns InterntVedtakTestdata.ReiseTilSamling.grunnlagsdata
        every { barnService.finnBarnPåBehandling(behandlingId) } returns emptyList()
        every { vilkårService.hentVilkår(behandlingId) } returns
            InterntVedtakTestdata.ReiseTilSamling.vilkårOffentligTransport +
            InterntVedtakTestdata.ReiseTilSamling.vilkårPrivatBil
        every { vedtakService.hentVedtak(behandlingId) } returns InterntVedtakTestdata.ReiseTilSamling.innvilgetVedtakTso()
    }

    private fun mockReiseTilSamlingTsr() {
        every { behandlingService.hentSaksbehandling(behandlingId) } returns InterntVedtakTestdata.ReiseTilSamling.behandlingTsr
        every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns InterntVedtakTestdata.ReiseTilSamling.vilkårperioderTsr
        every { faktaGrunnlagService.hentGrunnlagsdata(behandlingId) } returns InterntVedtakTestdata.ReiseTilSamling.grunnlagsdata
        every { barnService.finnBarnPåBehandling(behandlingId) } returns emptyList()
        every { vilkårService.hentVilkår(behandlingId) } returns
            InterntVedtakTestdata.ReiseTilSamling.vilkårOffentligTransport +
            InterntVedtakTestdata.ReiseTilSamling.vilkårPrivatBil
        every { vedtakService.hentVedtak(behandlingId) } returns InterntVedtakTestdata.ReiseTilSamling.innvilgetVedtakTsr()
    }

    private fun mockFlytting(stønadstype: Stønadstype) {
        every { behandlingService.hentSaksbehandling(behandlingId) } returns
            InterntVedtakTestdata.Flytting.behandling(stønadstype)
        every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns
            InterntVedtakTestdata.Flytting.vilkårperioder(stønadstype)
        every { faktaGrunnlagService.hentGrunnlagsdata(behandlingId) } returns
            InterntVedtakTestdata.Flytting.grunnlagsdata
        every { barnService.finnBarnPåBehandling(behandlingId) } returns emptyList()
        every { vilkårService.hentVilkår(behandlingId) } returns InterntVedtakTestdata.Flytting.vilkår
        every { vedtakService.hentVedtak(behandlingId) } returns InterntVedtakTestdata.Flytting.vedtak(stønadstype)
    }

    @Test
    fun `html skal være formatert for å enklere kunne sjekke diff`() {
        val rootFolder = "interntVedtak"
        FileUtil
            .listDir(rootFolder)
            .forEach { dir ->
                val fil = FileUtil.readFile("$rootFolder/${dir.fileName}/internt_vedtak.html")
                val erIkkeFormatert =
                    fil
                        .split("\n")
                        .none { it.contains("<body") && it.contains("<div") }
                assertThat(erIkkeFormatert).isTrue()
            }
    }

    @Test
    fun `htmlify HTTP500 skal kaste feil`() {
        val restTemplate = htmlifyRestTemplate()
        val server = MockRestServiceServer.createServer(restTemplate)
        val feiltekst = "Htmlify-feil"
        server
            .expect(requestTo(htmlifyEndpoint()))
            .andExpect(method(HttpMethod.POST))
            .andRespond(
                withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(feiltekst),
            )

        assertThatThrownBy { lagHtmlifyClient(restTemplate).generateHtml(testInterntVedtak()) }
            .isInstanceOf(HttpServerErrorException::class.java)
            .hasMessageContaining(feiltekst)

        server.verify()
    }

    private fun testInterntVedtak(): InterntVedtak {
        mock(Stønadstype.BARNETILSYN)
        return service.lagInterntVedtak(behandlingId)
    }

    private fun lagHtmlifyClient(restTemplate: RestTemplate = htmlifyRestTemplate()): HtmlifyClient =
        HtmlifyClient(htmlifyUri(), restTemplate)

    private fun htmlifyRestTemplate(): RestTemplate {
        val restTemplate = RestTemplate()
        restTemplate.messageConverters =
            listOf(
                StringHttpMessageConverter(),
                JacksonJsonHttpMessageConverter(JsonMapperProvider.jsonMapper),
            )
        return restTemplate
    }

    private fun htmlifyUri(): URI = URI.create(System.getenv("HTMLIFY_URI")?.takeIf { it.isNotBlank() } ?: DEFAULT_HTMLIFY_URI)

    private fun htmlifyEndpoint(): String =
        UriComponentsBuilder
            .fromUri(htmlifyUri())
            .pathSegment("api", "internt-vedtak")
            .toUriString()

    private fun generatePdf(
        html: String,
        name: String,
    ) {
        skrivTilFil(name, hentPdf(html))
    }

    private fun hentPdf(
        html: String,
        restTemplate: RestTemplate = pdfRestTemplate(),
    ): ByteArray {
        val request =
            HttpEntity(
                html,
                HttpHeaders().apply {
                    accept = listOf(MediaType.APPLICATION_PDF)
                },
            )
        return restTemplate.postForEntity(PDF_URL, request, ByteArray::class.java).body!!
    }

    private fun pdfRestTemplate(): RestTemplate = RestTemplate()

    private companion object {
        const val DEFAULT_HTMLIFY_URI = "http://localhost:8001"
        const val PDF_URL = "https://familie-dokument.intern.dev.nav.no/api/html-til-pdf"
    }
}
