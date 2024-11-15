package no.nav.tilleggsstonader.sak.vedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.libs.utils.osloDateNow
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingType
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
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
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class OpphørValidatorServiceTest {

    private val vilkårperiodeService = mockk<VilkårperiodeService>()
    private val vilkårService = mockk<VilkårService>()
    private val tilsynBarnBeregningService = mockk<TilsynBarnBeregningService>()

    val saksbehandling = saksbehandling(revurderFra = osloDateNow(), type = BehandlingType.REVURDERING)
    val opphørValidatorService = OpphørValidatorService(vilkårperiodeService, vilkårService, tilsynBarnBeregningService)

    @Test
    fun kasterIkkeFeilVedKorrektData() {
        every { vilkårService.hentVilkår(any()) } returns listOf(vilkår(behandlingId = saksbehandling.id, type = VilkårType.PASS_BARN, resultat = Vilkårsresultat.OPPFYLT, status = VilkårStatus.UENDRET))
        every { vilkårperiodeService.hentVilkårperioder(any()) } returns Vilkårperioder(listOf(VilkårperiodeTestUtil.målgruppe(status = Vilkårstatus.ENDRET)), listOf(VilkårperiodeTestUtil.aktivitet(status = Vilkårstatus.ENDRET)))
        every { tilsynBarnBeregningService.beregn(any()) } returns vedtakBeregningsresultat

        assertDoesNotThrow { opphørValidatorService.validerOpphør(saksbehandling.copy(revurderFra = osloDateNow().plusMonths(2))) }
    }

    @Test
    fun kasterFeilVedIngenNyeOppfylteVilkår() {
        every { vilkårService.hentVilkår(saksbehandling.id) } returns listOf(vilkår(behandlingId = saksbehandling.id, type = VilkårType.PASS_BARN, resultat = Vilkårsresultat.OPPFYLT, status = VilkårStatus.NY))
        every { vilkårperiodeService.hentVilkårperioder(saksbehandling.id) } returns Vilkårperioder(emptyList(), emptyList())
        every { tilsynBarnBeregningService.beregn(saksbehandling) } returns BeregningsresultatTilsynBarn(emptyList())

        val feil: ApiFeil = assertThrows {
            opphørValidatorService.validerOpphør(saksbehandling)
        }
        assertThat(feil.message).isEqualTo("Det er vilkår eller vilkårperiode med vilkårstatus NY og resultat OPPFYLT.")
    }

    @Test
    fun kasterFeilVedIngenNyeOppfylteMålgrupper() {
        every { vilkårService.hentVilkår(saksbehandling.id) } returns listOf(vilkår(behandlingId = saksbehandling.id, type = VilkårType.PASS_BARN, resultat = Vilkårsresultat.OPPFYLT, status = VilkårStatus.UENDRET))
        every { vilkårperiodeService.hentVilkårperioder(saksbehandling.id) } returns Vilkårperioder(listOf(VilkårperiodeTestUtil.målgruppe()), emptyList())
        every { tilsynBarnBeregningService.beregn(saksbehandling) } returns BeregningsresultatTilsynBarn(emptyList())

        val feil: ApiFeil = assertThrows {
            opphørValidatorService.validerOpphør(saksbehandling)
        }
        assertThat(feil.message).isEqualTo("Det er vilkår eller vilkårperiode med vilkårstatus NY og resultat OPPFYLT.")
    }

    @Test
    fun kasterFeilVedIngenNyeOppfylteAktivteter() {
        every { vilkårService.hentVilkår(saksbehandling.id) } returns listOf(vilkår(behandlingId = saksbehandling.id, type = VilkårType.PASS_BARN, resultat = Vilkårsresultat.OPPFYLT, status = VilkårStatus.UENDRET))
        every { vilkårperiodeService.hentVilkårperioder(saksbehandling.id) } returns Vilkårperioder(emptyList(), listOf(VilkårperiodeTestUtil.aktivitet()))
        every { tilsynBarnBeregningService.beregn(saksbehandling) } returns BeregningsresultatTilsynBarn(emptyList())

        val feil: ApiFeil = assertThrows {
            opphørValidatorService.validerOpphør(saksbehandling)
        }
        assertThat(feil.message).isEqualTo("Det er vilkår eller vilkårperiode med vilkårstatus NY og resultat OPPFYLT.")
    }

    @Test
    fun kasterFeilVedUtbetalingEtterOpphør() {
        every { vilkårService.hentVilkår(saksbehandling.id) } returns listOf(vilkår(behandlingId = saksbehandling.id, type = VilkårType.PASS_BARN, resultat = Vilkårsresultat.OPPFYLT, status = VilkårStatus.UENDRET))
        every { vilkårperiodeService.hentVilkårperioder(saksbehandling.id) } returns Vilkårperioder(emptyList(), emptyList())
        every { tilsynBarnBeregningService.beregn(saksbehandling) } returns vedtakBeregningsresultat

        val feil: ApiFeil = assertThrows {
            opphørValidatorService.validerOpphør(saksbehandling)
        }
        assertThat(feil.message).isEqualTo("Det er utbetalinger etter opphørsdato")
    }

    @Test
    fun kasterFeilVedMålgruppePeriodeFlyttetTilEtterOpphørtDato() {
        every { vilkårService.hentVilkår(saksbehandling.id) } returns listOf(vilkår(behandlingId = saksbehandling.id, type = VilkårType.PASS_BARN, resultat = Vilkårsresultat.OPPFYLT, status = VilkårStatus.UENDRET))
        every { vilkårperiodeService.hentVilkårperioder(saksbehandling.id) } returns Vilkårperioder(listOf(VilkårperiodeTestUtil.målgruppe(status = Vilkårstatus.ENDRET)), emptyList())
        every { tilsynBarnBeregningService.beregn(saksbehandling) } returns BeregningsresultatTilsynBarn(emptyList())

        val feil: ApiFeil = assertThrows {
            opphørValidatorService.validerOpphør(saksbehandling)
        }
        assertThat(feil.message).isEqualTo("TOM er etter opphørsdato for endret målgruppe")
    }

    @Test
    fun kasterFeilVedAktivitetPeriodeFlyttetTilEtterOpphørtDato() {
        every { vilkårService.hentVilkår(saksbehandling.id) } returns listOf(vilkår(behandlingId = saksbehandling.id, type = VilkårType.PASS_BARN, resultat = Vilkårsresultat.OPPFYLT, status = VilkårStatus.UENDRET))
        every { vilkårperiodeService.hentVilkårperioder(saksbehandling.id) } returns Vilkårperioder(emptyList(), listOf(VilkårperiodeTestUtil.aktivitet(status = Vilkårstatus.ENDRET)))
        every { tilsynBarnBeregningService.beregn(saksbehandling) } returns BeregningsresultatTilsynBarn(emptyList())

        val feil: ApiFeil = assertThrows {
            opphørValidatorService.validerOpphør(saksbehandling)
        }
        assertThat(feil.message).isEqualTo("TOM er etter opphørsdato for endret aktivitet")
    }

    @Test
    fun kasterFeilVedVilkårPeriodeFlyttetTilEtterOpphørtDato() {
        every { vilkårService.hentVilkår(saksbehandling.id) } returns listOf(vilkår(behandlingId = saksbehandling.id, type = VilkårType.PASS_BARN, resultat = Vilkårsresultat.OPPFYLT, status = VilkårStatus.ENDRET, tom = osloDateNow().plusMonths(1).tilSisteDagIMåneden()))
        every { vilkårperiodeService.hentVilkårperioder(saksbehandling.id) } returns Vilkårperioder(emptyList(), emptyList())
        every { tilsynBarnBeregningService.beregn(saksbehandling) } returns BeregningsresultatTilsynBarn(emptyList())

        val feil: ApiFeil = assertThrows {
            opphørValidatorService.validerOpphør(saksbehandling)
        }
        assertThat(feil.message).isEqualTo("TOM er etter opphørsdato for endret vilkår")
    }
}
