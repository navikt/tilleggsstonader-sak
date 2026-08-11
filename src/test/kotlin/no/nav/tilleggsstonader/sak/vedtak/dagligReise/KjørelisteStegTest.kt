package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.tilleggsstonader.libs.feil.ApiFeil
import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.tilUkeIÅr
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.privatbil.KjørelisteId
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørelisteService
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUke
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.AvklartKjørtUkeStatus
import no.nav.tilleggsstonader.sak.privatbil.avklartedager.UkeStatus
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil.rammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.privatBil.PrivatBilBeregningService
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBilProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.math.BigDecimal

class KjørelisteStegTest {
    private val privatBilBeregningService = mockk<PrivatBilBeregningService>(relaxed = true)
    private val vedtakService = mockk<VedtakService>(relaxed = true)
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>(relaxed = true)
    private val dagligReiseVedtakService = mockk<DagligReiseVedtakService>(relaxed = true)
    private val avklartKjørelisteService = mockk<AvklartKjørelisteService>()
    private val satsPrivatBilProvider = mockk<SatsPrivatBilProvider>()

    private val steg =
        KjørelisteSteg(
            privatBilBeregningService = privatBilBeregningService,
            vedtakService = vedtakService,
            arbeidsfordelingService = arbeidsfordelingService,
            dagligReiseVedtakService = dagligReiseVedtakService,
            avklartKjørelisteService = avklartKjørelisteService,
            satsPrivatBilProvider = satsPrivatBilProvider,
        )

    private val saksbehandling = saksbehandling(steg = StegType.KJØRELISTE)

    @BeforeEach
    fun setUp() {
        every { avklartKjørelisteService.hentAvklarteUkerForBehandling(saksbehandling.id) } returns emptyList()
    }

    @Test
    fun `skal ikke kaste feil når det ikke finnes avklarte uker`() {
        assertDoesNotThrow {
            steg.validerSteg(saksbehandling)
        }

        verify(exactly = 1) {
            avklartKjørelisteService.hentAvklarteUkerForBehandling(saksbehandling.id)
        }
    }

    @Test
    fun `skal ikke kaste feil når ingen uker har avvik`() {
        every { avklartKjørelisteService.hentAvklarteUkerForBehandling(saksbehandling.id) } returns
            listOf(
                avklartKjørtUke(UkeStatus.OK_AUTOMATISK),
                avklartKjørtUke(UkeStatus.OK_MANUELT),
                avklartKjørtUke(UkeStatus.IKKE_MOTTATT_KJØRELISTE),
            )

        assertDoesNotThrow {
            steg.validerSteg(saksbehandling)
        }

        verify(exactly = 1) {
            avklartKjørelisteService.hentAvklarteUkerForBehandling(saksbehandling.id)
        }
    }

    @Test
    fun `skal kaste feil når det finnes minst én uke med avvik`() {
        every { avklartKjørelisteService.hentAvklarteUkerForBehandling(saksbehandling.id) } returns
            listOf(
                avklartKjørtUke(UkeStatus.OK_MANUELT),
                avklartKjørtUke(UkeStatus.AVVIK),
            )

        val feil =
            catchThrowableOfType<ApiFeil> {
                steg.validerSteg(saksbehandling)
            }

        assertThat(feil.message).isEqualTo("Kan ikke gå videre til neste steg da det finnes uker med avvik")
        verify(exactly = 1) {
            avklartKjørelisteService.hentAvklarteUkerForBehandling(saksbehandling.id)
        }
    }

    @Test
    fun `skal ikke oppdatere rammevedtak når ingen satser kan oppdateres`() {
        val eksisterendeRammevedtak =
            rammevedtakPrivatBil(
                fom = 1 januar 2027,
                tom = 31 desember 2027,
                satsBekreftetVedVedtakstidspunkt = false,
                kilometersats = BigDecimal("3.50"),
            )
        every { vedtakService.hentVedtakEllerFeil(saksbehandling.id) } returns
            DagligReiseTestUtil.innvilgelse(
                behandlingId = saksbehandling.id,
                rammevedtakPrivatBil = eksisterendeRammevedtak,
            )
        every { satsPrivatBilProvider.alleSatser } returns
            listOf(
                SatsPrivatBil(
                    fom = 1 januar 2026,
                    tom = 31 desember 2026,
                    beløp = BigDecimal("2.94"),
                    bekreftet = true,
                ),
            )

        steg.utførSteg(saksbehandling, null)

        verify(exactly = 0) {
            dagligReiseVedtakService.oppdaterVedtakMedNyttRammevedtakPrivatBil(any(), any())
        }
    }

    @Test
    fun `skal oppdatere rammevedtak når ubekreftet sats har blitt bekreftet`() {
        val eksisterendeRammevedtak =
            rammevedtakPrivatBil(
                fom = 1 januar 2025,
                tom = 31 desember 2025,
                satsBekreftetVedVedtakstidspunkt = false,
                kilometersats = BigDecimal("2.70"),
            )
        every { vedtakService.hentVedtakEllerFeil(saksbehandling.id) } returns
            DagligReiseTestUtil.innvilgelse(
                behandlingId = saksbehandling.id,
                rammevedtakPrivatBil = eksisterendeRammevedtak,
            )
        every { satsPrivatBilProvider.alleSatser } returns
            listOf(
                SatsPrivatBil(
                    fom = 1 januar 2025,
                    tom = 31 desember 2025,
                    beløp = BigDecimal("2.88"),
                    bekreftet = true,
                ),
            )

        steg.utførSteg(saksbehandling, null)

        val oppdatertRammevedtak = slot<RammevedtakPrivatBil>()
        verify(exactly = 1) {
            dagligReiseVedtakService.oppdaterVedtakMedNyttRammevedtakPrivatBil(
                behandlingId = saksbehandling.id,
                rammevedtakPrivatBil = capture(oppdatertRammevedtak),
            )
        }

        val oppdatertSats =
            oppdatertRammevedtak.captured.reiser
                .first()
                .grunnlag.delperioder
                .first()
                .satser
                .first()
        assertThat(oppdatertSats.satsBekreftetVedVedtakstidspunkt).isTrue()
        assertThat(oppdatertSats.kilometersats).isEqualTo(BigDecimal("2.88"))
    }

    private fun avklartKjørtUke(status: UkeStatus): AvklartKjørtUke {
        val fom = 1 januar 2024
        val tom = 7 januar 2024

        return AvklartKjørtUke(
            behandlingId = saksbehandling.id,
            kjørelisteId = KjørelisteId.random(),
            reiseId = dummyReiseId,
            fom = fom,
            tom = tom,
            uke = fom.tilUkeIÅr(),
            status = status,
            avklartKjørtUkeStatus = AvklartKjørtUkeStatus.NY,
            dager = emptySet(),
        )
    }
}
