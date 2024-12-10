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
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.AktivitetBarnetilsynFaktaOgVurderingerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.dto.MålgruppeFaktaOgVurderingerDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.converter.StringHttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import java.net.URI
import java.time.LocalDate

class InterntVedtakServiceTest {

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
        every { behandlingService.hentSaksbehandling(behandlingId) } returns Testdata.behandling
        every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns Testdata.vilkårperioderTilsynBarn
        every { stønadsperiodeService.hentStønadsperioder(behandlingId) } returns Testdata.stønadsperioder
        every { totrinnskontrollService.hentTotrinnskontroll(behandlingId) } returns Testdata.totrinnskontroll
        every { søknadService.hentSøknadMetadata(behandlingId) } returns Testdata.søknadMetadata
        every { grunnlagsdataService.hentGrunnlagsdata(behandlingId) } returns Testdata.grunnlagsdata
        every { barnService.finnBarnPåBehandling(behandlingId) } returns Testdata.behandlingBarn
        every { vilkårService.hentVilkårsett(behandlingId) } returns Testdata.vilkår
        every { vedtakService.hentVedtak(behandlingId) } returns Testdata.vedtak
    }

    @Test
    fun `felter skal bli riktig mappede`() {
        val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)
        assertBehandling(interntVedtak.behandling)
        assertSøknad(interntVedtak.søknad)
        assertMålgrupper(interntVedtak.målgrupper)
        assertAktiviteter(interntVedtak.aktiviteter)
        assertStønadsperioder(interntVedtak.stønadsperioder)
    }

    @Test
    fun `json til htmlify er riktig`() {
        val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)
        assertFileIsEqual("interntVedtak/internt_vedtak.json", interntVedtak)
    }

    /**
     * Kommenter ut Disabled for å oppdatere html og pdf ved endringer i htmlify.
     * Endre SKAL_SKRIVE_TIL_FIL i fileUtil til true
     * Formatter htmlfil etter generering for å unngå stor diff
     */
    // TODO: Oppdater med ny HTML når htmlify har tatt i bruk vedtakOgBeregning-feltet
    @Test
    fun `lager html og pdf`() {
        val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)
        val html = lagHtmlifyClient().generateHtml(interntVedtak)
        skrivTilFil("interntVedtak/internt_vedtak.html", html)
        generatePdf(html, "interntVedtak/internt_vedtak.pdf")
    }

    @Test
    fun `html skal være formattert for å enklere kunne sjekke diff`() {
        val erIkkeFormatert = FileUtil.readFile("interntVedtak/internt_vedtak.html").split("\n")
            .none { it.contains("<body") && it.contains("<div") }

        assertThat(erIkkeFormatert).isTrue()
    }

    private fun assertStønadsperioder(stønadsperioder: List<Stønadsperiode>) {
        assertThat(stønadsperioder).hasSize(2)
        with(stønadsperioder.first()) {
            assertThat(målgruppe).isEqualTo(MålgruppeType.AAP)
            assertThat(aktivitet).isEqualTo(AktivitetType.TILTAK)
            assertThat(fom).isEqualTo(LocalDate.of(2024, 2, 1))
            assertThat(tom).isEqualTo(LocalDate.of(2024, 3, 31))
        }
        with(stønadsperioder.last()) {
            assertThat(målgruppe).isEqualTo(MålgruppeType.NEDSATT_ARBEIDSEVNE)
            assertThat(aktivitet).isEqualTo(AktivitetType.REELL_ARBEIDSSØKER)
            assertThat(fom).isEqualTo(LocalDate.of(2024, 2, 1))
            assertThat(tom).isEqualTo(LocalDate.of(2024, 3, 31))
        }
    }

    private fun assertBehandling(behandlinginfo: Behandlinginfo) {
        with(behandlinginfo) {
            assertThat(behandlingId).isEqualTo(Testdata.behandlingId)
            assertThat(eksternFagsakId).isEqualTo(1673L)
            assertThat(stønadstype).isEqualTo(Stønadstype.BARNETILSYN)
            assertThat(årsak).isEqualTo(Testdata.behandling.årsak)
            assertThat(ident).isEqualTo(Testdata.behandling.ident)
            assertThat(opprettetTidspunkt).isEqualTo(Testdata.behandling.opprettetTid)
            assertThat(resultat).isEqualTo(Testdata.behandling.resultat)
            assertThat(vedtakstidspunkt).isEqualTo(vedtakstidspunkt)
            assertThat(saksbehandler).isEqualTo("saksbehandler")
            assertThat(beslutter).isEqualTo("saksbeh2")
        }
    }

    private fun assertSøknad(søknad: Søknadsinformasjon?) {
        with(søknad!!) {
            assertThat(mottattTidspunkt).isEqualTo(søknad.mottattTidspunkt)
        }
    }

    private fun assertMålgrupper(målgrupper: List<VilkårperiodeInterntVedtak>) {
        assertThat(målgrupper).hasSize(2)

        val målgruppe =
            Testdata.vilkårperioderTilsynBarn.målgrupper.single { it.type == MålgruppeType.AAP }
        with(målgrupper.single { it.type == MålgruppeType.AAP }) {
            assertThat(type).isEqualTo(MålgruppeType.AAP)
            assertThat(fom).isEqualTo(målgruppe.fom)
            assertThat(tom).isEqualTo(målgruppe.tom)
            assertThat(kilde).isEqualTo(KildeVilkårsperiode.MANUELL)
            assertThat(resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
            assertThat(begrunnelse).isEqualTo("målgruppe aap")
            with(delvilkår.medlemskap!!) {
                assertThat(svar).isEqualTo(SvarJaNei.JA_IMPLISITT.name)
                assertThat(resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
            }
            assertThat(delvilkår.lønnet).isNull()
            with((faktaOgVurderinger as MålgruppeFaktaOgVurderingerDto).medlemskap!!) {
                assertThat(svar).isEqualTo(SvarJaNei.JA_IMPLISITT)
                assertThat(resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
            }
        }
    }

    private fun assertAktiviteter(aktiviteter: List<VilkårperiodeInterntVedtak>) {
        assertThat(aktiviteter).hasSize(2)
        val aktivitet =
            Testdata.vilkårperioderTilsynBarn.aktiviteter.single { it.resultat != ResultatVilkårperiode.SLETTET }
        with(aktiviteter.single { it.resultat != ResultatVilkårperiode.SLETTET }) {
            assertThat(type).isEqualTo(AktivitetType.TILTAK)
            assertThat(fom).isEqualTo(aktivitet.fom)
            assertThat(tom).isEqualTo(aktivitet.tom)
            assertThat(kilde).isEqualTo(KildeVilkårsperiode.MANUELL)
            assertThat(resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
            assertThat(begrunnelse).isEqualTo("aktivitet abd")
            with(delvilkår.lønnet!!) {
                assertThat(svar).isEqualTo(SvarJaNei.JA.name)
                assertThat(resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
            }
            assertThat(delvilkår.medlemskap).isNull()
            with((faktaOgVurderinger as AktivitetBarnetilsynFaktaOgVurderingerDto).lønnet!!) {
                assertThat(svar).isEqualTo(SvarJaNei.JA)
                assertThat(resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
            }
        }

        val aktivitetSlettet =
            Testdata.vilkårperioderTilsynBarn.aktiviteter.single { it.resultat == ResultatVilkårperiode.SLETTET }
        with(aktiviteter.single { it.resultat == ResultatVilkårperiode.SLETTET }) {
            assertThat(resultat).isEqualTo(ResultatVilkårperiode.SLETTET)
            assertThat(slettetKommentar).isEqualTo("kommentar slettet")
        }
    }

    private fun lagHtmlifyClient(): HtmlifyClient {
        val restTemplate = TestRestTemplate().restTemplate
        restTemplate.messageConverters = listOf(
            StringHttpMessageConverter(),
            MappingJackson2HttpMessageConverter(ObjectMapperProvider.objectMapper),
        )
        return HtmlifyClient(URI.create("http://localhost:8001"), restTemplate)
    }

    @Suppress("unused")
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
