package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.utbetaling.simulering.SimuleringService
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseService
import no.nav.tilleggsstonader.sak.util.aktivitet
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.barn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgelseDto
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.domain.StønadsperiodeRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

class TilsynBarnBeregnYtelseStegTest {
    private val repository = mockk<TilsynBarnVedtakRepository>(relaxed = true)
    private val barnService = mockk<BarnService>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>(relaxed = true)
    private val simuleringService = mockk<SimuleringService>(relaxed = true)
    private val stønadsperiodeService = mockk<StønadsperiodeRepository>(relaxed = true)
    private val vilkårperiodeRepository = mockk<VilkårperiodeRepository>(relaxed = true)

    val steg = TilsynBarnBeregnYtelseSteg(
        tilsynBarnBeregningService = TilsynBarnBeregningService(stønadsperiodeService, vilkårperiodeRepository),
        vedtakRepository = repository,
        barnService = barnService,
        tilkjentytelseService = tilkjentYtelseService,
        simuleringService = simuleringService,
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
        every { stønadsperiodeService.findAllByBehandlingId(saksbehandling.id) } returns listOf(
            stønadsperiode(
                behandlingId = saksbehandling.id,
                fom = fom,
                tom = tom,
            ),
        )
        every { vilkårperiodeRepository.findByBehandlingIdAndResultat(saksbehandling.id, ResultatVilkårperiode.OPPFYLT) } returns listOf(
            aktivitet(
                behandlingId = saksbehandling.id,
                fom = fom,
                tom = tom,
            ),
        )
    }

    @Test
    fun `skal slette data som finnes fra før, før man lagrer ny data`() {
        val vedtak = innvilgelseDto(
            utgifter = mapOf(barn(barn.id, Utgift(måned, måned, 100))),
        )
        steg.utførSteg(saksbehandling, vedtak)

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
}
