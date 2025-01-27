package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.OpphørValideringService
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgelseDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.opphørDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnBeregningService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnUtgiftService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.UtgiftBeregning
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class TilsynBarnBeregnYtelseStegTest {
    private val repository = mockk<VedtakRepository>(relaxed = true)
    private val barnService = mockk<BarnService>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>(relaxed = true)
    private val simuleringService = mockk<SimuleringService>(relaxed = true)
    private val stønadsperiodeService = mockk<StønadsperiodeRepository>(relaxed = true)
    private val vilkårperiodeRepository = mockk<VilkårperiodeRepository>(relaxed = true)
    private val tilsynBarnUtgiftService = mockk<TilsynBarnUtgiftService>(relaxed = true)
    private val opphørValideringService = mockk<OpphørValideringService>(relaxed = true)

    val tilsynBarnBeregningService =
        TilsynBarnBeregningService(
            stønadsperiodeRepository = stønadsperiodeService,
            vilkårperiodeRepository = vilkårperiodeRepository,
            tilsynBarnUtgiftService = tilsynBarnUtgiftService,
            repository = repository,
        )
    val steg =
        TilsynBarnBeregnYtelseSteg(
            beregningService = tilsynBarnBeregningService,
            vedtakRepository = repository,
            tilkjentytelseService = tilkjentYtelseService,
            simuleringService = simuleringService,
            opphørValideringService = opphørValideringService,
        )

    val saksbehandling = saksbehandling()
    val måned = YearMonth.of(2023, 1)
    val barn = BehandlingBarn(behandlingId = saksbehandling.id, ident = "")

    @BeforeEach
    fun setUp() {
        every { barnService.finnBarnPåBehandling(saksbehandling.id) } returns listOf(barn)
        every { repository.insert(any()) } answers { firstArg() }
        val fom = LocalDate.of(2023, 1, 1)
        val tom = LocalDate.of(2023, 1, 31)
        mockStønadsperioder(fom, tom, saksbehandling.id)
        mockVilkårperioder(fom, tom, saksbehandling.id)
        every { tilsynBarnUtgiftService.hentUtgifterTilBeregning(any()) } returns
            mapOf(barn.id to listOf(UtgiftBeregning(YearMonth.now(), YearMonth.now(), 1)))
    }

    @Test
    fun `skal slette data som finnes fra før, før man lagrer ny data`() {
        val vedtak = innvilgelseDto()
        steg.utførOgReturnerNesteSteg(saksbehandling, vedtak)

        verifyOrder {
            repository.deleteById(saksbehandling.id)
            tilkjentYtelseService.slettTilkjentYtelseForBehandling(saksbehandling)
            simuleringService.slettSimuleringForBehandling(saksbehandling)

            repository.insert(any())
            tilkjentYtelseService.opprettTilkjentYtelse(saksbehandling, any())
        }
        verify(exactly = 0) {
            simuleringService.hentOgLagreSimuleringsresultat(any())
        }
    }

    @Test
    fun `skal returnere neste steg SIMULERING ved førstegangsbehandling`() {
        val vedtak = innvilgelseDto()

        val nesteSteg = steg.utførOgReturnerNesteSteg(saksbehandling, vedtak)

        assertThat(saksbehandling.type).isEqualTo(BehandlingType.FØRSTEGANGSBEHANDLING)
        assertThat(nesteSteg).isEqualTo(StegType.SIMULERING)
    }

    @Test
    fun `skal feile dersom man velger opphør på en førstegangsbehandling`() {
        val vedtak = opphørDto()

        assertThatThrownBy {
            steg.utførOgReturnerNesteSteg(saksbehandling, vedtak)
        }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi behandlingen er en førstegangsbehandling")
    }

    @Test
    fun `skal returnere neste steg SIMULERING ved revurdering`() {
        val revurdering = saksbehandling(type = BehandlingType.REVURDERING)

        val vedtak = innvilgelseDto()

        mockVilkårperioder(behandlingId = revurdering.id)
        mockStønadsperioder(behandlingId = revurdering.id)

        val nesteSteg = steg.utførOgReturnerNesteSteg(revurdering, vedtak)
        assertThat(revurdering.type).isEqualTo(BehandlingType.REVURDERING)
        assertThat(nesteSteg).isEqualTo(StegType.SIMULERING)
    }

    private fun mockVilkårperioder(
        fom: LocalDate = LocalDate.of(2023, 1, 1),
        tom: LocalDate = LocalDate.of(2023, 1, 31),
        behandlingId: BehandlingId,
    ) {
        every {
            vilkårperiodeRepository.findByBehandlingIdAndResultat(
                behandlingId,
                ResultatVilkårperiode.OPPFYLT,
            )
        } returns
            listOf(
                aktivitet(
                    behandlingId = behandlingId,
                    fom = fom,
                    tom = tom,
                ),
            )
    }

    private fun mockStønadsperioder(
        fom: LocalDate = LocalDate.of(2023, 1, 1),
        tom: LocalDate = LocalDate.of(2023, 1, 31),
        behandlingId: BehandlingId,
    ) {
        every { stønadsperiodeService.findAllByBehandlingId(behandlingId) } returns
            listOf(
                stønadsperiode(
                    behandlingId = behandlingId,
                    fom = fom,
                    tom = tom,
                ),
            )
    }
}
