package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.ProblemDetailUtil.catchProblemDetailException
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgelseDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.VedtakTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.web.client.exchange
import java.time.LocalDate

class TilsynBarnVedtakControllerTest(
    @Autowired
    val barnRepository: BarnRepository,
    @Autowired
    val stønadsperiodeRepository: StønadsperiodeRepository,
    @Autowired
    val vilkårperiodeRepository: VilkårperiodeRepository,
    @Autowired
    val vilkårRepository: VilkårRepository,
) : IntegrationTest() {
    val fagsak = fagsak()
    val behandling = behandling(fagsak = fagsak, steg = StegType.BEREGNE_YTELSE, status = BehandlingStatus.UTREDES)
    val barn = BehandlingBarn(behandlingId = behandling.id, ident = "123")
    val stønadsperiode =
        stønadsperiode(behandlingId = behandling.id, fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 31))
    val aktivitet = aktivitet(behandling.id, fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 31))
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
        stønadsperiodeRepository.insert(stønadsperiode)
        vilkårperiodeRepository.insert(aktivitet)
        vilkårRepository.insert(vilkår)
    }

    @Test
    fun `skal validere token`() {
        headers.clear()
        val exception = catchProblemDetailException { hentVedtak(BehandlingId.random()) }

        assertThat(exception.httpStatus).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `skal returnere empty body når det ikke finnes noe lagret`() {
        val response = hentVedtak(behandling.id)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNull()
    }

    @Test
    fun `Ved innvilgelse skal utgifter på vedtaket være likt det man sender inn`() {
        val vedtak = innvilgelseDto()
        innvilgeVedtak(behandling, vedtak)

        val lagretDto = hentVedtak(behandling.id).body!!

        assertThat(lagretDto.type).isEqualTo(TypeVedtak.INNVILGELSE)
    }

    @Test
    fun `skal lagre og hente avslag`() {
        val vedtak =
            AvslagTilsynBarnDto(
                årsakerAvslag = listOf(ÅrsakAvslag.INGEN_AKTIVITET),
                begrunnelse = "begrunnelse",
            )

        avslåVedtak(behandling, vedtak)

        val lagretDto = hentVedtak(behandling.id).body!!

        assertThat((lagretDto as AvslagTilsynBarnDto).årsakerAvslag).isEqualTo(vedtak.årsakerAvslag)
        assertThat(lagretDto.begrunnelse).isEqualTo(vedtak.begrunnelse)
        assertThat(lagretDto.type).isEqualTo(TypeVedtak.AVSLAG)
    }

    @Test
    fun `skal lagre og hente opphør`() {
        innvilgeVedtak(behandling, innvilgelseDto())
        testoppsettService.ferdigstillBehandling(behandling)
        val behandlingLagreOpphør =
            testoppsettService.opprettRevurdering(
                forrigeBehandling = behandling,
                revurderFra = LocalDate.now(),
                fagsak = fagsak,
            )

        val vedtak =
            OpphørTilsynBarnRequest(
                årsakerOpphør = listOf(ÅrsakOpphør.ENDRING_UTGIFTER),
                begrunnelse = "endre utgifter opphør",
            )

        opphørVedtak(behandlingLagreOpphør, vedtak)

        val lagretDto = hentVedtak(behandlingLagreOpphør.id).body!!

        assertThat((lagretDto as OpphørTilsynBarnRequest).årsakerOpphør).isEqualTo(vedtak.årsakerOpphør)
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

    private fun hentVedtak(behandlingId: BehandlingId) =
        restTemplate.exchange<VedtakTilsynBarnDto>(
            localhost("api/vedtak/tilsyn-barn/$behandlingId"),
            HttpMethod.GET,
            HttpEntity(null, headers),
        )
}
