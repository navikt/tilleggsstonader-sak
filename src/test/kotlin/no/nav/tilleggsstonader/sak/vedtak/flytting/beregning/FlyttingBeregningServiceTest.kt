package no.nav.tilleggsstonader.sak.vedtak.flytting.beregning

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import no.nav.tilleggsstonader.libs.feil.Feil
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.felles.domain.VilkårId
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakService
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseFlytting
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlytteSelv
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlyttebyrå
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlyttevilkår
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlytting
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBilProvider
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlytteSelv
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttebyrå
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttingUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttebyråTilbud
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttingVilkårFakta
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.FlyttingVilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.domain.VilkårFlytting
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class FlyttingBeregningServiceTest {
    private val flyttingVilkårService = mockk<FlyttingVilkårService>()
    private val vedtakService = mockk<VedtakService>()
    private val vedtaksperiodeValideringService = mockk<VedtaksperiodeValideringService>()
    private val beregningService =
        FlyttingBeregningService(
            flyttingVilkårService = flyttingVilkårService,
            satsPrivatBilProvider = SatsPrivatBilProvider(),
            vedtakService = vedtakService,
            vedtaksperiodeValideringService = vedtaksperiodeValideringService,
        )
    private val behandling = saksbehandling()

    private fun FlyttingBeregningService.beregn(
        behandling: Saksbehandling,
        vedtaksperioder: List<Vedtaksperiode>,
    ) = beregn(
        behandling,
        vedtaksperioder,
        Beregningsplan(Beregningsomfang.ALLE_PERIODER),
        typeVedtak = TypeVedtak.INNVILGELSE,
    )

    @BeforeEach
    fun setup() {
        justRun {
            vedtaksperiodeValideringService.validerVedtaksperioder(any(), any(), any())
        }
        every { flyttingVilkårService.hentOppfylteVilkårforBehandling(any()) } returns emptyList()
    }

    @Test
    fun `flyttebyrå beregnes med laveste tilbudspris og riktig grunnlag`() {
        val periode = vedtaksperiode(fom = 1 januar 2025, tom = 31 januar 2025)
        every { flyttingVilkårService.hentOppfylteVilkårforBehandling(behandling.id) } returns
            listOf(
                vilkår(
                    fom = 1 januar 2025,
                    tom = 31 januar 2025,
                    fakta =
                        FaktaFlyttebyrå(
                            tilbud1 = FlyttebyråTilbud(navn = "Tilbud 1", pris = 123),
                            tilbud2 = FlyttebyråTilbud(navn = "Tilbud 2", pris = 124),
                            erBetalingDokumentert = true,
                            adresse = "Flytteveien 1",
                        ),
                ),
            )

        val resultat = beregningService.beregn(behandling, listOf(periode)).resultater.single()

        assertThat(resultat.beløp).isEqualByComparingTo("123")
        assertThat(resultat.grunnlag)
            .isEqualTo(
                BeregningsgrunnlagFlyttebyrå(
                    tilbud1Pris = BigDecimal("123"),
                    tilbud2Pris = BigDecimal("124"),
                    erBetalingDokumentert = true,
                ),
            )
        assertThat(resultat.målgruppe).isEqualTo(periode.målgruppe)
        assertThat(resultat.fraTidligereVedtak).isFalse()
    }

    @Test
    fun `egen flytting beregnes med kilometersats og tilleggskostnader`() {
        val periode = vedtaksperiode(fom = 1 januar 2025, tom = 31 januar 2025)
        every { flyttingVilkårService.hentOppfylteVilkårforBehandling(behandling.id) } returns
            listOf(
                vilkår(
                    fom = 1 januar 2025,
                    tom = 31 januar 2025,
                    fakta =
                        FaktaFlytteSelv(
                            avstandEnVei = 100,
                            henger = 100,
                            bompenger = 20,
                            ferge = 30,
                            parkering = 40,
                            adresse = "Flytteveien 1",
                        ),
                ),
            )

        val resultat = beregningService.beregn(behandling, listOf(periode)).resultater.single()

        assertThat(resultat.beløp).isEqualByComparingTo("478")
        assertThat(resultat.grunnlag)
            .isEqualTo(
                BeregningsgrunnlagFlytteSelv(
                    avstandEnVei = 100,
                    sats = BigDecimal("2.88"),
                    satsBekreftet = true,
                    henger = BigDecimal("100"),
                    bompenger = BigDecimal("20"),
                    ferge = BigDecimal("30"),
                    parkering = BigDecimal("40"),
                ),
            )
    }

    @Test
    fun `manglende tilleggskostnader for egen flytting regnes som null`() {
        every { flyttingVilkårService.hentOppfylteVilkårforBehandling(behandling.id) } returns
            listOf(
                vilkår(
                    fom = 1 januar 2025,
                    tom = 31 januar 2025,
                    fakta =
                        FaktaFlytteSelv(
                            avstandEnVei = 1,
                            henger = null,
                            bompenger = null,
                            ferge = null,
                            parkering = null,
                            adresse = "Flytteveien 1",
                        ),
                ),
            )

        val resultat =
            beregningService
                .beregn(behandling, listOf(vedtaksperiode(fom = 1 januar 2025, tom = 31 januar 2025)))
                .resultater
                .single()

        assertThat(resultat.beløp).isEqualByComparingTo("3")
        assertThat(resultat.grunnlag)
            .isEqualTo(
                BeregningsgrunnlagFlytteSelv(
                    avstandEnVei = 1,
                    sats = BigDecimal("2.88"),
                    satsBekreftet = true,
                    henger = BigDecimal.ZERO,
                    bompenger = BigDecimal.ZERO,
                    ferge = BigDecimal.ZERO,
                    parkering = BigDecimal.ZERO,
                ),
            )
    }

    @Test
    fun `resultater beregnes for alle vilkår og sorteres etter startdato`() {
        val førsteVilkår = vilkår(fom = 1 februar 2025, tom = 28 februar 2025)
        val andreVilkår = vilkår(fom = 1 januar 2025, tom = 31 januar 2025)
        val vedtaksperioder =
            listOf(
                vedtaksperiode(
                    fom = 1 februar 2025,
                    tom = 28 februar 2025,
                    målgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER,
                ),
                vedtaksperiode(
                    fom = 1 januar 2025,
                    tom = 31 januar 2025,
                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                ),
            )
        every { flyttingVilkårService.hentOppfylteVilkårforBehandling(behandling.id) } returns
            listOf(førsteVilkår, andreVilkår)

        val resultater = beregningService.beregn(behandling, vedtaksperioder).resultater

        assertThat(resultater.map { it.fom }).containsExactly(1 januar 2025, 1 februar 2025)
        assertThat(resultater.map { it.målgruppe })
            .containsExactly(FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE, FaktiskMålgruppe.ENSLIG_FORSØRGER)
    }

    @Test
    fun `beregning fra dato gjenbruker eldre resultat og beregner nye perioder`() {
        val forrigeBehandlingId = BehandlingId.random()
        val revurdering = saksbehandling(forrigeIverksatteBehandlingId = forrigeBehandlingId)
        val forrigeResultat =
            resultat(
                fom = 1 januar 2025,
                tom = 31 januar 2025,
                beløp = "500",
            )
        stubForrigeVedtak(forrigeBehandlingId, forrigeResultat)
        every { flyttingVilkårService.hentOppfylteVilkårforBehandling(revurdering.id) } returns
            listOf(vilkår(fom = 1 februar 2025, tom = 28 februar 2025, behandlingId = revurdering.id))

        val resultater =
            beregningService
                .beregn(
                    revurdering,
                    listOf(
                        vedtaksperiode(fom = 1 januar 2025, tom = 31 januar 2025),
                        vedtaksperiode(fom = 1 februar 2025, tom = 28 februar 2025),
                    ),
                    Beregningsplan(Beregningsomfang.FRA_DATO, fraDato = 1 februar 2025),
                    TypeVedtak.INNVILGELSE,
                ).resultater

        assertThat(resultater.map { it.fom }).containsExactly(1 januar 2025, 1 februar 2025)
        assertThat(resultater[0]).isEqualTo(forrigeResultat.copy(fraTidligereVedtak = true))
        assertThat(resultater[1].fraTidligereVedtak).isFalse()
    }

    @Test
    fun `gjenbruk av forrige resultat merker alle resultater som tidligere vedtak`() {
        val forrigeBehandlingId = BehandlingId.random()
        val revurdering = saksbehandling(forrigeIverksatteBehandlingId = forrigeBehandlingId)
        val resultat1 = resultat(fom = 1 januar 2025, tom = 31 januar 2025, beløp = "500")
        val resultat2 = resultat(fom = 1 februar 2025, tom = 28 februar 2025, beløp = "300")
        stubForrigeVedtak(forrigeBehandlingId, resultat1, resultat2)

        val beregningsresultat =
            beregningService.beregn(
                revurdering,
                emptyList(),
                Beregningsplan(Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT),
                TypeVedtak.INNVILGELSE,
            )

        assertThat(beregningsresultat.resultater)
            .containsExactly(
                resultat1.copy(fraTidligereVedtak = true),
                resultat2.copy(fraTidligereVedtak = true),
            )
        verify {
            vedtaksperiodeValideringService.validerVedtaksperioder(
                emptyList(),
                revurdering,
                TypeVedtak.INNVILGELSE,
            )
        }
    }

    @Test
    fun `flyttebyrå avviser manglende og negative tilbudspriser`() {
        val ugyldigeFakta =
            listOf(
                faktaFlyttebyrå(tilbud1Pris = null),
                faktaFlyttebyrå(tilbud2Pris = null),
                faktaFlyttebyrå(tilbud1Pris = -1),
                faktaFlyttebyrå(tilbud2Pris = -1),
            )

        ugyldigeFakta.forEach { fakta ->
            every { flyttingVilkårService.hentOppfylteVilkårforBehandling(behandling.id) } returns
                listOf(vilkår(fom = 1 januar 2025, tom = 31 januar 2025, fakta = fakta))

            assertThatThrownBy {
                beregningService.beregn(
                    behandling,
                    listOf(vedtaksperiode(fom = 1 januar 2025, tom = 31 januar 2025)),
                )
            }.isInstanceOf(Feil::class.java)
        }
    }

    @Test
    fun `egen flytting avviser manglende eller negative avstander og negative tilleggskostnader`() {
        val ugyldigeFakta =
            listOf(
                faktaFlytteSelv(avstandEnVei = null),
                faktaFlytteSelv(avstandEnVei = -1),
                faktaFlytteSelv(henger = -1),
                faktaFlytteSelv(bompenger = -1),
                faktaFlytteSelv(ferge = -1),
                faktaFlytteSelv(parkering = -1),
            )

        ugyldigeFakta.forEach { fakta ->
            every { flyttingVilkårService.hentOppfylteVilkårforBehandling(behandling.id) } returns
                listOf(vilkår(fom = 1 januar 2025, tom = 31 januar 2025, fakta = fakta))

            assertThatThrownBy {
                beregningService.beregn(
                    behandling,
                    listOf(vedtaksperiode(fom = 1 januar 2025, tom = 31 januar 2025)),
                )
            }.isInstanceOf(Feil::class.java)
        }
    }

    @Test
    fun `ukjente flyttefakta gir tydelig feil`() {
        every { flyttingVilkårService.hentOppfylteVilkårforBehandling(behandling.id) } returns
            listOf(
                vilkår(
                    fom = 1 januar 2025,
                    tom = 31 januar 2025,
                    fakta = FaktaFlyttingUbestemt(adresse = "Flytteveien 1"),
                ),
            )

        assertThatThrownBy {
            beregningService.beregn(
                behandling,
                listOf(vedtaksperiode(fom = 1 januar 2025, tom = 31 januar 2025)),
            )
        }.isInstanceOf(Feil::class.java)
            .hasMessageContaining("mangler fullstendige flyttefakta")
    }

    @Test
    fun `beregning fra dato uten forrige vedtak gir feil`() {
        val behandlingUtenForrige = saksbehandling(forrigeIverksatteBehandlingId = null)

        assertThatThrownBy {
            beregningService.beregn(
                behandlingUtenForrige,
                emptyList(),
                Beregningsplan(Beregningsomfang.FRA_DATO, fraDato = 1 januar 2025),
                TypeVedtak.INNVILGELSE,
            )
        }.isInstanceOf(Feil::class.java)
            .hasMessageContaining("Finner ikke forrige iverksatte vedtak")
    }

    @Test
    fun `gjenbruk uten forrige vedtak gir feil`() {
        val behandlingUtenForrige = saksbehandling(forrigeIverksatteBehandlingId = null)

        assertThatThrownBy {
            beregningService.beregn(
                behandlingUtenForrige,
                emptyList(),
                Beregningsplan(Beregningsomfang.GJENBRUK_FORRIGE_RESULTAT),
                TypeVedtak.INNVILGELSE,
            )
        }.isInstanceOf(Feil::class.java)
            .hasMessageContaining("Finner ikke forrige iverksatte vedtak")
    }

    @Test
    fun `beregningsomfang for kjørelisteturer støttes ikke for flytting`() {
        assertThatThrownBy {
            beregningService.beregn(
                behandling,
                emptyList(),
                Beregningsplan(Beregningsomfang.KUN_NYE_KJORELISTE_UKER),
                TypeVedtak.INNVILGELSE,
            )
        }.isInstanceOf(Feil::class.java)
            .hasMessageContaining("Ustøttet beregningsomfang")
    }

    private fun vilkår(
        fom: LocalDate,
        tom: LocalDate,
        behandlingId: BehandlingId = behandling.id,
        fakta: FlyttingVilkårFakta = faktaFlyttebyrå(),
    ) = VilkårFlytting(
        id = VilkårId.random(),
        behandlingId = behandlingId,
        fom = fom,
        tom = tom,
        resultat = Vilkårsresultat.OPPFYLT,
        status = VilkårStatus.NY,
        delvilkårsett = emptyList(),
        fakta = fakta,
    )

    private fun faktaFlyttebyrå(
        tilbud1Pris: Int? = 123,
        tilbud2Pris: Int? = 124,
    ) = FaktaFlyttebyrå(
        tilbud1 = FlyttebyråTilbud(navn = "Tilbud 1", pris = tilbud1Pris),
        tilbud2 = FlyttebyråTilbud(navn = "Tilbud 2", pris = tilbud2Pris),
        erBetalingDokumentert = true,
        adresse = "Flytteveien 1",
    )

    private fun faktaFlytteSelv(
        avstandEnVei: Int? = 100,
        henger: Int? = 10,
        bompenger: Int? = 20,
        ferge: Int? = 30,
        parkering: Int? = 40,
    ) = FaktaFlytteSelv(
        avstandEnVei = avstandEnVei,
        henger = henger,
        bompenger = bompenger,
        ferge = ferge,
        parkering = parkering,
        adresse = "Flytteveien 1",
    )

    private fun resultat(
        fom: LocalDate,
        tom: LocalDate,
        beløp: String,
    ) = BeregningsresultatFlyttevilkår(
        fom = fom,
        tom = tom,
        beløp = BigDecimal(beløp),
        målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
        grunnlag =
            BeregningsgrunnlagFlyttebyrå(
                tilbud1Pris = BigDecimal("500"),
                tilbud2Pris = BigDecimal("600"),
                erBetalingDokumentert = true,
            ),
    )

    private fun stubForrigeVedtak(
        behandlingId: BehandlingId,
        vararg resultater: BeregningsresultatFlyttevilkår,
    ) {
        val data =
            InnvilgelseFlytting(
                vedtaksperioder = emptyList(),
                beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                beregningsresultat = BeregningsresultatFlytting(resultater.toList()),
            )
        every { vedtakService.hentVedtakEllerFeil(behandlingId) } returns
            GeneriskVedtak(
                behandlingId = behandlingId,
                data = data,
                gitVersjon = null,
                tidligsteEndring = null,
            )
    }
}
