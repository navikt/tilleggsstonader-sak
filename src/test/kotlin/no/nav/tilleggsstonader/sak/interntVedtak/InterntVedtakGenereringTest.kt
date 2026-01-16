package no.nav.tilleggsstonader.sak.interntVedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.interntVedtak.InterntVedtakTestdata.behandlingId
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.FaktaGrunnlagService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.util.FileUtil
import no.nav.tilleggsstonader.sak.util.FileUtil.assertFileIsEqual
import no.nav.tilleggsstonader.sak.util.FileUtil.skrivTilFil
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
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
    @MethodSource("stønadstyperInterntVedtak")
    fun `json til htmlify er riktig`(type: StønadstypeInterntVedtak) {
        val stønadstype = type.stønadstype
        if (type.håndteresAvInterntVedtak) {
            mock(type)
            val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)
            assertFileIsEqual("interntVedtak/$stønadstype/internt_vedtak.json", interntVedtak)
        } else {
            val saksbehandling = saksbehandling(fagsak(stønadstype = stønadstype))
            every { behandlingService.hentSaksbehandling(behandlingId) } returns saksbehandling
            assertThatThrownBy {
                service.lagInterntVedtak(behandlingId = behandlingId)
            }.hasMessageContaining("Internt vedtak håndterer ikke stønadstype=$stønadstype")
        }
    }

    /**
     * Kommenter ut Disabled for å oppdatere html og pdf ved endringer i htmlify.
     * Endre SKAL_SKRIVE_TIL_FIL i fileUtil til true
     * Formatter htmlfil etter generering for å unngå stor diff
     */
    @Disabled
    @ParameterizedTest
    @MethodSource("stønadstyperInterntVedtak")
    fun `lager html og pdf`(type: StønadstypeInterntVedtak) {
        if (!type.håndteresAvInterntVedtak) {
            return
        }
        mock(type)
        val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)
        val html = lagHtmlifyClient().generateHtml(interntVedtak)
        skrivTilFil("interntVedtak/${type.stønadstype}/internt_vedtak.html", html)
        generatePdf(html, "interntVedtak/${type.stønadstype}/internt_vedtak.pdf")
    }

    private fun mock(type: StønadstypeInterntVedtak) {
        when (type.stønadstype) {
            Stønadstype.BARNETILSYN -> mockTilsynBarn()
            Stønadstype.LÆREMIDLER -> mockLæremidler()
            Stønadstype.BOUTGIFTER -> mockBoutgifter()
            Stønadstype.DAGLIG_REISE_TSO -> mockDagligReiseTso()
            Stønadstype.DAGLIG_REISE_TSR -> mockDagligReiseTsr()
        }
    }

    private fun mockTilsynBarn() {
        every { behandlingService.hentSaksbehandling(behandlingId) } returns InterntVedtakTestdata.TilsynBarn.behandling
        every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns InterntVedtakTestdata.TilsynBarn.vilkårperioder
        every { faktaGrunnlagService.hentGrunnlagsdata(behandlingId) } returns InterntVedtakTestdata.TilsynBarn.grunnlagsdata
        every { barnService.finnBarnPåBehandling(behandlingId) } returns InterntVedtakTestdata.TilsynBarn.behandlingBarn
        every { vilkårService.hentVilkår(behandlingId) } returns InterntVedtakTestdata.TilsynBarn.vilkår
        every { vedtakService.hentVedtak(behandlingId) } returns InterntVedtakTestdata.TilsynBarn.vedtak
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
        every { vilkårService.hentVilkår(behandlingId) } returns InterntVedtakTestdata.DagligReise.vilkårOffentligTransport
        every { vedtakService.hentVedtak(behandlingId) } returns InterntVedtakTestdata.DagligReise.innvilgetVedtakTso()
    }

    private fun mockDagligReiseTsr() {
        every { behandlingService.hentSaksbehandling(behandlingId) } returns InterntVedtakTestdata.DagligReise.behandling
        every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns InterntVedtakTestdata.DagligReise.vilkårperioderTsr
        every { faktaGrunnlagService.hentGrunnlagsdata(behandlingId) } returns InterntVedtakTestdata.DagligReise.grunnlagsdata
        every { barnService.finnBarnPåBehandling(behandlingId) } returns emptyList()
        every { vilkårService.hentVilkår(behandlingId) } returns InterntVedtakTestdata.DagligReise.vilkårOffentligTransport
        every { vedtakService.hentVedtak(behandlingId) } returns InterntVedtakTestdata.DagligReise.innvilgetVedtakTsr()
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

    private fun lagHtmlifyClient(): HtmlifyClient {
        val restTemplate = TestRestTemplate().restTemplate
        restTemplate.messageConverters =
            listOf(
                StringHttpMessageConverter(),
                MappingJackson2HttpMessageConverter(ObjectMapperProvider.objectMapper),
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
        skrivTilFil(name, pdf)
    }

    companion object {
        @JvmStatic
        fun stønadstyperInterntVedtak(): List<StønadstypeInterntVedtak> =
            Stønadstype.entries.map {
                when (it) {
                    Stønadstype.BARNETILSYN,
                    Stønadstype.LÆREMIDLER,
                    Stønadstype.BOUTGIFTER,
                    Stønadstype.DAGLIG_REISE_TSO,
                    Stønadstype.DAGLIG_REISE_TSR,
                    -> it.håndteres()
                }
            }

        data class StønadstypeInterntVedtak(
            val stønadstype: Stønadstype,
            val håndteresAvInterntVedtak: Boolean,
        )

        private fun Stønadstype.håndteres() = StønadstypeInterntVedtak(stønadstype = this, håndteresAvInterntVedtak = true)

        private fun Stønadstype.håndteresIkke() = StønadstypeInterntVedtak(stønadstype = this, håndteresAvInterntVedtak = false)
    }
}
