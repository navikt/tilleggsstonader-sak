package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseTilSamlingOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseTilSamlingPrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReiseTilSamlingBeregningsTest {
    private val vilkårService = mockk<VilkårService>()
    private val vedtaksperiodeValideringService = mockk<VedtaksperiodeValideringService>()

    private val beregningService =
        ReiseTilSamlingBeregningService(
            vilkårService,
            vedtaksperiodeValideringService,
        )

    private val behandling = saksbehandling()

    private val vedtaksperioder =
        listOf(
            vedtaksperiode(fom = 1 januar 2025, tom = 31 januar 2025),
        )

    @BeforeEach
    fun setup() {
        justRun {
            vedtaksperiodeValideringService.validerVedtaksperioder(any(), any(), any())
        }
    }

    @Test
    fun `beregner offentlig transport riktig for ett vilkår`() {
        every { vilkårService.hentOppfylteReiseTilSamlingVilkår(behandling.id) } returns
            listOf(
                vilkår(
                    behandlingId = behandling.id,
                    type = VilkårType.REISE_TIL_SAMLING,
                    resultat = Vilkårsresultat.OPPFYLT,
                    status = VilkårStatus.NY,
                    fom = 1 januar 2025,
                    tom = 15 januar 2025,
                    fakta =
                        FaktaReiseTilSamlingOffentligTransport(
                            reiseId = dummyReiseId,
                            adresse = "Samlingsgata 1",
                            utgifterOffentligTransport = 500,
                        ),
                ),
                vilkår(
                    behandlingId = behandling.id,
                    type = VilkårType.REISE_TIL_SAMLING,
                    resultat = Vilkårsresultat.OPPFYLT,
                    status = VilkårStatus.NY,
                    fom = 16 januar 2025,
                    tom = 31 januar 2025,
                    fakta =
                        FaktaReiseTilSamlingOffentligTransport(
                            reiseId = dummyReiseId,
                            adresse = "B",
                            utgifterOffentligTransport = 200,
                        ),
                ),
            )

        val result =
            beregningService.beregn(
                behandling,
                vedtaksperioder,
                TypeVedtak.INNVILGELSE,
            )
        val privatBil =
            result.reiser.filterIsInstance<BeregningsresultatPrivatBil>().single()
        val offentligTransport =
            result.reiser.filterIsInstance<BeregningsresultatOffentligTransport>().single()
        assertThat(privatBil.reiser).hasSize(0)
        assertThat(offentligTransport.reiser).hasSize(2)
    }

    @Test
    fun `beregner privat bil riktig for ett vilkår`() {
        every { vilkårService.hentOppfylteReiseTilSamlingVilkår(behandling.id) } returns
            listOf(
                vilkår(
                    behandlingId = behandling.id,
                    type = VilkårType.REISE_TIL_SAMLING,
                    resultat = Vilkårsresultat.OPPFYLT,
                    status = VilkårStatus.NY,
                    fom = 1 januar 2025,
                    tom = 31 januar 2025,
                    fakta =
                        FaktaReiseTilSamlingPrivatBil(
                            reiseId = dummyReiseId,
                            adresse = "Samlingsgata 1",
                            reiseavstand = 20.toBigDecimal(),
                        ),
                ),
            )

        val result =
            beregningService.beregn(
                behandling,
                vedtaksperioder,
                TypeVedtak.INNVILGELSE,
            )
        val privatBil =
            result.reiser.filterIsInstance<BeregningsresultatPrivatBil>().single()
        val offentligTransport =
            result.reiser.filterIsInstance<BeregningsresultatOffentligTransport>().single()
        assertThat(privatBil.reiser).hasSize(1)
        assertThat(offentligTransport.reiser).hasSize(0)
        assertThat(privatBil.reiser.first().beløp).isEqualTo(58)
    }

    @Test
    fun `filtrerer bort vilkår som ikke overlapper vedtaksperiodene`() {
        every { vilkårService.hentOppfylteReiseTilSamlingVilkår(behandling.id) } returns
            listOf(
                vilkår(
                    behandlingId = behandling.id,
                    type = VilkårType.REISE_TIL_SAMLING,
                    resultat = Vilkårsresultat.OPPFYLT,
                    status = VilkårStatus.NY,
                    fom = 1 januar 2025,
                    tom = 31 januar 2025,
                    fakta =
                        FaktaReiseTilSamlingOffentligTransport(
                            reiseId = dummyReiseId,
                            adresse = "A",
                            utgifterOffentligTransport = 300,
                        ),
                ),
                vilkår(
                    behandlingId = behandling.id,
                    type = VilkårType.REISE_TIL_SAMLING,
                    resultat = Vilkårsresultat.OPPFYLT,
                    status = VilkårStatus.NY,
                    fom = 1 februar 2025,
                    tom = 28 februar 2025,
                    fakta =
                        FaktaReiseTilSamlingOffentligTransport(
                            reiseId = dummyReiseId,
                            adresse = "B",
                            utgifterOffentligTransport = 200,
                        ),
                ),
            )

        val result =
            beregningService.beregn(
                behandling,
                vedtaksperioder,
                TypeVedtak.INNVILGELSE,
            )
        assertThat(
            result.reiser
                .filterIsInstance<BeregningsresultatOffentligTransport>()
                .single()
                .reiser,
        ).hasSize(1)
    }

    @Test
    fun `kaster brukerfeil hvis ingen oppfylte vilkår`() {
        every { vilkårService.hentOppfylteReiseTilSamlingVilkår(behandling.id) } returns emptyList()

        val feil =
            catchThrowableOfType<ApiFeil> {
                beregningService.beregn(
                    behandling,
                    vedtaksperioder,
                    TypeVedtak.INNVILGELSE,
                )
            }

        assertThat(feil.message)
            .contains("Det er ikke lagt inn noen oppfylte utgiftsperioder")
    }
}
