package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.tilleggsstonader.libs.utils.dato.UkeIÅr
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBilProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.Optional

class DagligReiseVedtakServiceTest {
    private val vedtakRepository = mockk<VedtakRepository>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>(relaxed = true)
    private val simuleringService = mockk<SimuleringService>(relaxed = true)
    private val satsPrivatBilProvider = mockk<SatsPrivatBilProvider>()

    private val dagligReiseVedtakService =
        DagligReiseVedtakService(
            vedtakRepository = vedtakRepository,
            tilkjentYtelseService = tilkjentYtelseService,
            simuleringService = simuleringService,
            satsPrivatBilProvider = satsPrivatBilProvider,
        )

    @Test
    fun `skal returnere true når forrige behandling har rammevedtak`() {
        val forrigeBehandlingId = BehandlingId.random()
        every { vedtakRepository.harRammevedtak(listOf(forrigeBehandlingId)) } returns true

        val harRammevedtak =
            dagligReiseVedtakService.forrigeIverksatteBehandlingHarRammevedtakForPrivatBil(
                forrigeIverksatteBehandlingId = forrigeBehandlingId,
            )

        assertThat(harRammevedtak).isTrue
    }

    @Test
    fun `skal returnere false når forrige behandling ikke har rammevedtak`() {
        val forrigeBehandlingId = BehandlingId.random()
        every { vedtakRepository.harRammevedtak(listOf(forrigeBehandlingId)) } returns false

        val harRammevedtak =
            dagligReiseVedtakService.forrigeIverksatteBehandlingHarRammevedtakForPrivatBil(
                forrigeIverksatteBehandlingId = forrigeBehandlingId,
            )

        assertThat(harRammevedtak).isFalse
    }

    @Test
    fun `skal returnere false når det ikke finnes forrige behandling`() {
        val harRammevedtak =
            dagligReiseVedtakService.forrigeIverksatteBehandlingHarRammevedtakForPrivatBil(
                forrigeIverksatteBehandlingId = null,
            )

        assertThat(harRammevedtak).isFalse
    }

    @Test
    fun `skal gjenbruke opphørsvedtak som innvilgelse for kjøreliste`() {
        val forrigeBehandlingId = BehandlingId.random()
        val nyBehandlingId = BehandlingId.random()
        val eksisterendeVedtak =
            GeneriskVedtak(
                behandlingId = forrigeBehandlingId,
                type = TypeVedtak.OPPHØR,
                data =
                    OpphørDagligReise(
                        vedtaksperioder = DagligReiseTestUtil.defaultVedtaksperioder,
                        beregningsresultat = DagligReiseTestUtil.defaultBeregningsresultat,
                        rammevedtakPrivatBil = DagligReiseTestUtil.defaultRammevedtakPrivatBil,
                        årsaker = listOf(ÅrsakOpphør.ANNET),
                        begrunnelse = "opphør",
                        beregningsplan = Beregningsplan(Beregningsomfang.FRA_DATO, 1 januar 2025),
                    ),
                gitVersjon = "123",
                tidligsteEndring = null,
                opphørsdato = null,
            )
        val lagretVedtak = slot<GeneriskVedtak<*>>()

        every { vedtakRepository.findById(forrigeBehandlingId) } returns Optional.of(eksisterendeVedtak)
        every { vedtakRepository.insert(capture(lagretVedtak)) } answers { firstArg() }

        dagligReiseVedtakService.gjenbrukVedtak(
            forrigeIverksatteBehandlingId = forrigeBehandlingId,
            nyBehandlingId = nyBehandlingId,
        )

        assertThat(lagretVedtak.captured.type).isEqualTo(TypeVedtak.INNVILGELSE)
        assertThat(lagretVedtak.captured.data).isInstanceOf(InnvilgelseDagligReise::class.java)
        assertThat((lagretVedtak.captured.data as InnvilgelseDagligReise).vedtaksperioder)
            .isEqualTo(eksisterendeVedtak.data.vedtaksperioder)
        assertThat((lagretVedtak.captured.data as InnvilgelseDagligReise).beregningsplan.omfang)
            .isEqualTo(Beregningsomfang.KUN_NYE_KJORELISTE_UKER)
    }

