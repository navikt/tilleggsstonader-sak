package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.UkeIÅr
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingResultat
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingStatus
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
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
    fun `skal vise kjøreliste etter lagret innvilgelse uten å vente på beslutter`() {
        val behandling =
            saksbehandling(
                fagsak = fagsak(stønadstype = Stønadstype.DAGLIG_REISE_TSR),
                type = BehandlingType.REVURDERING,
                status = BehandlingStatus.UTREDES,
                steg = StegType.KJØRELISTE,
                forrigeIverksatteBehandlingId = BehandlingId.random(),
            )
        every { vedtakRepository.findById(behandling.id) } returns
            Optional.of(DagligReiseTestUtil.innvilgelse(behandlingId = behandling.id))
        every { vedtakRepository.harRammevedtak(listOf(behandling.forrigeIverksatteBehandlingId!!)) } returns true

        assertThat(behandling.resultat).isEqualTo(BehandlingResultat.IKKE_SATT)
        assertThat(dagligReiseVedtakService.kanViseKjøreliste(behandling)).isTrue
        assertThat(
            dagligReiseVedtakService.skalBehandleKjøreliste(behandling.copy(steg = StegType.BEREGNE_YTELSE)),
        ).isTrue
        assertThat(
            dagligReiseVedtakService.kanViseKjøreliste(
                behandling.copy(
                    steg = StegType.BEHANDLING_FERDIGSTILT,
                    status = BehandlingStatus.FERDIGSTILT,
                    resultat = BehandlingResultat.INNVILGET,
                ),
            ),
        ).isTrue
    }

    @Test
    fun `skal vise kjøreliste ved opphør selv uten reiser i aktuelt rammevedtak`() {
        val behandling =
            saksbehandling(
                fagsak = fagsak(stønadstype = Stønadstype.DAGLIG_REISE_TSO),
                type = BehandlingType.REVURDERING,
                steg = StegType.KJØRELISTE,
                forrigeIverksatteBehandlingId = BehandlingId.random(),
            )
        val vedtak =
            GeneriskVedtak(
                behandlingId = behandling.id,
                type = TypeVedtak.OPPHØR,
                data =
                    OpphørDagligReise(
                        vedtaksperioder = emptyList(),
                        beregningsresultat = DagligReiseTestUtil.defaultBeregningsresultat,
                        rammevedtakPrivatBil = null,
                        årsaker = listOf(ÅrsakOpphør.ANNET),
                        begrunnelse = "Opphør av alle perioder",
                        beregningsplan = Beregningsplan(Beregningsomfang.FRA_DATO, 1 januar 2025),
                    ),
                gitVersjon = "123",
                tidligsteEndring = null,
                opphørsdato = 1 januar 2025,
            )
        every { vedtakRepository.findById(behandling.id) } returns Optional.of(vedtak)
        every { vedtakRepository.harRammevedtak(listOf(behandling.forrigeIverksatteBehandlingId!!)) } returns true

        assertThat(dagligReiseVedtakService.kanViseKjøreliste(behandling)).isTrue
    }

    @Test
    fun `skal ikke vise kjøreliste uten aktuelt vedtak selv om forrige har rammevedtak`() {
        val behandling =
            saksbehandling(
                fagsak = fagsak(stønadstype = Stønadstype.DAGLIG_REISE_TSO),
                steg = StegType.KJØRELISTE,
                forrigeIverksatteBehandlingId = BehandlingId.random(),
            )
        every { vedtakRepository.findById(behandling.id) } returns Optional.empty()

        assertThat(dagligReiseVedtakService.kanViseKjøreliste(behandling)).isFalse
        assertThat(dagligReiseVedtakService.skalBehandleKjøreliste(behandling)).isFalse
        verify(exactly = 0) { vedtakRepository.harRammevedtak(any()) }
    }

    @Test
    fun `skal ikke vise kjøreliste eller velge kjøreliste som neste steg ved lagret avslag`() {
        val behandling =
            saksbehandling(
                fagsak = fagsak(stønadstype = Stønadstype.DAGLIG_REISE_TSO),
                steg = StegType.SIMULERING,
                forrigeIverksatteBehandlingId = BehandlingId.random(),
            )
        every { vedtakRepository.findById(behandling.id) } returns
            Optional.of(DagligReiseTestUtil.avslag(behandlingId = behandling.id))

        assertThat(dagligReiseVedtakService.kanViseKjøreliste(behandling)).isFalse
        assertThat(dagligReiseVedtakService.skalBehandleKjøreliste(behandling)).isFalse
        verify(exactly = 0) { vedtakRepository.harRammevedtak(any()) }
    }

    @ParameterizedTest
    @EnumSource(StegType::class, names = ["INNGANGSVILKÅR", "VILKÅR", "BEREGNE_YTELSE"])
    fun `skal skjule kjøreliste før vedtaksvalg og etter tilbakesteg selv om et gammelt vedtak finnes`(steg: StegType) {
        val behandling =
            saksbehandling(
                fagsak = fagsak(stønadstype = Stønadstype.DAGLIG_REISE_TSO),
                steg = steg,
                forrigeIverksatteBehandlingId = BehandlingId.random(),
            )
        every { vedtakRepository.findById(behandling.id) } returns
            Optional.of(DagligReiseTestUtil.innvilgelse(behandlingId = behandling.id))

        assertThat(dagligReiseVedtakService.kanViseKjøreliste(behandling)).isFalse
        verify(exactly = 0) { vedtakRepository.findById(any()) }
    }

    @ParameterizedTest
    @EnumSource(BehandlingResultat::class, names = ["HENLAGT", "AVSLÅTT"])
    fun `skal ikke vise kjøreliste på henlagt eller avslått behandling med gammelt vedtak`(resultat: BehandlingResultat) {
        val behandling =
            saksbehandling(
                fagsak = fagsak(stønadstype = Stønadstype.DAGLIG_REISE_TSO),
                steg = StegType.BEHANDLING_FERDIGSTILT,
                status = BehandlingStatus.FERDIGSTILT,
                resultat = resultat,
                forrigeIverksatteBehandlingId = BehandlingId.random(),
            )
        every { vedtakRepository.findById(behandling.id) } returns
            Optional.of(DagligReiseTestUtil.innvilgelse(behandlingId = behandling.id))

        assertThat(dagligReiseVedtakService.kanViseKjøreliste(behandling)).isFalse
        verify(exactly = 0) { vedtakRepository.findById(any()) }
    }

    @Test
    fun `skal ikke vise kjøreliste hvis rammevedtaket bare finnes på aktuell behandling`() {
        val behandling =
            saksbehandling(
                fagsak = fagsak(stønadstype = Stønadstype.DAGLIG_REISE_TSO),
                steg = StegType.SIMULERING,
                forrigeIverksatteBehandlingId = BehandlingId.random(),
            )
        every { vedtakRepository.findById(behandling.id) } returns
            Optional.of(DagligReiseTestUtil.innvilgelse(behandlingId = behandling.id))
        every { vedtakRepository.harRammevedtak(listOf(behandling.forrigeIverksatteBehandlingId!!)) } returns false

        assertThat(dagligReiseVedtakService.kanViseKjøreliste(behandling)).isFalse
        assertThat(
            dagligReiseVedtakService.kanViseKjøreliste(behandling.copy(forrigeIverksatteBehandlingId = null)),
        ).isFalse
    }

    @Test
    fun `skal bevare kjørelistefaner i egne kjørelistebehandlinger`() {
        val behandling =
            saksbehandling(
                fagsak = fagsak(stønadstype = Stønadstype.DAGLIG_REISE_TSO),
                type = BehandlingType.KJØRELISTE,
                steg = StegType.REGISTRER_KJØRELISTE,
            )

        assertThat(dagligReiseVedtakService.kanViseKjøreliste(behandling)).isTrue
        assertThat(
            dagligReiseVedtakService.kanViseKjøreliste(behandling.copy(resultat = BehandlingResultat.HENLAGT)),
        ).isFalse
        verify(exactly = 0) { vedtakRepository.findById(any()) }
    }

    @Test
    fun `skal ikke hente vedtak for andre stønadstyper`() {
        val behandling = saksbehandling(fagsak = fagsak(stønadstype = Stønadstype.BARNETILSYN), steg = StegType.SIMULERING)

        assertThat(dagligReiseVedtakService.kanViseKjøreliste(behandling)).isFalse
        verify(exactly = 0) { vedtakRepository.findById(any()) }
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
