package no.nav.tilleggsstonader.sak.interntVedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.ObjectMapperProvider
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.fagsak.domain.EksternFagsakId
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.util.FileUtil.assertFileIsEqual
import no.nav.tilleggsstonader.sak.util.FileUtil.skrivTilFil
import no.nav.tilleggsstonader.sak.util.SøknadBarnetilsynUtil
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.TotrinnskontrollService
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnInternStatus
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.domain.TotrinnskontrollUtil
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.StønadsperiodeService
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.StønadsperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.delvilkårAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.delvilkårMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.vurdering
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.KildeVilkårsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatDelvilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.SvarJaNei
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import java.net.URI
import java.time.LocalDate
import java.util.UUID

class InterntVedtakServiceTest {

    private val behandlingService = mockk<BehandlingService>()
    private val totrinnskontrollService = mockk<TotrinnskontrollService>()
    private val vilkårperiodeService = mockk<VilkårperiodeService>()
    private val stønadsperiodeService = mockk<StønadsperiodeService>()
    private val søknadService = mockk<SøknadService>()

    val service = InterntVedtakService(
        behandlingService = behandlingService,
        totrinnskontrollService = totrinnskontrollService,
        vilkårperiodeService = vilkårperiodeService,
        stønadsperiodeService = stønadsperiodeService,
        søknadService = søknadService,
    )

    val vedtakstidspunkt = LocalDate.of(2024, 1, 1).atStartOfDay()
    val fagsak = fagsak(eksternId = EksternFagsakId(1673L, UUID.randomUUID()))
    val behandling = saksbehandling(
        behandling = behandling(
            id = UUID.fromString("001464ca-20dc-4f6c-b3e8-c83bd98b3e31"),
            vedtakstidspunkt = vedtakstidspunkt,
            opprettetTid = LocalDate.of(2024, 2, 5).atStartOfDay(),
            fagsak = fagsak,
        ),
        fagsak = fagsak,
    )
    val vilkårperioder = Vilkårperioder(
        målgrupper = listOf(
            VilkårperiodeTestUtil.målgruppe(
                begrunnelse = "målgrupp abc",
                delvilkår = delvilkårMålgruppe(vurdering = vurdering()),
                fom = LocalDate.of(2024, 2, 5),
                tom = LocalDate.of(2024, 2, 10),
            ),
            VilkårperiodeTestUtil.målgruppe(
                type = MålgruppeType.OVERGANGSSTØNAD,
                begrunnelse = "målgrupp os",
                delvilkår = delvilkårMålgruppe(vurdering = vurdering()),
                fom = LocalDate.of(2024, 2, 5),
                tom = LocalDate.of(2024, 2, 10),
            ),
        ),
        aktiviteter = listOf(
            VilkårperiodeTestUtil.aktivitet(
                begrunnelse = "aktivitet abd",
                resultat = ResultatVilkårperiode.IKKE_OPPFYLT,
                delvilkår = delvilkårAktivitet(
                    lønnet = vurdering(
                        SvarJaNei.JA,
                        "begrunnelse lønnet",
                        resultat = ResultatDelvilkårperiode.IKKE_OPPFYLT,
                    ),
                ),
                fom = LocalDate.of(2024, 2, 5),
                tom = LocalDate.of(2024, 2, 10),
            ),
            VilkårperiodeTestUtil.aktivitet(
                resultat = ResultatVilkårperiode.SLETTET,
                kilde = KildeVilkårsperiode.MANUELL,
                slettetKommentar = "kommentar slettet",
                fom = LocalDate.of(2024, 2, 5),
                tom = LocalDate.of(2024, 2, 10),
            ),
        ),
    )
    val stønadsperioder = listOf(
        StønadsperiodeDto(
            id = UUID.randomUUID(),
            fom = LocalDate.of(2024, 2, 1),
            tom = LocalDate.of(2024, 3, 31),
            målgruppe = MålgruppeType.AAP,
            aktivitet = AktivitetType.TILTAK,
        ),
        StønadsperiodeDto(
            id = UUID.randomUUID(),
            fom = LocalDate.of(2024, 2, 1),
            tom = LocalDate.of(2024, 3, 31),
            målgruppe = MålgruppeType.NEDSATT_ARBEIDSEVNE,
            aktivitet = AktivitetType.REELL_ARBEIDSSØKER,
        ),
    )
    val totrinnskontroll = TotrinnskontrollUtil.totrinnskontroll(TotrinnInternStatus.GODKJENT, beslutter = "saksbeh2")
    val søknad = SøknadBarnetilsynUtil.søknadBarnetilsyn()

    val behandlingId = behandling.id

