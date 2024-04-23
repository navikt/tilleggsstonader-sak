package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.util.ProblemDetailUtil.catchProblemDetailException
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.barn
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
import java.time.YearMonth
import java.util.UUID

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

    val behandling = behandling(steg = StegType.BEREGNE_YTELSE, status = BehandlingStatus.UTREDES)
    val barn = BehandlingBarn(behandlingId = behandling.id, ident = "123")
    val stønadsperiode =
        stønadsperiode(behandlingId = behandling.id, fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 31))
    val aktivitet = aktivitet(behandling.id, fom = LocalDate.of(2023, 1, 1), tom = LocalDate.of(2023, 1, 31))
    val vilkår = vilkår(behandlingId = behandling.id, barnId = barn.id, type = VilkårType.PASS_BARN, resultat = Vilkårsresultat.OPPFYLT)

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
        val exception = catchProblemDetailException { hentVedtak(UUID.randomUUID()) }

        assertThat(exception.httpStatus).isEqualTo(HttpStatus.UNAUTHORIZED)
    }

    @Test
    fun `skal returnere empty body når det ikke finnes noe lagret`() {
        val response = hentVedtak(behandling.id)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body).isNull()
    }

    @Test
    fun `vedtaket skal være likt det man sender inn`() {
        val vedtak = lagVedtak()
        lagreVedtak(behandling, vedtak)

        val lagretDto = hentVedtak(behandling.id).body!!

        assertThat(lagretDto).isEqualTo(vedtak)
    }

    @Test
    fun `skal returnere lagret beregningsresultat som er likt beregnet`() {
        val vedtak = lagVedtak()
        val beregnet = beregn(behandling.id, vedtak).body!!
        lagreVedtak(behandling, vedtak)

        val lagretBeregning = hentLagretBeregningsresultat(behandling.id).body!!

        assertThat(lagretBeregning).isEqualTo(beregnet)
    }

    private fun lagVedtak() = InnvilgelseTilsynBarnDto(
        utgifter = mapOf(
            barn(barn.id, Utgift(YearMonth.of(2023, 1), YearMonth.of(2023, 1), 100)),
        ),
    )

    private fun lagreVedtak(
        behandling: Behandling,
        vedtak: InnvilgelseTilsynBarnDto,
    ) {
        restTemplate.exchange<Map<String, Any>?>(
            localhost("api/vedtak/tilsyn-barn/${behandling.id}"),
            HttpMethod.POST,
            HttpEntity(vedtak, headers),
        )
    }

    private fun hentVedtak(behandlingId: UUID) =
        restTemplate.exchange<InnvilgelseTilsynBarnDto>(
            localhost("api/vedtak/tilsyn-barn/$behandlingId"),
            HttpMethod.GET,
            HttpEntity(null, headers),
        )

    private fun beregn(behandlingId: UUID, vedtak: InnvilgelseTilsynBarnDto) =
        restTemplate.exchange<BeregningsresultatTilsynBarnDto>(
            localhost("api/vedtak/tilsyn-barn/$behandlingId/beregn"),
            HttpMethod.POST,
            HttpEntity(vedtak, headers),
        )

    private fun hentLagretBeregningsresultat(behandlingId: UUID) =
        restTemplate.exchange<BeregningsresultatTilsynBarnDto>(
            localhost("api/vedtak/tilsyn-barn/$behandlingId/resultat"),
            HttpMethod.GET,
            HttpEntity(null, headers),
        )
}
