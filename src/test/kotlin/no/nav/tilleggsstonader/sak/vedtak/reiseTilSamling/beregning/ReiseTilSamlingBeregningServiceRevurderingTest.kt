package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import no.nav.tilleggsstonader.libs.feil.Feil
import no.nav.tilleggsstonader.libs.test.assertions.catchThrowableOfType
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsgrunnlagOffentligTransportForSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBilProvider
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseTilSamlingOffentligTransport
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tester kombinasjonslogikken i [ReiseTilSamlingBeregningService.beregn] for revurdering,
 * dvs. gjenbruk av uendrede reiser fra forrige iverksatte vedtak kombinert med reberegning
 * av berørte/nye reiser, samt håndteringen av [Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT].
 */
class ReiseTilSamlingBeregningServiceRevurderingTest {
    private val vilkårService = mockk<VilkårService>()
    private val vedtaksperiodeValideringService = mockk<VedtaksperiodeValideringService>()
    private val satsPrivatBilProvider = mockk<SatsPrivatBilProvider>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    private val vedtakRepository = mockk<VedtakRepository>()

    private val beregningService =
        ReiseTilSamlingBeregningService(
            vilkårService,
            vedtaksperiodeValideringService,
            satsPrivatBilProvider,
            arbeidsfordelingService,
            vedtakRepository,
        )

    private val forrigeBehandlingId = BehandlingId.random()
    private val behandling = saksbehandling(forrigeIverksatteBehandlingId = forrigeBehandlingId)

    private val vedtaksperioder =
        listOf(
            vedtaksperiode(fom = 1 januar 2025, tom = 28 februar 2025),
        )

    @BeforeEach
    fun setup() {
        justRun {
            vedtaksperiodeValideringService.validerVedtaksperioder(any(), any(), any())
        }
    }

    private fun offentligTransportVilkår(
        reiseId: ReiseId,
        fom: java.time.LocalDate,
        tom: java.time.LocalDate,
        beløp: Int = 500,
    ) = vilkår(
        behandlingId = behandling.id,
        type = VilkårType.REISE_TIL_SAMLING,
        resultat = Vilkårsresultat.OPPFYLT,
        status = VilkårStatus.NY,
        fom = fom,
        tom = tom,
        fakta =
            FaktaReiseTilSamlingOffentligTransport(
                reiseId = reiseId,
                adresse = "Samlingsgata 1",
                utgifterOffentligTransport = beløp.toBigDecimal(),
                begrunnelse = "Togbillett",
            ),
    )

    private fun forrigeVedtakMedResultat(
        vararg resultater: BeregningsresultatOffentligTransport,
    ): GeneriskVedtak<InnvilgelseReiseTilSamling> =
        GeneriskVedtak(
            behandlingId = forrigeBehandlingId,
            data =
                InnvilgelseReiseTilSamling(
                    beregningsresultat =
                        BeregningsresultatReiseTilSamling(
                            offentligTransport = resultater.toList(),
                            privatBil = emptyList(),
                        ),
                    vedtaksperioder = vedtaksperioder,
                    beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                ),
            type = TypeVedtak.INNVILGELSE,
            gitVersjon = "versjon-test",
            tidligsteEndring = null,
            opphørsdato = null,
        )

    private fun forrigeResultat(
        reiseId: ReiseId,
        fom: java.time.LocalDate,
        tom: java.time.LocalDate,
        beløp: Int,
    ) = BeregningsresultatOffentligTransport(
        reiseId = reiseId,
        aktivitetId = null,
        grunnlag =
            BeregningsgrunnlagOffentligTransportForSamling(
                adresse = "Samlingsgata 1",
                fom = fom,
                tom = tom,
                vedtaksperioder = emptyList(),
                brukersNavKontor = null,
            ),
        beløp = beløp.toBigDecimal(),
        fraTidligereVedtak = false,
    )

