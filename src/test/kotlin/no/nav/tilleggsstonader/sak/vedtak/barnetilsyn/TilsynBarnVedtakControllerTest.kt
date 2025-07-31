package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.libs.test.httpclient.ProblemDetailUtil.catchProblemDetailException
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgelseDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.client.exchange
import java.time.LocalDate
import java.util.UUID

class TilsynBarnVedtakControllerTest : IntegrationTest() {
    @Autowired
    lateinit var barnRepository: BarnRepository

    @Autowired
    lateinit var vilkårperiodeRepository: VilkårperiodeRepository

    @Autowired
    lateinit var vilkårRepository: VilkårRepository

    val fagsak = fagsak()
    val behandling = behandling(fagsak = fagsak, steg = StegType.BEREGNE_YTELSE, status = BehandlingStatus.UTREDES)
    val barn = BehandlingBarn(behandlingId = behandling.id, ident = "123")
    val vedtaksperiodeDto =
        VedtaksperiodeDto(
            id = UUID.randomUUID(),
            fom = LocalDate.of(2023, 1, 1),
            tom = LocalDate.of(2023, 1, 31),
            aktivitetType = AktivitetType.TILTAK,
            målgruppeType = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
        )
    val aktivitet = aktivitet(behandling.id, fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 31))
    val målgruppe = målgruppe(behandling.id, fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 31))
    val vilkår =
        vilkår(
            behandlingId = behandling.id,
            barnId = barn.id,
            type = VilkårType.PASS_BARN,
            resultat = Vilkårsresultat.OPPFYLT,
            fom = LocalDate.of(2023, 1, 1),
            tom = LocalDate.of(2023, 1, 31),
            utgift = 100,
        )

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(onBehalfOfToken())
        testoppsettService.opprettBehandlingMedFagsak(behandling)
        barnRepository.insert(barn)
        vilkårperiodeRepository.insert(aktivitet)
        vilkårperiodeRepository.insert(målgruppe)
        vilkårRepository.insert(vilkår)
    }

    @Test
    fun `skal validere token`() {
        headers.clear()
        val exception = catchProblemDetailException { hentVedtak<InnvilgelseTilsynBarnResponse>(BehandlingId.random()) }

        assertThat(exception.httpStatus).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `skal returnere empty body når det ikke finnes noe lagret`() {
        val response = hentVedtak<InnvilgelseTilsynBarnResponse>(behandling.id)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNull()
    }

    @Test
    fun `Skal lagre og hente innvilgelse med vedtaksperioder og begrunnelse`() {
        val vedtak = innvilgelseDto(listOf(vedtaksperiodeDto), "Jo du skjønner det, at...")
        innvilgeVedtak(behandling, vedtak)

        val lagretDto = hentVedtak<InnvilgelseTilsynBarnResponse>(behandling.id).body!!

        assertThat(lagretDto.vedtaksperioder).isEqualTo(vedtak.vedtaksperioder)
        assertThat(lagretDto.begrunnelse).isEqualTo(vedtak.begrunnelse)
        assertThat(lagretDto.type).isEqualTo(TypeVedtak.INNVILGELSE)
    }

    @Test
    fun `skal lagre og hente avslag`() {
        val vedtak =
            AvslagTilsynBarnDto(
                årsakerAvslag = listOf(ÅrsakAvslag.ANNET),
                begrunnelse = "begrunnelse",
            )

        avslåVedtak(behandling, vedtak)

        val lagretDto = hentVedtak<AvslagTilsynBarnDto>(behandling.id).body!!

        assertThat((lagretDto).årsakerAvslag).isEqualTo(vedtak.årsakerAvslag)
        assertThat(lagretDto.begrunnelse).isEqualTo(vedtak.begrunnelse)
        assertThat(lagretDto.type).isEqualTo(TypeVedtak.AVSLAG)
    }

    @Test
    fun `skal lagre og hente opphør med vedtaksperioder`() {
        innvilgeVedtak(
            behandling = behandling,
            vedtak =
                InnvilgelseTilsynBarnRequest(
                    listOf(vedtaksperiodeDto),
                    begrunnelse = "Jo du skjønner det, at...",
                ),
        )
        testoppsettService.ferdigstillBehandling(behandling)
        val revurderFra = LocalDate.of(2023, 1, 15)
        val behandlingLagreOpphør =
            testoppsettService.opprettRevurdering(
                forrigeBehandling = behandling,
                revurderFra = revurderFra,
                fagsak = fagsak,
            )

        vilkårRepository.insert(
            vilkår.copy(
                id = VilkårId.random(),
                behandlingId = behandlingLagreOpphør.id,
                status = VilkårStatus.UENDRET,
            ),
        )
        vilkårperiodeRepository.insert(
            aktivitet.copy(
                id = UUID.randomUUID(),
                behandlingId = behandlingLagreOpphør.id,
                status = Vilkårstatus.UENDRET,
            ),
        )
        vilkårperiodeRepository.insert(
            målgruppe.copy(
                id = UUID.randomUUID(),
                behandlingId = behandlingLagreOpphør.id,
                status = Vilkårstatus.UENDRET,
            ),
        )

        val vedtak =
            OpphørTilsynBarnRequest(
                årsakerOpphør = listOf(ÅrsakOpphør.ENDRING_UTGIFTER),
                begrunnelse = "endre utgifter opphør",
                opphørsdato = revurderFra,
            )

        opphørVedtak(behandlingLagreOpphør, vedtak)

        val lagretDto = hentVedtak<OpphørTilsynBarnResponse>(behandlingLagreOpphør.id).body!!

        assertThat(lagretDto.årsakerOpphør).isEqualTo(vedtak.årsakerOpphør)
        assertThat(lagretDto.begrunnelse).isEqualTo(vedtak.begrunnelse)
        assertThat(lagretDto.type).isEqualTo(TypeVedtak.OPPHØR)
    }

    private fun innvilgeVedtak(
        behandling: Behandling,
        vedtak: InnvilgelseTilsynBarnRequest,
    ) {
        restTemplate.exchange<Map<String, Any>?>(
            localhost("api/vedtak/tilsyn-barn/${behandling.id}/innvilgelse"),
            HttpMethod.POST,
            HttpEntity(vedtak, headers),
        )
    }

    private fun avslåVedtak(
        behandling: Behandling,
        vedtak: AvslagTilsynBarnDto,
    ) {
        restTemplate.exchange<Map<String, Any>?>(
            localhost("api/vedtak/tilsyn-barn/${behandling.id}/avslag"),
            HttpMethod.POST,
            HttpEntity(vedtak, headers),
        )
    }

    private fun opphørVedtak(
        behandling: Behandling,
        vedtak: OpphørTilsynBarnRequest,
    ) {
        restTemplate.exchange<Map<String, Any>?>(
            localhost("api/vedtak/tilsyn-barn/${behandling.id}/opphor"),
            HttpMethod.POST,
            HttpEntity(vedtak, headers),
        )
    }

    private inline fun <reified T : VedtakTilsynBarnResponse> hentVedtak(behandlingId: BehandlingId) =
        restTemplate.exchange<T>(
            localhost("api/vedtak/tilsyn-barn/$behandlingId"),
            HttpMethod.GET,
            HttpEntity(null, headers),
        )
}
