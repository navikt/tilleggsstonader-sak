package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.infrastruktur.unleash.mockUnleashService
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.barn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgelseDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnBeregningService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.UtgiftBeregning
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.Utgift
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

class TilsynBarnBeregnYtelseStegTest {
    private val repository = mockk<TilsynBarnVedtakRepository>(relaxed = true)
    private val barnService = mockk<BarnService>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>(relaxed = true)
    private val simuleringService = mockk<SimuleringService>(relaxed = true)
    private val stønadsperiodeService = mockk<StønadsperiodeRepository>(relaxed = true)
    private val vilkårperiodeRepository = mockk<VilkårperiodeRepository>(relaxed = true)
    private val vilkårService = mockk<VilkårService>(relaxed = true)
    private val tilsynBarnUtgiftService = mockk<TilsynBarnUtgiftService>(relaxed = true)

    val steg = TilsynBarnBeregnYtelseSteg(
        tilsynBarnBeregningService = TilsynBarnBeregningService(stønadsperiodeService, vilkårperiodeRepository),
        vedtakRepository = repository,
        tilkjentytelseService = tilkjentYtelseService,
        simuleringService = simuleringService,
        vilkårService = vilkårService,
        unleashService = mockUnleashService(),
        tilsynBarnUtgiftService = tilsynBarnUtgiftService,
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
        mockVilkår(saksbehandling.id)
        every { tilsynBarnUtgiftService.hentUtgifterTilBeregning(any(), any()) } returns
            mapOf(barn.id to listOf(UtgiftBeregning(YearMonth.now(), YearMonth.now(), 1)))
    }

    @Test
    fun `skal slette data som finnes fra før, før man lagrer ny data`() {
        val vedtak = innvilgelseDto(
            utgifter = mapOf(barn(barn.id, Utgift(måned, måned, 100))),
        )
        steg.utførOgReturnerNesteSteg(saksbehandling, vedtak)

        verifyOrder {
            repository.deleteById(saksbehandling.id)
            tilkjentYtelseService.slettTilkjentYtelseForBehandling(saksbehandling)
            simuleringService.slettSimuleringForBehandling(saksbehandling)

            repository.insert(any())
            tilkjentYtelseService.opprettTilkjentYtelse(any())
        }
        verify(exactly = 0) {
            simuleringService.hentOgLagreSimuleringsresultat(any())
        }
    }

    @Test
    fun `skal returnere neste steg SIMULERING ved førstegangsbehandling`() {
        val vedtak = innvilgelseDto(
            utgifter = mapOf(barn(barn.id, Utgift(måned, måned, 100))),
        )

        val nesteSteg = steg.utførOgReturnerNesteSteg(saksbehandling, vedtak)

        assertThat(saksbehandling.type).isEqualTo(BehandlingType.FØRSTEGANGSBEHANDLING)
        assertThat(nesteSteg).isEqualTo(StegType.SIMULERING)
    }

    @Test
    fun `skal returnere neste steg SIMULERING ved revurdering`() {
        val revurdering = saksbehandling(type = BehandlingType.REVURDERING)

        val vedtak = innvilgelseDto(
            utgifter = mapOf(barn(barn.id, Utgift(måned, måned, 100))),
        )

        mockVilkår(revurdering.id)
        mockVilkårperioder(behandlingId = revurdering.id)
        mockStønadsperioder(behandlingId = revurdering.id)

        val nesteSteg = steg.utførOgReturnerNesteSteg(revurdering, vedtak)
        assertThat(revurdering.type).isEqualTo(BehandlingType.REVURDERING)
        assertThat(nesteSteg).isEqualTo(StegType.SIMULERING)
    }

    private fun mockVilkår(behandlingId: UUID) {
        every { vilkårService.hentOppfyltePassBarnVilkår(behandlingId) } returns listOf(
            vilkår(
                behandlingId = behandlingId,
                barnId = barn.id,
                resultat = Vilkårsresultat.OPPFYLT,
                type = VilkårType.PASS_BARN,
            ),
        )
    }

    private fun mockVilkårperioder(
        fom: LocalDate = LocalDate.of(2023, 1, 1),
        tom: LocalDate = LocalDate.of(2023, 1, 31),
        behandlingId: UUID,
    ) {
        every {
            vilkårperiodeRepository.findByBehandlingIdAndResultat(
                behandlingId,
                ResultatVilkårperiode.OPPFYLT,
            )
        } returns listOf(
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
        behandlingId: UUID,
    ) {
        every { stønadsperiodeService.findAllByBehandlingId(behandlingId) } returns listOf(
            stønadsperiode(
                behandlingId = behandlingId,
                fom = fom,
                tom = tom,
            ),
        )
    }
}