    @Test
    fun `uendret reise matches på reiseId mot forrige vedtak og gjenbrukes med fraTidligereVedtak lik true`() {
        val uendretReiseId = ReiseId.random()
        val nyReiseId = ReiseId.random()

        every { vilkårService.hentOppfylteReiseTilSamlingVilkår(behandling.id) } returns
            listOf(
                offentligTransportVilkår(uendretReiseId, 1 januar 2025, 31 januar 2025, beløp = 500),
                offentligTransportVilkår(nyReiseId, 1 februar 2025, 28 februar 2025, beløp = 300),
            )
        every { vedtakRepository.findByIdOrThrow(forrigeBehandlingId) } returns
            forrigeVedtakMedResultat(
                forrigeResultat(uendretReiseId, 1 januar 2025, 31 januar 2025, beløp = 500),
            )

        val result =
            beregningService.beregn(
                behandling,
                vedtaksperioder,
                TypeVedtak.INNVILGELSE,
                beregningsplan = Beregningsplan(Beregningsomfang.FRA_DATO, fraDato = 1 februar 2025),
            )

        assertThat(result.offentligTransport).hasSize(2)

        val gjenbruktReise = result.offentligTransport.single { it.reiseId == uendretReiseId }
        assertThat(gjenbruktReise.fraTidligereVedtak).isTrue()
        assertThat(gjenbruktReise.beløp).isEqualTo(500.toBigDecimal())

        val reberegnetReise = result.offentligTransport.single { it.reiseId == nyReiseId }
        assertThat(reberegnetReise.fraTidligereVedtak).isFalse()
        assertThat(reberegnetReise.beløp).isEqualTo(300.toBigDecimal())
    }

    @Test
    fun `reise som slutter nøyaktig på beregnFra reberegnes, siden splitten bruker tom mindre enn beregnFra for uendret`() {
        val reiseId = ReiseId.random()
        val beregnFra = 31 januar 2025

        // Reisen slutter nøyaktig på beregnFra (31 januar), altså tom == beregnFra, som IKKE er
        // "tom < beregnFra" -> reisen regnes som berørt og skal reberegnes fra bunnen, ikke gjenbrukes.
        every { vilkårService.hentOppfylteReiseTilSamlingVilkår(behandling.id) } returns
            listOf(
                offentligTransportVilkår(reiseId, 1 januar 2025, beregnFra, beløp = 500),
            )
        // Ingen stubbing av forrige vedtak sitt resultat nødvendig her, ettersom reisen skal reberegnes
        // og aldri slår opp i forrige vedtak sitt beregningsresultat.
        every { vedtakRepository.findByIdOrThrow(forrigeBehandlingId) } returns
            forrigeVedtakMedResultat()

        val result =
            beregningService.beregn(
                behandling,
                listOf(vedtaksperiode(fom = 1 januar 2025, tom = beregnFra)),
                TypeVedtak.INNVILGELSE,
                beregningsplan = Beregningsplan(Beregningsomfang.FRA_DATO, fraDato = beregnFra),
            )

        val reise = result.offentligTransport.single()
        assertThat(reise.fraTidligereVedtak).isFalse()
    }

    @Test
    fun `reise som starter nøyaktig på beregnFra reberegnes`() {
        val reiseId = ReiseId.random()
        val beregnFra = 1 februar 2025

        every { vilkårService.hentOppfylteReiseTilSamlingVilkår(behandling.id) } returns
            listOf(
                offentligTransportVilkår(reiseId, beregnFra, 28 februar 2025, beløp = 300),
            )
        every { vedtakRepository.findByIdOrThrow(forrigeBehandlingId) } returns
            forrigeVedtakMedResultat()

        val result =
            beregningService.beregn(
                behandling,
                listOf(vedtaksperiode(fom = beregnFra, tom = 28 februar 2025)),
                TypeVedtak.INNVILGELSE,
                beregningsplan = Beregningsplan(Beregningsomfang.FRA_DATO, fraDato = beregnFra),
            )

        val reise = result.offentligTransport.single()
        assertThat(reise.fraTidligereVedtak).isFalse()
    }

