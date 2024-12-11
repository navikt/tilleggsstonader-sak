package no.nav.tilleggsstonader.sak.interntVedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.interntVedtak.Testdata.behandlingId
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.util.FileUtil
import no.nav.tilleggsstonader.sak.util.FileUtil.assertFileIsEqual
import no.nav.tilleggsstonader.sak.util.FileUtil.skrivTilFil
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import java.net.URI

class DokumentgenereringTest {

    private val behandlingService = mockk<BehandlingService>()
    private val totrinnskontrollService = mockk<TotrinnskontrollService>()
    private val vilkårperiodeService = mockk<VilkårperiodeService>()
    private val stønadsperiodeService = mockk<StønadsperiodeService>()
    private val søknadService = mockk<SøknadService>()
    private val grunnlagsdataService = mockk<GrunnlagsdataService>()
    private val barnService = mockk<BarnService>()
    private val vilkårService = mockk<VilkårService>()
    private val vedtakService = mockk<VedtakService>()

    val service = InterntVedtakService(
        behandlingService = behandlingService,
        totrinnskontrollService = totrinnskontrollService,
        vilkårperiodeService = vilkårperiodeService,
        stønadsperiodeService = stønadsperiodeService,
        søknadService = søknadService,
        grunnlagsdataService = grunnlagsdataService,
        barnService = barnService,
        vilkårService = vilkårService,
        vedtakService = vedtakService,
    )

    @BeforeEach
    fun setUp() {
        every { stønadsperiodeService.hentStønadsperioder(behandlingId) } returns Testdata.stønadsperioder
        every { totrinnskontrollService.hentTotrinnskontroll(behandlingId) } returns Testdata.totrinnskontroll
        every { søknadService.hentSøknadMetadata(behandlingId) } returns Testdata.søknadMetadata
    }

    @Nested
    inner class GenererDokumenterTilsynBarn {
        @BeforeEach
        fun setUp() {
            every { behandlingService.hentSaksbehandling(behandlingId) } returns Testdata.TilsynBarn.behandling
            every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns Testdata.TilsynBarn.vilkårperioder
            every { grunnlagsdataService.hentGrunnlagsdata(behandlingId) } returns Testdata.TilsynBarn.grunnlagsdata
            every { barnService.finnBarnPåBehandling(behandlingId) } returns Testdata.TilsynBarn.behandlingBarn
            every { vilkårService.hentVilkårsett(behandlingId) } returns Testdata.TilsynBarn.vilkår
            every { vedtakService.hentVedtak(behandlingId) } returns Testdata.TilsynBarn.vedtak
        }

        @Test
        fun `json til htmlify er riktig`() {
            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)
            assertFileIsEqual("interntVedtak/BARNETILSYN/internt_vedtak.json", interntVedtak)
        }

        /**
         * Kommenter ut Disabled for å oppdatere html og pdf ved endringer i htmlify.
         * Endre SKAL_SKRIVE_TIL_FIL i fileUtil til true
         * Formatter htmlfil etter generering for å unngå stor diff
         */
        // TODO: Oppdater med ny HTML når feltene delvilkår og aktivitetsdager har blitt fjernet fra VilkårperiodeInterntVedtak
        @Test
        @Disabled
        fun `lager html og pdf`() {
            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)
            val html = lagHtmlifyClient().generateHtml(interntVedtak)
            skrivTilFil("interntVedtak/BARNETILSYN/internt_vedtak.html", html)
            generatePdf(html, "interntVedtak/BARNETILSYN/internt_vedtak.pdf")
        }
    }

    @Nested
    inner class GenererDokumenterLæremidler {
        @BeforeEach
        fun setUp() {
            every { behandlingService.hentSaksbehandling(behandlingId) } returns Testdata.Læremidler.behandling
            every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns Testdata.Læremidler.vilkårperioder
            every { grunnlagsdataService.hentGrunnlagsdata(behandlingId) } returns Testdata.Læremidler.grunnlagsdata
            every { barnService.finnBarnPåBehandling(behandlingId) } returns emptyList()
            every { vilkårService.hentVilkårsett(behandlingId) } returns emptyList()
            every { vedtakService.hentVedtak(behandlingId) } returns Testdata.Læremidler.avslåttVedtak
        }

        @Test
        fun `json til htmlify er riktig`() {
            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)
            assertFileIsEqual("interntVedtak/LÆREMIDLER/internt_vedtak.json", interntVedtak)
        }

        /**
         * Kommenter ut Disabled for å oppdatere html og pdf ved endringer i htmlify.
         * Endre SKAL_SKRIVE_TIL_FIL i fileUtil til true
         * Formatter htmlfil etter generering for å unngå stor diff
         */
        @Test
        @Disabled
        fun `lager html og pdf`() {
            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)
            val html = lagHtmlifyClient().generateHtml(interntVedtak)
            skrivTilFil("interntVedtak/LÆREMIDLER/internt_vedtak.html", html)
            generatePdf(html, "interntVedtak/LÆREMIDLER/internt_vedtak.pdf")
        }
    }

    @ParameterizedTest
    @EnumSource(Stønadstype::class)
    fun `html skal være formatert for å enklere kunne sjekke diff`(stønadstype: Stønadstype) {
        Stønadstype.entries.forEach { }
        val erIkkeFormatert = FileUtil.readFile("interntVedtak/$stønadstype/internt_vedtak.html").split("\n")
            .none { it.contains("<body") && it.contains("<div") }

        assertThat(erIkkeFormatert).isTrue()
    }

    private fun lagHtmlifyClient(): HtmlifyClient {
        val restTemplate = TestRestTemplate().restTemplate
        restTemplate.messageConverters = listOf(
            StringHttpMessageConverter(),
            MappingJackson2HttpMessageConverter(ObjectMapperProvider.objectMapper),
        )
        return HtmlifyClient(URI.create("http://localhost:8001"), restTemplate)
    }

    private fun generatePdf(html: String, name: String) {
        val url = "https://familie-dokument.intern.dev.nav.no/api/html-til-pdf"
        val request = HttpEntity(
            html,
            HttpHeaders().apply {
                accept = listOf(MediaType.APPLICATION_PDF)
            },
        )
        val pdf = TestRestTemplate().postForEntity<ByteArray>(url, request).body!!
        skrivTilFil(name, pdf)
    }
}
