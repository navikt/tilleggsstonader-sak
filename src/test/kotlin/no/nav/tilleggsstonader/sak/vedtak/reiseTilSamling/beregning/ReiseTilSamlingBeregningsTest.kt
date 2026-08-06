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
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBilProvider
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
    private val satsReiseTilSamlingPrivatBilProvider = mockk<SatsPrivatBilProvider>()

    private val beregningService =
        ReiseTilSamlingBeregningService(
            vilkårService,
            vedtaksperiodeValideringService,
            satsReiseTilSamlingPrivatBilProvider,
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
                            utgifterOffentligTransport = 500.toBigDecimal(),
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
                            utgifterOffentligTransport = 200.toBigDecimal(),
                        ),
                ),
            )

        val result =
            beregningService.beregn(
                behandling,
                vedtaksperioder,
                TypeVedtak.INNVILGELSE,
            )
        val offentligTransport = result.offentligTransport
        assertThat(offentligTransport).hasSize(2)
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
        every {
            satsReiseTilSamlingPrivatBilProvider.finnRelevantKilometerSatsForPeriode(any())
        } returns SatsPrivatBil(1 januar 2025, tom = 31 januar 2025, beløp = 2.94.toBigDecimal())
        val result =
            beregningService.beregn(
                behandling,
                vedtaksperioder,
                TypeVedtak.INNVILGELSE,
            )
        val privatBil = result.privatBil
        assertThat(privatBil).hasSize(1)
        assertThat(privatBil.first().beløp).isEqualTo(59.toBigDecimal())
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
                            utgifterOffentligTransport = 300.toBigDecimal(),
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
                            utgifterOffentligTransport = 200.toBigDecimal(),
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
            result.offentligTransport,
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
