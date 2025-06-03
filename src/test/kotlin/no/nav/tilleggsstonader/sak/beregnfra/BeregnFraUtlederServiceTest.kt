package no.nav.tilleggsstonader.sak.beregnfra

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Behandling
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.VedtaksperiodeService
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
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
    private val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    private val utledBeregnFraDatoService =
        UtledBeregnFraDatoService(
            behandlingService,
            vilkårService,
            vilkårperiodeService,
            vedtaksperiodeService,
        )

    var behandling: Behandling = behandling()
    var sisteIverksatteBehandling: Behandling = behandling()

    lateinit var vilkår: List<Vilkår>
    lateinit var vilkårSisteIverksatteBehandling: List<Vilkår>
    lateinit var vilkårperioder: Vilkårperioder
    lateinit var vilkårperioderSisteIverksattBehandling: Vilkårperioder
    lateinit var vedtaksperioder: List<Vedtaksperiode>
    lateinit var vedtaksperioderSisteIverksatteBehandling: List<Vedtaksperiode>

    @BeforeEach
    fun setUp() {
        every { behandlingService.hentBehandling(behandling.id) } answers { behandling }
        every { behandlingService.hentBehandlinger(behandling.fagsakId) } answers { listOf(sisteIverksatteBehandling, behandling) }

        every { vilkårService.hentVilkår(behandling.id) } answers { vilkår }
        every { vilkårperiodeService.hentVilkårperioder(behandling.id) } answers { vilkårperioder }
        every { vedtaksperiodeService.finnVedtaksperioderForBehandling(behandling.id, null) } answers { vedtaksperioder }

        every { vilkårService.hentVilkår(sisteIverksatteBehandling.id) } answers { vilkårSisteIverksatteBehandling }
        every { vilkårperiodeService.hentVilkårperioder(sisteIverksatteBehandling.id) } answers { vilkårperioderSisteIverksattBehandling }
        every { vedtaksperiodeService.finnVedtaksperioderForBehandling(sisteIverksatteBehandling.id, null) } answers
            { vedtaksperioderSisteIverksatteBehandling }

        val fom = LocalDate.now().minusMonths(1)
        val tom = LocalDate.now()
        vilkårSisteIverksatteBehandling =
            listOf(
                vilkår(
                    behandlingId = sisteIverksatteBehandling.id,
                    type = VilkårType.UTGIFTER_OVERNATTING,
                    fom = fom,
                    tom = tom,
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
        vedtaksperioderSisteIverksatteBehandling =
            listOf(
                vedtaksperiode(
                    fom = LocalDate.now().minusMonths(1),
                    tom = LocalDate.now(),
                ),
            )
    }

    @Test
    fun `utled beregnFraDato, lagt på nye perioder, beregnFraDato blir fom-dato på ny periode`() {
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
        vedtaksperioder = vedtaksperioderSisteIverksatteBehandling

        val result = utledBeregnFraDatoService.utledBeregnFraDato(behandling.id)

        assertThat(result).isEqualTo(nyttVilkår.fom)
    }

    @Test
    fun `utled beregnFraDato, målgruppe endret, beregnFraDato blir startdato på endret målgruppe`() {
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
        vedtaksperioder = vedtaksperioderSisteIverksatteBehandling

        val result = utledBeregnFraDatoService.utledBeregnFraDato(behandling.id)

        assertThat(result).isEqualTo(vilkårperioder.målgrupper.single().fom)
    }

    @Test
    fun `utled beregnFraDato, aktivitet endret, beregnFraDato blir startdato på endret målgruppe`() {
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
        vedtaksperioder = vedtaksperioderSisteIverksatteBehandling

        val result = utledBeregnFraDatoService.utledBeregnFraDato(behandling.id)

        assertThat(result).isEqualTo(vilkårperioder.aktiviteter.single().fom)
    }

    @Test
    fun `utled beregnFraDato, vilkår-fom endret, beregnFraDato blir startdato på originalt vilkår`() {
        vilkår = listOf(vilkårSisteIverksatteBehandling.single().copy(fom = LocalDate.now().minusWeeks(1)))
        vilkårperioder = vilkårperioderSisteIverksattBehandling
        vedtaksperioder = vedtaksperioderSisteIverksatteBehandling

        val result = utledBeregnFraDatoService.utledBeregnFraDato(behandling.id)

        assertThat(result).isEqualTo(vilkårSisteIverksatteBehandling.single().fom)
    }

    @Test
    fun `utled beregnFraDato, vedtaksperiode splittet i to, beregnFraDato blir første dato etter korteste vedtaksperiode`() {
        vilkår = vilkårSisteIverksatteBehandling
        vilkårperioder = vilkårperioderSisteIverksattBehandling
        vedtaksperioder =
            listOf(
                vedtaksperiode(fom = LocalDate.now().minusMonths(1), tom = LocalDate.now().minusWeeks(1)),
                vedtaksperiode(fom = LocalDate.now().minusWeeks(1).plusDays(1), tom = LocalDate.now()),
            )

        val result = utledBeregnFraDatoService.utledBeregnFraDato(behandling.id)

        assertThat(result).isEqualTo(vedtaksperioder.last().fom)
    }
}
