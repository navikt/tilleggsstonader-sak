package no.nav.tilleggsstonader.sak.vedtak.reiseOppstartAvslutningHjemreise.beregning

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import no.nav.tilleggsstonader.libs.feil.ApiFeil
import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBilProvider
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseOppstartAvslutningHjemreiseOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseOppstartAvslutningHjemreisePrivatBil
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReiseOppstartAvslutningHjemreiseBeregningServiceTest {
    private val vilkårService = mockk<VilkårService>()
    private val vedtaksperiodeValideringService = mockk<VedtaksperiodeValideringService>()
    private val satsPrivatBilProvider = mockk<SatsPrivatBilProvider>()

    private val beregningService =
        ReiseOppstartAvslutningHjemreiseBeregningService(
            vilkårService,
            vedtaksperiodeValideringService,
            satsPrivatBilProvider,
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
    fun `beregner offentlig transport riktig for flere vilkår`() {
        val aktivitetId1 = VilkårperiodeGlobalId.random()
        val aktivitetId2 = VilkårperiodeGlobalId.random()
        every { vilkårService.hentOppfylteReiseOppstartAvslutningHjemreiseVilkår(behandling.id) } returns
            listOf(
                vilkår(
                    behandlingId = behandling.id,
                    type = VilkårType.REISE_OPPSTART_AVSLUTNING_HJEMREISE,
                    resultat = Vilkårsresultat.OPPFYLT,
                    status = VilkårStatus.NY,
                    fom = 1 januar 2025,
                    tom = 15 januar 2025,
                    fakta =
                        FaktaReiseOppstartAvslutningHjemreiseOffentligTransport(
                            reiseId = dummyReiseId,
                            adresse = "Oppstartsgata 1",
                            typeReiseformål = TypeReiseformål.OPPSTART,
                            utgifterOffentligTransport = 500.toBigDecimal(),
                            aktivitetId = aktivitetId1,
                        ),
                ),
                vilkår(
                    behandlingId = behandling.id,
                    type = VilkårType.REISE_OPPSTART_AVSLUTNING_HJEMREISE,
                    resultat = Vilkårsresultat.OPPFYLT,
                    status = VilkårStatus.NY,
                    fom = 16 januar 2025,
                    tom = 31 januar 2025,
                    fakta =
                        FaktaReiseOppstartAvslutningHjemreiseOffentligTransport(
                            reiseId = dummyReiseId,
                            adresse = "B",
                            typeReiseformål = TypeReiseformål.HJEMREISE,
                            utgifterOffentligTransport = 200.toBigDecimal(),
                            aktivitetId = aktivitetId2,
                        ),
                ),
            )

        val result =
            beregningService.beregn(
                behandling,
                vedtaksperioder,
                TypeVedtak.INNVILGELSE,
                beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
            )
        val offentligTransport = result.offentligTransport
        assertThat(offentligTransport).hasSize(2)
        assertThat(offentligTransport[0].aktivitetId).isEqualTo(aktivitetId1)
        assertThat(offentligTransport[1].aktivitetId).isEqualTo(aktivitetId2)
    }

    @Test
    fun `beregner privat bil riktig for ett vilkår`() {
        val aktivitetId = VilkårperiodeGlobalId.random()
        every { vilkårService.hentOppfylteReiseOppstartAvslutningHjemreiseVilkår(behandling.id) } returns
            listOf(
                vilkår(
                    behandlingId = behandling.id,
                    type = VilkårType.REISE_OPPSTART_AVSLUTNING_HJEMREISE,
                    resultat = Vilkårsresultat.OPPFYLT,
                    status = VilkårStatus.NY,
                    fom = 1 januar 2025,
                    tom = 31 januar 2025,
                    fakta =
                        FaktaReiseOppstartAvslutningHjemreisePrivatBil(
                            reiseId = dummyReiseId,
                            adresse = "Oppstartsgata 1",
                            typeReiseformål = TypeReiseformål.AVSLUTNING,
                            reiseavstand = 20.toBigDecimal(),
                            aktivitetId = aktivitetId,
                        ),
                ),
            )
        every {
            satsPrivatBilProvider.finnRelevantKilometerSatsForPeriode(any())
        } returns SatsPrivatBil(1 januar 2025, tom = 31 januar 2025, beløp = 2.94.toBigDecimal())
        val result =
            beregningService.beregn(
                behandling,
                vedtaksperioder,
                TypeVedtak.INNVILGELSE,
                beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
            )
        val privatBil = result.privatBil
        assertThat(privatBil).hasSize(1)
        assertThat(privatBil.first().beløp).isEqualTo(59.toBigDecimal())
        assertThat(privatBil.first().aktivitetId).isEqualTo(aktivitetId)
    }

    @Test
    fun `filtrerer bort vilkår som ikke overlapper vedtaksperiodene`() {
        every { vilkårService.hentOppfylteReiseOppstartAvslutningHjemreiseVilkår(behandling.id) } returns
            listOf(
                vilkår(
                    behandlingId = behandling.id,
                    type = VilkårType.REISE_OPPSTART_AVSLUTNING_HJEMREISE,
                    resultat = Vilkårsresultat.OPPFYLT,
                    status = VilkårStatus.NY,
                    fom = 1 januar 2025,
                    tom = 31 januar 2025,
                    fakta =
                        FaktaReiseOppstartAvslutningHjemreiseOffentligTransport(
                            reiseId = dummyReiseId,
                            adresse = "A",
                            typeReiseformål = TypeReiseformål.OPPSTART,
                            utgifterOffentligTransport = 300.toBigDecimal(),
                            aktivitetId = VilkårperiodeGlobalId.random(),
                        ),
                ),
                vilkår(
                    behandlingId = behandling.id,
                    type = VilkårType.REISE_OPPSTART_AVSLUTNING_HJEMREISE,
                    resultat = Vilkårsresultat.OPPFYLT,
                    status = VilkårStatus.NY,
                    fom = 1 februar 2025,
                    tom = 28 februar 2025,
                    fakta =
                        FaktaReiseOppstartAvslutningHjemreiseOffentligTransport(
                            reiseId = dummyReiseId,
                            adresse = "B",
                            typeReiseformål = TypeReiseformål.OPPSTART,
                            utgifterOffentligTransport = 200.toBigDecimal(),
                            aktivitetId = VilkårperiodeGlobalId.random(),
                        ),
                ),
            )

        val result =
            beregningService.beregn(
                behandling,
                vedtaksperioder,
                TypeVedtak.INNVILGELSE,
                beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
            )
        assertThat(result.offentligTransport).hasSize(1)
    }

    @Test
    fun `kaster brukerfeil hvis ingen oppfylte vilkår`() {
        every { vilkårService.hentOppfylteReiseOppstartAvslutningHjemreiseVilkår(behandling.id) } returns emptyList()

        val feil =
            catchThrowableOfType<ApiFeil> {
                beregningService.beregn(
                    behandling,
                    vedtaksperioder,
                    TypeVedtak.INNVILGELSE,
                    beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                )
            }

        assertThat(feil.message)
            .contains("Det er ikke lagt inn noen oppfylte utgiftsperioder")
    }
}
