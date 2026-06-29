package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseTilSamlingOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ReiseTilSamlingBeregningsTest {
    private val vilkårService = mockk<VilkårService>()
    private val vedtakService = mockk<VedtakService>()
    private val beregningService = ReiseTilSamlingBeregningService(vilkårService, vedtakService)
    private val behandling = saksbehandling()

    private val vedtaksperioder = listOf(vedtaksperiode(fom = 1 januar 2025, tom = 31 januar 2025))

    @Test
    fun `beregner offentlig transport riktig for ett vilkår`() {
        val utgifter = 500
        every { vilkårService.hentOppfylteReiseTilSamlingVilkår(behandling.id) } returns
            listOf(
                vilkår(
                    behandlingId = behandling.id,
                    type = VilkårType.REISE_TIL_SAMLING,
                    resultat = Vilkårsresultat.OPPFYLT,
                    status = VilkårStatus.NY,
                    fom = 1 januar 2025,
                    tom = 31 januar 2025,
                    utgift = null,
                    fakta =
                        FaktaReiseTilSamlingOffentligTransport(
                            reiseId = dummyReiseId,
                            adresse = "Samlingsgata 1",
                            utgifterOffentligTransport = utgifter,
                        ),
                ),
            )

        val resultat = beregningService.beregn(behandling, vedtaksperioder, TypeVedtak.INNVILGELSE)

        assertThat(resultat.beregningsresultatOffentligTransport.beløp).isEqualTo(utgifter)
        assertThat(resultat.beregningsresultatOffentligTransport.samling).hasSize(1)

        val samling = resultat.beregningsresultatOffentligTransport.samling.single()
        assertThat(samling.reiseId).isEqualTo(dummyReiseId)
        assertThat(samling.adresse).isEqualTo("Samlingsgata 1")
        assertThat(samling.fom).isEqualTo(1 januar 2025)
        assertThat(samling.tom).isEqualTo(31 januar 2025)
        assertThat(samling.utgifterOffentligTransport).isEqualTo(utgifter)
    }

    @Test
    fun `summerer utgifter fra flere oppfylte vilkår`() {
        every { vilkårService.hentOppfylteReiseTilSamlingVilkår(behandling.id) } returns
            listOf(
                vilkår(
                    behandlingId = behandling.id,
                    type = VilkårType.REISE_TIL_SAMLING,
                    resultat = Vilkårsresultat.OPPFYLT,
                    status = VilkårStatus.NY,
                    fom = 1 januar 2025,
                    tom = 15 januar 2025,
                    utgift = null,
                    fakta =
                        FaktaReiseTilSamlingOffentligTransport(
                            reiseId = dummyReiseId,
                            adresse = "Samlingsgata 1",
                            utgifterOffentligTransport = 300,
                        ),
                ),
                vilkår(
                    behandlingId = behandling.id,
                    type = VilkårType.REISE_TIL_SAMLING,
                    resultat = Vilkårsresultat.OPPFYLT,
                    status = VilkårStatus.NY,
                    fom = 20 januar 2025,
                    tom = 31 januar 2025,
                    utgift = null,
                    fakta =
                        FaktaReiseTilSamlingOffentligTransport(
                            reiseId = dummyReiseId,
                            adresse = "Samlingsgata 2",
                            utgifterOffentligTransport = 200,
                        ),
                ),
            )

        val resultat = beregningService.beregn(behandling, vedtaksperioder, TypeVedtak.INNVILGELSE)

        assertThat(resultat.beregningsresultatOffentligTransport.beløp).isEqualTo(500)
        assertThat(resultat.beregningsresultatOffentligTransport.samling).hasSize(2)
    }

    @Test
    fun `kaster brukerfeil hvis ingen oppfylte vilkår`() {
        every { vilkårService.hentOppfylteReiseTilSamlingVilkår(behandling.id) } returns emptyList()

        val feil = catchThrowableOfType<ApiFeil> {
            beregningService.beregn(behandling, vedtaksperioder, TypeVedtak.INNVILGELSE)
        }

        assertThat(feil.message).contains("Innvilgelse er ikke et gyldig vedtaksresultat")
    }
}