    @BeforeEach
    fun setUp() {
        every { behandlingService.hentSaksbehandling(behandlingId) } returns behandling
        every { vilkårperiodeService.hentVilkårperioder(behandlingId) } returns vilkårperioder
        every { stønadsperiodeService.hentStønadsperioder(behandlingId) } returns stønadsperioder
        every { totrinnskontrollService.hentTotrinnskontroll(behandlingId) } returns totrinnskontroll
        every { søknadService.hentSøknadBarnetilsyn(behandlingId) } returns søknad
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
     * Endre SKAL_SKRIVE_TIL_FIL til true
     * Formatter htmlfil etter generering for å unngå stor diff
     */
    @Disabled
    @Test
    fun `lager html og pdf`() {
        val interntVedtak = service.lagInterntVedtak(behandlingId = behandlingId)
        val html = lagHtmlifyClient().generateHtml(interntVedtak)
        skrivTilFil("interntVedtak/internt_vedtak.html", html)
        generatePdf(html, "interntVedtak/internt_vedtak.pdf")
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
            assertThat(behandlingId).isEqualTo(behandling.id)
            assertThat(eksternFagsakId).isEqualTo(1673L)
            assertThat(stønadstype).isEqualTo(Stønadstype.BARNETILSYN)
            assertThat(årsak).isEqualTo(behandling.årsak)
            assertThat(ident).isEqualTo(behandling.ident)
            assertThat(opprettetTidspunkt).isEqualTo(behandling.opprettetTid)
            assertThat(resultat).isEqualTo(behandling.resultat)
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

    private fun assertMålgrupper(målgrupper: List<Vilkårperiode>) {
        assertThat(målgrupper).hasSize(2)

        val målgruppe = vilkårperioder.målgrupper.single { it.type == MålgruppeType.AAP }
        with(målgrupper.single { it.type == MålgruppeType.AAP }) {
            assertThat(type).isEqualTo(MålgruppeType.AAP)
            assertThat(fom).isEqualTo(målgruppe.fom)
            assertThat(tom).isEqualTo(målgruppe.tom)
            assertThat(kilde).isEqualTo(KildeVilkårsperiode.SYSTEM)
            assertThat(resultat).isEqualTo(ResultatVilkårperiode.OPPFYLT)
            assertThat(begrunnelse).isEqualTo("målgrupp abc")
            with(delvilkår.medlemskap!!) {
                assertThat(svar).isEqualTo(SvarJaNei.JA_IMPLISITT.name)
                assertThat(begrunnelse).isNull()
                assertThat(resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
            }
            assertThat(delvilkår.lønnet).isNull()
            assertThat(delvilkår.mottarSykepenger).isNull()
        }
    }

    private fun assertAktiviteter(aktiviteter: List<Vilkårperiode>) {
        assertThat(aktiviteter).hasSize(2)
        val aktivitet = vilkårperioder.aktiviteter.single { it.resultat != ResultatVilkårperiode.SLETTET }
        with(aktiviteter.single { it.resultat != ResultatVilkårperiode.SLETTET }) {
            assertThat(type).isEqualTo(AktivitetType.TILTAK)
            assertThat(fom).isEqualTo(aktivitet.fom)
            assertThat(tom).isEqualTo(aktivitet.tom)
            assertThat(kilde).isEqualTo(KildeVilkårsperiode.SYSTEM)
            assertThat(resultat).isEqualTo(ResultatVilkårperiode.IKKE_OPPFYLT)
            assertThat(begrunnelse).isEqualTo("aktivitet abd")
            with(delvilkår.lønnet!!) {
                assertThat(svar).isEqualTo(SvarJaNei.JA.name)
                assertThat(begrunnelse).isEqualTo("begrunnelse lønnet")
                assertThat(resultat).isEqualTo(ResultatDelvilkårperiode.IKKE_OPPFYLT)
            }
            with(delvilkår.mottarSykepenger!!) {
                assertThat(svar).isEqualTo(SvarJaNei.NEI.name)
                assertThat(begrunnelse).isNull()
                assertThat(resultat).isEqualTo(ResultatDelvilkårperiode.OPPFYLT)
            }
            assertThat(delvilkår.medlemskap).isNull()
        }

        val aktivitetSlettet = vilkårperioder.aktiviteter.single { it.resultat == ResultatVilkårperiode.SLETTET }
        with(aktiviteter.single { it.resultat == ResultatVilkårperiode.SLETTET }) {
            assertThat(resultat).isEqualTo(ResultatVilkårperiode.SLETTET)
            assertThat(slettetKommentar).isEqualTo("kommentar slettet")
        }
    }

    private fun lagHtmlifyClient(): HtmlifyClient {
        val restTemplate = TestRestTemplate().restTemplate
        restTemplate.messageConverters.removeIf { it is MappingJackson2HttpMessageConverter }
        restTemplate.messageConverters.add(MappingJackson2HttpMessageConverter(ObjectMapperProvider.objectMapper))
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
