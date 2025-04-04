package no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag

import io.mockk.every
import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.PdlClientConfig
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.PdlClientConfig.Companion.familierelasjonerBarn
import no.nav.tilleggsstonader.sak.infrastruktur.mocks.PdlClientConfig.Companion.resetMock
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.GeneriskFaktaGrunnlagTestUtil.faktaGrunnlagBarnAnnenForelder
import no.nav.tilleggsstonader.sak.opplysninger.pdl.PdlClient
import no.nav.tilleggsstonader.sak.util.PdlTestdataHelper.pdlBarn
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.behandlingBarn
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.fagsakpersoner
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.vedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.UtgiftBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningTestUtil.vedtaksperiodeBeregning
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class FaktaGrunnlagServiceTest : IntegrationTest() {
    @Autowired
    lateinit var faktaGrunnlagService: FaktaGrunnlagService

    @Autowired
    lateinit var faktaGrunnlagRepository: FaktaGrunnlagRepository

    @Autowired
    lateinit var vedtakRepository: VedtakRepository

    @Autowired
    lateinit var barnService: BarnService

    @Autowired
    lateinit var pdlClient: PdlClient

    @BeforeEach
    fun setUp() {
        every { pdlClient.hentBarn(any()) } returns
            mapOf(
                PdlClientConfig.BARN_FNR to pdlBarn(forelderBarnRelasjon = familierelasjonerBarn()),
            )
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        resetMock(pdlClient)
    }

    @Nested
    inner class FaktaGrunnlagBarnAndreForeldreSaksinformasjonTest {
        val fagsakAnnenForelder = fagsak(identer = fagsakpersoner(PdlClientConfig.ANNEN_FORELDER_FNR))
        val behandlingAnnenForelder =
            behandling(
                fagsakAnnenForelder,
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.FERDIGSTILT,
            )

        val fagsak = fagsak(identer = fagsakpersoner(PdlClientConfig.SØKER_FNR))
        val behandling = behandling(fagsak)

        val vedtakperiode = Datoperiode(fom = LocalDate.of(2025, 1, 1), tom = LocalDate.of(2025, 1, 10))

        @BeforeEach
        fun setUp() {
            testoppsettService.lagreFagsak(fagsak)
            testoppsettService.lagre(behandling, opprettGrunnlagsdata = false)
            val barn = behandlingBarn(behandlingId = behandling.id, personIdent = PdlClientConfig.BARN_FNR)
            val barn2 = behandlingBarn(behandlingId = behandling.id, personIdent = PdlClientConfig.BARN2_FNR)
            barnService.opprettBarn(listOf(barn, barn2))
        }

        @Test
        fun `skal hente opp riktig type av faktagrunnlag`() {
            faktaGrunnlagRepository.insert(faktaGrunnlagBarnAnnenForelder(behandlingId = behandling.id))

            val grunnlag = hentGrunnlag()

            assertThat(grunnlag).hasSize(1)
            assertThat(grunnlag[0].data).isInstanceOf(FaktaGrunnlagBarnAndreForeldreSaksinformasjon::class.java)
        }

        @Test
        fun `skal opprette grunnlag for annen forelder`() {
            opprettVedtakAnnenForelder()

            faktaGrunnlagService.opprettGrunnlag(behandlingId = behandling.id)

            val grunnlagAndreForeldre = hentGrunnlag()
            assertThat(grunnlagAndreForeldre).hasSize(1)
            with(grunnlagAndreForeldre.single().data) {
                assertThat(this.identBarn).isEqualTo(PdlClientConfig.BARN_FNR)
                assertThat(this.andreForeldre).hasSize(1)
                assertThat(this.andreForeldre[0].ident).isEqualTo(PdlClientConfig.ANNEN_FORELDER_FNR)
                assertThat(this.andreForeldre[0].vedtaksperioderBarn).containsExactly(vedtakperiode)
            }
        }

        @Test
        fun `skal opprette grunnlag selv om annen forelder ikke har vedtak`() {
            every { pdlClient.hentBarn(any()) } returns
                mapOf(
                    PdlClientConfig.BARN_FNR to pdlBarn(),
                )

            faktaGrunnlagService.opprettGrunnlag(behandlingId = behandling.id)

            val grunnlagAndreForeldre = hentGrunnlag()

            assertThat(grunnlagAndreForeldre).hasSize(1)
            grunnlagAndreForeldre.forEach { grunnlag ->
                assertThat(grunnlag.data.identBarn).isEqualTo(PdlClientConfig.BARN_FNR)
                assertThat(grunnlag.data.andreForeldre).isEmpty()
            }
        }

        private fun hentGrunnlag() = faktaGrunnlagService.hentGrunnlag<FaktaGrunnlagBarnAndreForeldreSaksinformasjon>(behandling.id)

        private fun opprettVedtakAnnenForelder() {
            testoppsettService.lagreFagsak(fagsakAnnenForelder)
            testoppsettService.lagre(behandlingAnnenForelder, opprettGrunnlagsdata = false)
            val barn = behandlingBarn(behandlingId = behandlingAnnenForelder.id, personIdent = PdlClientConfig.BARN_FNR)
            barnService.opprettBarn(listOf(barn))

            val vedtaksperiodeBeregning = vedtaksperiodeBeregning(fom = vedtakperiode.fom, tom = vedtakperiode.tom)
            val beregningsgrunnlag =
                beregningsgrunnlag(
                    vedtaksperioder = listOf(vedtaksperiodeGrunnlag(vedtaksperiodeBeregning)),
                    utgifter = listOf(UtgiftBarn(barnId = barn.id, 100)),
                )
            val beregningsresultat = beregningsresultatForMåned(grunnlag = beregningsgrunnlag)
            val vedtak =
                innvilgetVedtak(
                    behandlingId = behandlingAnnenForelder.id,
                    beregningsresultat = BeregningsresultatTilsynBarn(listOf(beregningsresultat)),
                )
            vedtakRepository.insert(vedtak)
        }
    }
}