    @Test
    fun `reise som slutter dagen før beregnFra regnes som uendret og gjenbrukes`() {
        val reiseId = ReiseId.random()
        val beregnFra = 1 februar 2025

        every { vilkårService.hentOppfylteReiseTilSamlingVilkår(behandling.id) } returns
            listOf(
                offentligTransportVilkår(reiseId, 1 januar 2025, 31 januar 2025, beløp = 500),
            )
        every { vedtakRepository.findByIdOrThrow(forrigeBehandlingId) } returns
            forrigeVedtakMedResultat(
                forrigeResultat(reiseId, 1 januar 2025, 31 januar 2025, beløp = 500),
            )

        val result =
            beregningService.beregn(
                behandling,
                listOf(vedtaksperiode(fom = 1 januar 2025, tom = 31 januar 2025)),
                TypeVedtak.INNVILGELSE,
                beregningsplan = Beregningsplan(Beregningsomfang.FRA_DATO, fraDato = beregnFra),
            )

        val reise = result.offentligTransport.single()
        assertThat(reise.fraTidligereVedtak).isTrue()
        assertThat(reise.beløp).isEqualTo(500.toBigDecimal())
    }

    @Test
    fun `GJENBRUK_FORRIGE_RESULTAT returnerer forrige beregningsresultat med alle poster merket fraTidligereVedtak lik true`() {
        val reiseId1 = ReiseId.random()
        val reiseId2 = ReiseId.random()

        every { vedtakRepository.findByIdOrThrow(forrigeBehandlingId) } returns
            forrigeVedtakMedResultat(
                forrigeResultat(reiseId1, 1 januar 2025, 31 januar 2025, beløp = 500),
                forrigeResultat(reiseId2, 1 februar 2025, 28 februar 2025, beløp = 300),
            )

        val result =
            beregningService.beregn(
                behandling,
                vedtaksperioder,
                TypeVedtak.INNVILGELSE,
                beregningsplan = Beregningsplan(Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT),
            )

        assertThat(result.offentligTransport).hasSize(2)
        assertThat(result.offentligTransport.all { it.fraTidligereVedtak }).isTrue()
        assertThat(result.offentligTransport.map { it.reiseId to it.beløp })
            .containsExactlyInAnyOrder(
                reiseId1 to 500.toBigDecimal(),
                reiseId2 to 300.toBigDecimal(),
            )
    }

    @Test
    fun `GJENBRUK_FORRIGE_RESULTAT kaster feil dersom det ikke finnes noe forrige iverksatt vedtak`() {
        val behandlingUtenForrige = saksbehandling(forrigeIverksatteBehandlingId = null)

        val feil =
            catchThrowableOfType<IllegalArgumentException> {
                beregningService.beregn(
                    behandlingUtenForrige,
                    vedtaksperioder,
                    TypeVedtak.INNVILGELSE,
                    beregningsplan = Beregningsplan(Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT),
                )
            }

        assertThat(feil.message).contains("Kan ikke gjenbruke forrige beregningsresultat")
    }

    @Test
    fun `kaster tydelig feil dersom en uendret reise ikke finnes i forrige vedtak sitt beregningsresultat`() {
        val uendretReiseId = ReiseId.random()
        val beregnFra = 1 februar 2025

        every { vilkårService.hentOppfylteReiseTilSamlingVilkår(behandling.id) } returns
            listOf(
                offentligTransportVilkår(uendretReiseId, 1 januar 2025, 31 januar 2025, beløp = 500),
            )
        // Forrige vedtak sitt beregningsresultat mangler reisen med uendretReiseId - skal ikke skje i praksis,
        // men koden skal kaste en tydelig feil i stedet for å feile stille (f.eks. NoSuchElementException).
        every { vedtakRepository.findByIdOrThrow(forrigeBehandlingId) } returns
            forrigeVedtakMedResultat(
                forrigeResultat(ReiseId.random(), 1 desember 2024, 31 desember 2024, beløp = 100),
            )

        val feil =
            catchThrowableOfType<Feil> {
                beregningService.beregn(
                    behandling,
                    listOf(vedtaksperiode(fom = 1 januar 2025, tom = 31 januar 2025)),
                    TypeVedtak.INNVILGELSE,
                    beregningsplan = Beregningsplan(Beregningsomfang.FRA_DATO, fraDato = beregnFra),
                )
            }

        assertThat(feil.message)
            .contains("Fant ikke forrige beregningsresultat for offentlig transport med reiseId=$uendretReiseId")
    }
}
