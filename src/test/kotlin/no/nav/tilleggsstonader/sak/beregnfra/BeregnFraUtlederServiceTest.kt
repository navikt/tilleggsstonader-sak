package no.nav.tilleggsstonader.sak.beregnfra

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.barn.BarnService
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
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.ResultatVilkårperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.faktavurderinger.AktivitetFaktaOgVurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

private fun vilkårperioder(block: VilkårperioderBuilder.() -> Unit): Vilkårperioder = VilkårperioderBuilder().apply(block).build()

private class VilkårperioderBuilder {
    val målgrupper = mutableListOf<VilkårperiodeMålgruppe>()
    val aktiviteter = mutableListOf<VilkårperiodeAktivitet>()

    fun målgruppe(
        fom: LocalDate,
        tom: LocalDate,
        resultat: ResultatVilkårperiode = ResultatVilkårperiode.OPPFYLT,
    ) {
        målgrupper += VilkårperiodeTestUtil.målgruppe(fom = fom, tom = tom, resultat = resultat)
    }

    fun aktivitet(
        fom: LocalDate,
        tom: LocalDate,
        resultat: ResultatVilkårperiode = ResultatVilkårperiode.OPPFYLT,
        faktaOgVurdering: AktivitetFaktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(),
    ) {
        aktiviteter +=
            VilkårperiodeTestUtil.aktivitet(
                fom = fom,
                tom = tom,
                resultat = resultat,
                faktaOgVurdering = faktaOgVurdering,
            )
    }

    fun build() = Vilkårperioder(målgrupper = målgrupper, aktiviteter = aktiviteter)
}

private fun vedtaksperiode(
    fom: LocalDate,
    tom: LocalDate,
    block: (Vedtaksperiode.() -> Unit)? = null,
): Vedtaksperiode {
    val v =
        vedtaksperiode(fom = fom, tom = tom)
    block?.invoke(v)
    return v
}

class BeregnFraUtlederServiceTest {
    private val behandlingService = mockk<BehandlingService>()
    private val vilkårService = mockk<VilkårService>()
    private val vilkårperiodeService = mockk<VilkårperiodeService>()
    private val vedtaksperiodeService = mockk<VedtaksperiodeService>()
    private val barnService = mockk<BarnService>()
    private val utledBeregnFraDatoService =
        UtledBeregnFraDatoService(
            behandlingService,
            vilkårService,
            vilkårperiodeService,
            vedtaksperiodeService,
            barnService,
        )

    var sisteIverksatteBehandling: Behandling = behandling()
    var behandling: Behandling = behandling(forrigeIverksatteBehandlingId = sisteIverksatteBehandling.id)

    lateinit var vilkår: List<Vilkår>
    lateinit var vilkårSisteIverksatteBehandling: List<Vilkår>
    lateinit var vilkårperioder: Vilkårperioder
    lateinit var vilkårperioderSisteIverksattBehandling: Vilkårperioder
    lateinit var vedtaksperioder: List<Vedtaksperiode>
    lateinit var vedtaksperioderSisteIverksatteBehandling: List<Vedtaksperiode>

    var originalFom = LocalDate.now().minusMonths(1)
    var originalTom = LocalDate.now()

    @BeforeEach
    fun setUp() {
        every { behandlingService.hentBehandling(behandling.id) } answers { behandling }
        every { behandlingService.hentBehandling(behandling.forrigeIverksatteBehandlingId!!) } answers { sisteIverksatteBehandling }

        every { vilkårService.hentVilkår(behandling.id) } answers { vilkår }
        every { vilkårperiodeService.hentVilkårperioder(behandling.id) } answers { vilkårperioder }
        every { vedtaksperiodeService.finnVedtaksperioderForBehandling(behandling.id, null) } answers { vedtaksperioder }

        every { vilkårService.hentVilkår(sisteIverksatteBehandling.id) } answers { vilkårSisteIverksatteBehandling }
        every { vilkårperiodeService.hentVilkårperioder(sisteIverksatteBehandling.id) } answers { vilkårperioderSisteIverksattBehandling }
        every { vedtaksperiodeService.finnVedtaksperioderForBehandling(sisteIverksatteBehandling.id, null) } answers
            { vedtaksperioderSisteIverksatteBehandling }

        every { barnService.finnBarnPåBehandling(any()) } returns emptyList()

        vilkårSisteIverksatteBehandling =
            listOf(
                vilkår(
                    behandlingId = sisteIverksatteBehandling.id,
                    type = VilkårType.UTGIFTER_OVERNATTING,
                    fom = originalFom,
                    tom = originalTom,
                ),
            )
        vilkårperioderSisteIverksattBehandling =
            vilkårperioder {
                målgruppe(originalFom, originalTom)
                aktivitet(originalFom, originalTom)
            }
        vedtaksperioderSisteIverksatteBehandling =
            listOf(
                vedtaksperiode(originalFom, originalTom),
            )
    }

