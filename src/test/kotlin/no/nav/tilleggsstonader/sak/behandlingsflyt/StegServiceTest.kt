package no.nav.tilleggsstonader.sak.behandlingsflyt

import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.sak.IntegrationTest
import no.nav.tilleggsstonader.sak.behandling.barn.BarnRepository
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandling.historikk.domain.BehandlingshistorikkRepository
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.infrastruktur.exception.Feil
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil
import no.nav.tilleggsstonader.sak.util.BrukerContextUtil.mockBrukerContext
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.InnvilgelseTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.Stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnBeregnYtelseSteg
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.Utgift
import no.nav.tilleggsstonader.sak.vedtak.totrinnskontroll.dto.BeslutteVedtakDto
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchIllegalStateException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import java.time.YearMonth

class StegServiceTest(
    @Autowired
    val stegService: StegService,
    @Autowired
    val behandlingshistorikkRepository: BehandlingshistorikkRepository,
    @Autowired
    val behandlingRepository: BehandlingRepository,
    @Autowired
    val barnRepository: BarnRepository,
    @Autowired
    val tilsynBarnBeregnYtelseSteg: TilsynBarnBeregnYtelseSteg,
) : IntegrationTest() {

    val stegForBeslutter = object : BehandlingSteg<String> {
        override fun utførSteg(saksbehandling: Saksbehandling, data: String) {
            val behandling = behandlingRepository.findByIdOrThrow(saksbehandling.id)
            behandlingRepository.update(behandling.copy(status = BehandlingStatus.IVERKSETTER_VEDTAK))
        }
        override fun stegType(): StegType = StegType.BESLUTTE_VEDTAK
    }

    @AfterEach
    override fun tearDown() {
        super.tearDown()
        BrukerContextUtil.clearBrukerContext()
    }

    @Test
    internal fun `skal legge inn historikkinnslag for beregn ytelse selv om behandlingen står på send til beslutter`() {
        val behandling = testoppsettService.opprettBehandlingMedFagsak(
            behandling(
                status = BehandlingStatus.UTREDES,
                steg = StegType.SEND_TIL_BESLUTTER,
            ),
        )
        val vedtakTilsynBarn = opprettVedtakTilsynBarn(behandling)
        stegService.håndterSteg(saksbehandling(behandling = behandling), tilsynBarnBeregnYtelseSteg, vedtakTilsynBarn)

        assertThat(behandlingshistorikkRepository.findByBehandlingIdOrderByEndretTidDesc(behandling.id).first().steg)
            .isEqualTo(StegType.BEREGNE_YTELSE)
    }

    @Nested
    inner class Validering {

        @Test
        fun `saksbehandler har ikke tilgang til steg som gjelder beslutter`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(
                behandling(steg = StegType.BESLUTTE_VEDTAK, status = BehandlingStatus.FATTER_VEDTAK),
            )
            mockBrukerContext("Saksbehandler123", listOf(rolleConfig.saksbehandlerRolle))

            val exception = catchThrowableOfType<Feil> {
                stegService.håndterSteg(
                    saksbehandling(behandling = behandling),
                    stegForBeslutter,
                    "",
                )
            }
            assertThat(exception)
                .hasMessage("Saksbehandler123 kan ikke utføre steg 'Beslutte vedtak' pga manglende rolle.")
        }

        @Test
        fun `beslutter har tilgang til steg som gjelder beslutter`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(
                behandling(steg = StegType.BESLUTTE_VEDTAK, status = BehandlingStatus.FATTER_VEDTAK),
            )
            mockBrukerContext("Saksbehandler123", listOf(rolleConfig.beslutterRolle))

            assertDoesNotThrow {
                stegService.håndterSteg(
                    saksbehandling(behandling = behandling),
                    stegForBeslutter,
                    "",
                )
            }
        }

        @Test
        internal fun `skal feile håndtering av ny søknad hvis en behandling er ferdigstilt`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(
                behandling(steg = StegType.BEHANDLING_FERDIGSTILT),
            )

            val vedtakTilsynBarn = opprettVedtakTilsynBarn(behandling)
            val exception = catchThrowableOfType<Feil> {
                stegService.håndterSteg(
                    saksbehandling(behandling = behandling),
                    tilsynBarnBeregnYtelseSteg,
                    vedtakTilsynBarn,
                )
            }
            assertThat(exception).hasMessage("Kan ikke utføre 'Beregne ytelse' når behandlingstatus er Opprettet")
        }

        @Disabled
        @Test
        internal fun `skal feile hvis behandling iverksettes og man prøver godkjenne saksbehandling`() {
            val behandling = behandlingSomIverksettes()

            mockBrukerContext("navIdent")
            val beslutteVedtakDto = BeslutteVedtakDto(true, "")
            val feil = catchThrowableOfType<ApiFeil> {
                // TODO
                // stegService.håndterBeslutteVedtak(saksbehandling(behandling =  behandling), beslutteVedtakDto)
            }
            assertThat(feil.message).isEqualTo("Behandlingen er allerede besluttet. Status på behandling er 'Iverksetter vedtak'")
        }
    }

    @Nested
    inner class Reset {

        @Test
        fun `kan ikke resette når behandlingen ikke har status utredes`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(
                behandling(
                    status = BehandlingStatus.IVERKSETTER_VEDTAK,
                    steg = StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT,
                ),
            )
            val exception = catchIllegalStateException {
                stegService.resetSteg(behandling.id, steg = StegType.BEREGNE_YTELSE)
            }
            assertThat(exception).hasMessageContaining("Kan ikke resette steg når status=IVERKSETTER_VEDTAK ")
        }

        @Test
        internal fun `kast feil når man resetter med et steg etter behandlingen sitt steg`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(
                behandling(
                    status = BehandlingStatus.UTREDES,
                    steg = StegType.VILKÅR,
                ),
            )

            val exception = catchIllegalStateException {
                stegService.resetSteg(behandling.id, steg = StegType.BEREGNE_YTELSE)
            }
            assertThat(exception).hasMessageContaining("Kan ikke resette behandling til steg=BEREGNE_YTELSE når behandling allerede er på VILKÅR")
        }

        @Test
        internal fun `steg på behandlingen beholdes når man resetter på samme steg`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(
                behandling(
                    status = BehandlingStatus.UTREDES,
                    steg = StegType.BEREGNE_YTELSE,
                ),
            )

            stegService.resetSteg(behandling.id, steg = StegType.BEREGNE_YTELSE)
            assertThat(behandlingRepository.findByIdOrThrow(behandling.id).steg).isEqualTo(StegType.BEREGNE_YTELSE)
        }

        @Test
        internal fun `steg på behandlingen oppdateres når man resetter med et tidligere steg`() {
            val behandling = testoppsettService.opprettBehandlingMedFagsak(
                behandling(
                    status = BehandlingStatus.UTREDES,
                    steg = StegType.BEREGNE_YTELSE,
                ),
            )

            stegService.resetSteg(behandling.id, steg = StegType.VILKÅR)
            assertThat(behandlingRepository.findByIdOrThrow(behandling.id).steg).isEqualTo(StegType.VILKÅR)
        }
    }

    private fun opprettVedtakTilsynBarn(behandling: Behandling): InnvilgelseTilsynBarnDto {
        val barn = barnRepository.insert(BehandlingBarn(behandlingId = behandling.id, ident = "123"))

        val måned = YearMonth.of(2023, 1)
        return InnvilgelseTilsynBarnDto(
            stønadsperioder = listOf(Stønadsperiode(måned.atDay(2), måned.atDay(2))),
            utgifter = mapOf(barn.id to listOf(Utgift(måned, måned, 100))),
        )
    }

    private fun behandlingSomIverksettes(): Behandling {
        val nyBehandling =
            behandling(status = BehandlingStatus.IVERKSETTER_VEDTAK, steg = StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT)
        return testoppsettService.opprettBehandlingMedFagsak(nyBehandling)
    }
}