    @Test
    fun `skal ikke oppdatere satser der ingen behøver oppdatering`() {
        val fom = UkeIÅr(2, 2025).mandag()
        val tom = UkeIÅr(2, 2025).søndag()
        val eksisterendeSats =
            SatsPrivatBil(
                fom = fom,
                tom = tom,
                beløp = 100.toBigDecimal(),
                bekreftet = false,
            )

        val behandlingId = BehandlingId.random()
        val eksisterendeRammevedtak =
            DagligReiseTestUtil.defaultRammevedtakPrivatBil.copy(
                reiser =
                    DagligReiseTestUtil.defaultRammevedtakPrivatBil.reiser.map {
                        it.copy(
                            grunnlag =
                                it.grunnlag.copy(
                                    delperioder =
                                        it.grunnlag.delperioder.map { delperiode ->
                                            delperiode.copy(
                                                satser =
                                                    delperiode.satser.map { sats ->
                                                        sats.copy(
                                                            fom = eksisterendeSats.fom,
                                                            tom = eksisterendeSats.tom,
                                                            kilometersats = eksisterendeSats.beløp,
                                                            satsBekreftetVedVedtakstidspunkt = eksisterendeSats.bekreftet,
                                                        )
                                                    },
                                            )
                                        },
                                ),
                        )
                    },
            )

        every {
            satsPrivatBilProvider.alleSatser
        } returns
            listOf(eksisterendeSats)

        val oppdatertRammevedtak =
            dagligReiseVedtakService.oppdaterRammevedtakHvisNyeSatser(
                behandlingId,
                eksisterendeRammevedtak,
            )

        assertThat(oppdatertRammevedtak).isEqualTo(eksisterendeRammevedtak)
    }

    @Test
    fun `skal oppdatere satser hvis de har tilgjengelig oppdatering`() {
        val fom = UkeIÅr(2, 2025).mandag()
        val tom = UkeIÅr(2, 2025).søndag()
        val gammelSats =
            SatsPrivatBil(
                fom = fom,
                tom = tom,
                beløp = 100.toBigDecimal(),
                bekreftet = false,
            )

        val behandlingId = BehandlingId.random()
        val eksisterendeRammevedtak =
            DagligReiseTestUtil.defaultRammevedtakPrivatBil.copy(
                reiser =
                    DagligReiseTestUtil.defaultRammevedtakPrivatBil.reiser.map {
                        it.copy(
                            grunnlag =
                                it.grunnlag.copy(
                                    delperioder =
                                        it.grunnlag.delperioder.map { delperiode ->
                                            delperiode.copy(
                                                satser =
                                                    delperiode.satser.map { sats ->
                                                        sats.copy(
                                                            fom = gammelSats.fom,
                                                            tom = gammelSats.tom,
                                                            kilometersats = gammelSats.beløp,
                                                            satsBekreftetVedVedtakstidspunkt = gammelSats.bekreftet,
                                                        )
                                                    },
                                            )
                                        },
                                ),
                        )
                    },
            )

        val nySats =
            SatsPrivatBil(
                fom = fom,
                tom = tom,
                beløp = 500.toBigDecimal(),
            )

        every {
            satsPrivatBilProvider.alleSatser
        } returns
            listOf(nySats)

        every { vedtakRepository.findById(behandlingId) } returns
            Optional.of(
                DagligReiseTestUtil.innvilgelse(
                    behandlingId = behandlingId,
                    rammevedtakPrivatBil = eksisterendeRammevedtak,
                ),
            )
        every { vedtakRepository.update(any()) } answers { firstArg() }

        val oppdatertRammevedtak =
            dagligReiseVedtakService.oppdaterRammevedtakHvisNyeSatser(
                behandlingId,
                eksisterendeRammevedtak,
            )

        assertThat(oppdatertRammevedtak).isNotEqualTo(eksisterendeRammevedtak)
        assertThat(
            oppdatertRammevedtak.reiser
                .flatMap { it.grunnlag.delperioder }
                .flatMap { it.satser }
                .map { it.kilometersats },
        ).containsOnly(nySats.beløp)
    }
}
