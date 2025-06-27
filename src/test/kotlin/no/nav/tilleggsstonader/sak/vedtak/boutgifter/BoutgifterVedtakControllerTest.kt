package no.nav.tilleggsstonader.sak.vedtak.boutgifter

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.test.httpclient.ProblemDetailUtil.catchProblemDetailException
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.AvslagBoutgifterDto
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.InnvilgelseBoutgifterRequest
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.InnvilgelseBoutgifterResponse
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.OpphørBoutgifterRequest
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.OpphørBoutgifterResponse
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.dto.VedtakBoutgifterResponse
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.client.exchange
import java.time.LocalDate
import java.util.UUID

class BoutgifterVedtakControllerTest : IntegrationTest() {
    @Autowired
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    @Autowired
    lateinit var vilkårRepository: VilkårRepository

    val dummyFom: LocalDate = LocalDate.now()
    val dummyTom: LocalDate = LocalDate.now().plusDays(7)
    val dummyFagsak = fagsak(stønadstype = Stønadstype.BOUTGIFTER)
    val dummyBehandling = behandling(fagsak = dummyFagsak, steg = StegType.BEREGNE_YTELSE, status = BehandlingStatus.UTREDES)

    val vedtaksperiode = vedtaksperiode(fom = dummyFom, tom = dummyTom)
    val aktivitet = aktivitet(dummyBehandling.id, fom = dummyFom, tom = dummyTom)
    val målgruppe = målgruppe(dummyBehandling.id, fom = dummyFom, tom = dummyTom)

    val vilkår =
        vilkår(
            behandlingId = dummyBehandling.id,
            type = VilkårType.UTGIFTER_OVERNATTING,
            fom = dummyFom,
            tom = dummyTom,
        )

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(onBehalfOfToken())
        testoppsettService.opprettBehandlingMedFagsak(dummyBehandling, stønadstype = Stønadstype.BOUTGIFTER)
        vilkårperiodeRepository.insert(aktivitet)
        vilkårperiodeRepository.insert(målgruppe)
        vilkårRepository.insert(vilkår)
    }

    @Test
    fun `skal validere token`() {
        headers.clear()
        val exception = catchProblemDetailException { hentVedtak<InnvilgelseBoutgifterResponse>(BehandlingId.random()) }

        assertThat(exception.httpStatus).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `hent vedtak skal returnere tom body når det ikke finnes noen lagrede vedtak`() {
        val response = hentVedtak<InnvilgelseBoutgifterResponse>(dummyBehandling.id)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNull()
    }

    @Nested
    inner class Avslag {
        @Test
        fun `skal lagre og hente avslag`() {
            val avslag =
                AvslagBoutgifterDto(
                    årsakerAvslag = listOf(ÅrsakAvslag.INGEN_AKTIVITET),
                    begrunnelse = "begrunnelse",
                )

            avslåVedtak(dummyBehandling, avslag)

            val lagretDto = hentVedtak<AvslagBoutgifterDto>(dummyBehandling.id).body!!

            assertThat(lagretDto.årsakerAvslag).isEqualTo(avslag.årsakerAvslag)
            assertThat(lagretDto.begrunnelse).isEqualTo(avslag.begrunnelse)
            assertThat(lagretDto.type).isEqualTo(TypeVedtak.AVSLAG)
        }

        private fun avslåVedtak(
            behandling: Behandling,
            vedtak: AvslagBoutgifterDto,
        ) {
            restTemplate.exchange<Map<String, Any>?>(
                localhost("api/vedtak/boutgifter/${behandling.id}/avslag"),
                HttpMethod.POST,
                HttpEntity(vedtak, headers),
            )
        }
    }

    @Nested
    inner class Opphør {
        @Test
        fun `skal lagre og hente opphør`() {
            val revurderFraDato = dummyFom.plusDays(4)
            innvilgeVedtak(
                behandling = dummyBehandling,
                vedtak = InnvilgelseBoutgifterRequest(listOf(vedtaksperiode.tilDto())),
            )
            testoppsettService.ferdigstillBehandling(dummyBehandling)

            val revurdering =
                testoppsettService.opprettRevurdering(
                    forrigeBehandling = dummyBehandling,
                    revurderFra = revurderFraDato,
                    fagsak = dummyFagsak,
                )

            vilkårRepository.insert(
                vilkår.copy(
                    fom = dummyFom,
                    tom = revurderFraDato.minusDays(1),
                    id = VilkårId.random(),
                    behandlingId = revurdering.id,
                    status = VilkårStatus.ENDRET,
                ),
            )

            vilkårperiodeRepository.insert(
                aktivitet.copy(
                    id = UUID.randomUUID(),
                    behandlingId = revurdering.id,
                    status = Vilkårstatus.UENDRET,
                ),
            )
            vilkårperiodeRepository.insert(
                målgruppe.copy(
                    id = UUID.randomUUID(),
                    behandlingId = revurdering.id,
                    status = Vilkårstatus.UENDRET,
                ),
            )

            val opphørVedtak =
                OpphørBoutgifterRequest(
                    årsakerOpphør = listOf(ÅrsakOpphør.ANNET),
                    begrunnelse = "Statsbudsjettet er tomt",
                    opphørsdato = revurderFraDato,
                )

            opphørVedtak(revurdering, opphørVedtak)

            val lagretDto = hentVedtak<OpphørBoutgifterResponse>(revurdering.id).body!!

            assertThat((lagretDto).årsakerOpphør).isEqualTo(opphørVedtak.årsakerOpphør)
            assertThat(lagretDto.begrunnelse).isEqualTo(opphørVedtak.begrunnelse)
            assertThat(lagretDto.type).isEqualTo(TypeVedtak.OPPHØR)
        }

        private fun opphørVedtak(
            behandling: Behandling,
            vedtak: OpphørBoutgifterRequest,
        ) {
            restTemplate.exchange<Map<String, Any>?>(
                localhost("api/vedtak/boutgifter/${behandling.id}/opphor"),
                HttpMethod.POST,
                HttpEntity(vedtak, headers),
            )
        }
    }

    private fun innvilgeVedtak(
        behandling: Behandling,
        vedtak: InnvilgelseBoutgifterRequest,
    ) {
        restTemplate.exchange<Map<String, Any>?>(
            localhost("api/vedtak/boutgifter/${behandling.id}/innvilgelse"),
            HttpMethod.POST,
            HttpEntity(vedtak, headers),
        )
    }

    private inline fun <reified T : VedtakBoutgifterResponse> hentVedtak(behandlingId: BehandlingId) =
        restTemplate.exchange<T>(
            localhost("api/vedtak/boutgifter/$behandlingId"),
            HttpMethod.GET,
            HttpEntity(null, headers),
        )
}
