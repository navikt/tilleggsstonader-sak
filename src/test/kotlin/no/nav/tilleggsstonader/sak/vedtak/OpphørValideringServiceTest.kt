package no.nav.tilleggsstonader.sak.vedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.tilSisteDagIMåneden
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.vedtakBeregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning.TilsynBarnBeregningService
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.felles.Vilkårstatus
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class OpphørValideringServiceTest {

    private val vilkårperiodeService = mockk<VilkårperiodeService>()
    private val vilkårService = mockk<VilkårService>()
    private val tilsynBarnBeregningService = mockk<TilsynBarnBeregningService>()

    val saksbehandling = saksbehandling(revurderFra = osloDateNow(), type = BehandlingType.REVURDERING)
    val opphørValideringService = OpphørValideringService(vilkårperiodeService, vilkårService)
    val vilkår = vilkår(behandlingId = saksbehandling.id, type = VilkårType.PASS_BARN, resultat = Vilkårsresultat.OPPFYLT, status = VilkårStatus.UENDRET)

    @BeforeEach
    fun setUp() {
        every { vilkårService.hentVilkår(saksbehandling.id) } returns listOf(vilkår)
        every { vilkårperiodeService.hentVilkårperioder(saksbehandling.id) } returns Vilkårperioder(emptyList(), emptyList())
        every { tilsynBarnBeregningService.beregn(saksbehandling) } returns BeregningsresultatTilsynBarn(emptyList())
    }

    @Test
    fun `Kaster feil ved utbetaling etter opphørdato`() {
        assertThatThrownBy {
            opphørValideringService.validerIngenUtbetalingEtterOpphør(vedtakBeregningsresultat, saksbehandling.revurderFra)
        }.hasMessage("Det er utbetalinger etter opphørsdato")
    }

    @Nested
    inner class GyldigData{

        @Test
        fun `Kaster ikke feil ved korrekt data`() {
            every { vilkårperiodeService.hentVilkårperioder(any()) } returns Vilkårperioder(listOf(VilkårperiodeTestUtil.målgruppe(status = Vilkårstatus.ENDRET)), listOf(VilkårperiodeTestUtil.aktivitet(status = Vilkårstatus.ENDRET)))
            every { tilsynBarnBeregningService.beregn(any()) } returns vedtakBeregningsresultat

            assertDoesNotThrow { opphørValideringService.validerPerioder(saksbehandling.copy(revurderFra = osloDateNow().plusMonths(2))) }
        }

    }

    @Nested
    inner class PerioderMedStatusNyOgResultatOppfylt{
        @Test
        fun `Kaster feil ved nye oppfylte vilkår`() {
            every { vilkårService.hentVilkår(saksbehandling.id) } returns listOf(vilkår.copy(status = VilkårStatus.NY))

            assertThatThrownBy {
                opphørValideringService.validerPerioder(saksbehandling)
            }.hasMessage("Det er vilkår eller vilkårperiode med vilkårstatus NY og resultat OPPFYLT.")
        }

        @Test
        fun `Kaster feil ved nye oppfylte målgrupper`() {
            every { vilkårperiodeService.hentVilkårperioder(saksbehandling.id) } returns Vilkårperioder(listOf(VilkårperiodeTestUtil.målgruppe()), emptyList())

            assertThatThrownBy {
                opphørValideringService.validerPerioder(saksbehandling)
            }.hasMessage("Det er vilkår eller vilkårperiode med vilkårstatus NY og resultat OPPFYLT.")
        }

        @Test
        fun `Kaster feil ved nye oppfylte aktivteter`() {
            every { vilkårperiodeService.hentVilkårperioder(saksbehandling.id) } returns Vilkårperioder(emptyList(), listOf(VilkårperiodeTestUtil.aktivitet()))

            assertThatThrownBy {
                opphørValideringService.validerPerioder(saksbehandling)
            }.hasMessage("Det er vilkår eller vilkårperiode med vilkårstatus NY og resultat OPPFYLT.")
        }
    }

    @Nested
    inner class TomEtterOpphørsdato(){
        @Test
        fun `Kaster feil ved målgruppe flyttet til etter opphørt dato`() {
            every { vilkårperiodeService.hentVilkårperioder(saksbehandling.id) } returns Vilkårperioder(listOf(VilkårperiodeTestUtil.målgruppe(status = Vilkårstatus.ENDRET)), emptyList())

            assertThatThrownBy {
                opphørValideringService.validerPerioder(saksbehandling)
            }.hasMessage("Til og med dato for endret målgruppe er etter opphørsdato")
        }

        @Test
        fun `Kaster feil ved aktivitet flyttet til etter opphørt dato`() {
            every { vilkårperiodeService.hentVilkårperioder(saksbehandling.id) } returns Vilkårperioder(emptyList(), listOf(VilkårperiodeTestUtil.aktivitet(status = Vilkårstatus.ENDRET)))

            assertThatThrownBy {
                opphørValideringService.validerPerioder(saksbehandling)
            }.hasMessage("Til og med dato for endret aktivitet er etter opphørsdato")
        }

        @Test
        fun `Kaster feil ved vilkår flyttet til etter opphørt dato`() {
            every { vilkårService.hentVilkår(saksbehandling.id) } returns listOf(vilkår.copy(status = VilkårStatus.ENDRET, tom = osloDateNow().plusMonths(1).tilSisteDagIMåneden()))

            assertThatThrownBy {
                opphørValideringService.validerPerioder(saksbehandling)
            }.hasMessage("Til og med dato for endret vilkår er etter opphørsdato")
        }
    }
}
