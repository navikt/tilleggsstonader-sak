package no.nav.tilleggsstonader.sak.beregnfra

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BeregnFraUtlederServiceTest {
    private val behandlingService = mockk<BehandlingService>()
    private val vilkårService = mockk<VilkårService>()
    private val vilkårperiodeService = mockk<VilkårperiodeService>()
    private val utledBeregnFraDatoService = UtledBeregnFraDatoService(behandlingService, vilkårService, vilkårperiodeService)

    var behandling: Behandling = behandling()
    var sisteIverksatteBehandling: Behandling = behandling()

    lateinit var vilkår: List<Vilkår>
    lateinit var vilkårSisteIverksatteBehandling: List<Vilkår>
    lateinit var vilkårperioder: Vilkårperioder
    lateinit var vilkårperioderSisteIverksattBehandling: Vilkårperioder

    @BeforeEach
    fun setUp() {
        every { behandlingService.hentBehandling(behandling.id) } answers { behandling }
        every { behandlingService.hentBehandlinger(behandling.fagsakId) } answers { listOf(sisteIverksatteBehandling, behandling) }

        every { vilkårService.hentVilkår(behandling.id) } answers { vilkår }
        every { vilkårperiodeService.hentVilkårperioder(behandling.id) } answers { vilkårperioder }

        every { vilkårService.hentVilkår(sisteIverksatteBehandling.id) } answers { vilkårSisteIverksatteBehandling }
        every { vilkårperiodeService.hentVilkårperioder(sisteIverksatteBehandling.id) } answers { vilkårperioderSisteIverksattBehandling }
    }

    @Test
    fun `utled beregnFraDato, lagt på nye perioder, beregnFraDato blir fom-dato på ny periode`() {
        vilkårSisteIverksatteBehandling =
            listOf(
                vilkår(
                    sisteIverksatteBehandling.id,
                    VilkårType.UTGIFTER_OVERNATTING,
                    fom = LocalDate.now().minusMonths(1),
                    tom = LocalDate.now(),
                ),
            )
        vilkårperioderSisteIverksattBehandling =
            Vilkårperioder(
                målgrupper =
                    listOf(
                        VilkårperiodeTestUtil.målgruppe(fom = LocalDate.now().minusMonths(1), tom = LocalDate.now()),
                    ),
                aktiviteter =
                    listOf(
                        VilkårperiodeTestUtil.aktivitet(fom = LocalDate.now().minusMonths(1), tom = LocalDate.now()),
                    ),
            )

        val nyttVilkår =
            vilkår(
                sisteIverksatteBehandling.id,
                VilkårType.UTGIFTER_OVERNATTING,
                fom = LocalDate.now().plusDays(1),
                tom = LocalDate.now().plusMonths(1),
            )

        vilkår = vilkårSisteIverksatteBehandling + nyttVilkår
        vilkårperioder =
            Vilkårperioder(
                målgrupper =
                    vilkårperioderSisteIverksattBehandling.målgrupper +
                        VilkårperiodeTestUtil.målgruppe(fom = LocalDate.now().plusDays(1), tom = LocalDate.now().plusMonths(1)),
                aktiviteter =
                    vilkårperioderSisteIverksattBehandling.aktiviteter +
                        VilkårperiodeTestUtil.aktivitet(fom = LocalDate.now().plusDays(1), tom = LocalDate.now().plusMonths(1)),
            )

        val result = utledBeregnFraDatoService.utledBeregnFraDato(behandling.id)

        assertThat(result).isEqualTo(nyttVilkår.fom)
    }

    @Test
    fun `utled beregnFraDato, målgruppe endret, beregnFraDato blir startdato på endret målgruppe`() {
        vilkårSisteIverksatteBehandling =
            listOf(
                vilkår(
                    sisteIverksatteBehandling.id,
                    VilkårType.UTGIFTER_OVERNATTING,
                    fom = LocalDate.now().minusMonths(1),
                    tom = LocalDate.now(),
                ),
            )
        vilkårperioderSisteIverksattBehandling =
            Vilkårperioder(
                målgrupper =
                    listOf(
                        VilkårperiodeTestUtil.målgruppe(fom = LocalDate.now().minusMonths(1), tom = LocalDate.now()),
                    ),
                aktiviteter =
                    listOf(
                        VilkårperiodeTestUtil.aktivitet(fom = LocalDate.now().minusMonths(1), tom = LocalDate.now()),
                    ),
            )

        vilkår = vilkårSisteIverksatteBehandling
        vilkårperioder =
            Vilkårperioder(
                målgrupper =
                    listOf(
                        vilkårperioderSisteIverksattBehandling.målgrupper
                            .single()
                            .copy(resultat = ResultatVilkårperiode.IKKE_OPPFYLT),
                    ),
                aktiviteter = vilkårperioderSisteIverksattBehandling.aktiviteter,
            )

        val result = utledBeregnFraDatoService.utledBeregnFraDato(behandling.id)

        assertThat(result).isEqualTo(vilkårperioder.målgrupper.single().fom)
    }

    @Test
    fun `utled beregnFraDato, aktivitet endret, beregnFraDato blir startdato på endret målgruppe`() {
        vilkårSisteIverksatteBehandling =
            listOf(
                vilkår(
                    sisteIverksatteBehandling.id,
                    VilkårType.UTGIFTER_OVERNATTING,
                    fom = LocalDate.now().minusMonths(1),
                    tom = LocalDate.now(),
                ),
            )
        vilkårperioderSisteIverksattBehandling =
            Vilkårperioder(
                målgrupper =
                    listOf(
                        VilkårperiodeTestUtil.målgruppe(fom = LocalDate.now().minusMonths(1), tom = LocalDate.now()),
                    ),
                aktiviteter =
                    listOf(
                        VilkårperiodeTestUtil.aktivitet(fom = LocalDate.now().minusMonths(1), tom = LocalDate.now()),
                    ),
            )

        vilkår = vilkårSisteIverksatteBehandling
        vilkårperioder =
            Vilkårperioder(
                målgrupper = vilkårperioderSisteIverksattBehandling.målgrupper,
                aktiviteter =
                    listOf(
                        vilkårperioderSisteIverksattBehandling.aktiviteter
                            .single()
                            .copy(resultat = ResultatVilkårperiode.IKKE_OPPFYLT),
                    ),
            )

        val result = utledBeregnFraDatoService.utledBeregnFraDato(behandling.id)

        assertThat(result).isEqualTo(vilkårperioder.aktiviteter.single().fom)
    }

    @Test
    fun `utled beregnFraDato, vilkår-fom endret, beregnFraDato blir startdato på originalt vilkår`() {
        vilkårSisteIverksatteBehandling =
            listOf(
                vilkår(
                    sisteIverksatteBehandling.id,
                    VilkårType.UTGIFTER_OVERNATTING,
                    fom = LocalDate.now().minusMonths(1),
                    tom = LocalDate.now(),
                ),
            )
        vilkårperioderSisteIverksattBehandling =
            Vilkårperioder(
                målgrupper =
                    listOf(
                        VilkårperiodeTestUtil.målgruppe(fom = LocalDate.now().minusMonths(1), tom = LocalDate.now()),
                    ),
                aktiviteter =
                    listOf(
                        VilkårperiodeTestUtil.aktivitet(fom = LocalDate.now().minusMonths(1), tom = LocalDate.now()),
                    ),
            )

        vilkår = listOf(vilkårSisteIverksatteBehandling.single().copy(fom = LocalDate.now().minusWeeks(1)))
        vilkårperioder = vilkårperioderSisteIverksattBehandling

        val result = utledBeregnFraDatoService.utledBeregnFraDato(behandling.id)

        assertThat(result).isEqualTo(vilkårSisteIverksatteBehandling.single().fom)
    }
}