    @Test
    fun `utled beregnFraDato, lagt på nye perioder, beregnFraDato blir fom-dato på ny periode`() {
        val nyttFom = originalTom.plusDays(1)
        val nyttTom = originalTom.plusMonths(1)
        val nyttVilkår =
            vilkår(
                sisteIverksatteBehandling.id,
                VilkårType.UTGIFTER_OVERNATTING,
                fom = nyttFom,
                tom = nyttTom,
            )

        vilkår = vilkårSisteIverksatteBehandling + nyttVilkår
        vilkårperioder =
            vilkårperioder {
                målgruppe(LocalDate.now().minusMonths(1), LocalDate.now())
                aktivitet(LocalDate.now().minusMonths(1), LocalDate.now())
                målgruppe(nyttFom, nyttTom)
                aktivitet(nyttFom, nyttTom)
            }
        vedtaksperioder = vedtaksperioderSisteIverksatteBehandling

        val result = utledBeregnFraDatoService.utledBeregnFraDato(behandling.id)

        assertThat(result).isEqualTo(nyttVilkår.fom)
    }

    @Test
    fun `utled beregnFraDato, målgruppe endret, beregnFraDato blir startdato på endret målgruppe`() {
        vilkår = vilkårSisteIverksatteBehandling
        vilkårperioder =
            vilkårperioder {
                målgruppe(originalFom, originalTom, resultat = ResultatVilkårperiode.IKKE_OPPFYLT)
                aktivitet(originalFom, originalTom)
            }
        vedtaksperioder = vedtaksperioderSisteIverksatteBehandling

        val result = utledBeregnFraDatoService.utledBeregnFraDato(behandling.id)

        assertThat(result).isEqualTo(vilkårperioder.målgrupper.single().fom)
    }

    @Test
    fun `utled beregnFraDato, aktivitet endret, beregnFraDato blir startdato på endret målgruppe`() {
        vilkår = vilkårSisteIverksatteBehandling
        vilkårperioder =
            vilkårperioder {
                målgruppe(originalFom, originalTom)
                aktivitet(originalFom, originalTom, resultat = ResultatVilkårperiode.IKKE_OPPFYLT)
            }
        vedtaksperioder = vedtaksperioderSisteIverksatteBehandling

        val result = utledBeregnFraDatoService.utledBeregnFraDato(behandling.id)

        assertThat(result).isEqualTo(vilkårperioder.aktiviteter.single().fom)
    }

    @Test
    fun `utled beregnFraDato, vilkår-fom endret, beregnFraDato blir startdato på originalt vilkår`() {
        vilkår =
            listOf(
                vilkår(
                    behandlingId = sisteIverksatteBehandling.id,
                    type = VilkårType.UTGIFTER_OVERNATTING,
                    fom = originalFom.plusWeeks(1),
                    tom = originalTom,
                ),
            )
        vilkårperioder =
            vilkårperioder {
                målgruppe(originalFom, originalTom)
                aktivitet(originalFom, originalTom)
            }
        vedtaksperioder = vedtaksperioderSisteIverksatteBehandling

        val result = utledBeregnFraDatoService.utledBeregnFraDato(behandling.id)

        assertThat(result).isEqualTo(vilkårSisteIverksatteBehandling.single().fom)
    }

    @Test
    fun `utled beregnFraDato, vedtaksperiode splittet i to, beregnFraDato blir første dato etter korteste vedtaksperiode`() {
        val endringsdato = originalFom.plusWeeks(1)
        vilkår = vilkårSisteIverksatteBehandling
        vilkårperioder = vilkårperioderSisteIverksattBehandling
        vedtaksperioder =
            listOf(
                vedtaksperiode(endringsdato, originalTom),
                vedtaksperiode(originalFom, endringsdato.minusDays(1)),
            )

        val result = utledBeregnFraDatoService.utledBeregnFraDato(behandling.id)

        assertThat(result).isEqualTo(endringsdato)
    }

    @Test
    fun `utled beregnFraDato, tiltak og vedtaksperiode blir forelenget, beregnFraDato blir første dato etter gammel tom`() {
        val nyttTom = LocalDate.now().plusMonths(1)

        vilkår =
            listOf(
                vilkår(
                    behandlingId = sisteIverksatteBehandling.id,
                    type = VilkårType.UTGIFTER_OVERNATTING,
                    fom = originalFom,
                    tom = nyttTom,
                ),
            )
        vilkårperioder =
            vilkårperioder {
                målgruppe(originalFom, nyttTom)
                aktivitet(originalFom, nyttTom)
            }
        vedtaksperioder = vedtaksperioderSisteIverksatteBehandling

        val result = utledBeregnFraDatoService.utledBeregnFraDato(behandling.id)

        assertThat(result).isEqualTo(originalTom.plusDays(1))
    }

    @Test
    fun `utled beregnFraDato, gammelt tiltak endret og lagt til nytt tiltak, beregnFraDato blir startdato på gammelt tiltakt`() {
        val nyttTom = LocalDate.now().plusMonths(1)

        vilkår =
            listOf(
                vilkår(
                    behandlingId = sisteIverksatteBehandling.id,
                    type = VilkårType.UTGIFTER_OVERNATTING,
                    fom = originalFom,
                    tom = nyttTom,
                ),
            )
        vilkårperioder =
            vilkårperioder {
                målgruppe(originalFom, nyttTom)
                aktivitet(originalFom, originalTom, faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(aktivitetsdager = 2))
                aktivitet(originalTom.plusDays(1), nyttTom)
            }
        vedtaksperioder = vedtaksperioderSisteIverksatteBehandling.map { it.copy(tom = nyttTom) }

        val result = utledBeregnFraDatoService.utledBeregnFraDato(behandling.id)

        assertThat(result).isEqualTo(originalFom)
    }
}
