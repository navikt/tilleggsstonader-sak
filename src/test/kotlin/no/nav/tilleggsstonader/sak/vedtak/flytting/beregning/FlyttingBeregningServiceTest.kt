package no.nav.tilleggsstonader.sak.vedtak.flytting.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.libs.feil.Feil
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vedtak.sats.SatsPrivatBilProvider
import no.nav.tilleggsstonader.sak.vedtak.validering.VedtaksperiodeValideringService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlytteSelv
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttebyrå
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttebyråTilbud
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.FlyttingVilkårService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class FlyttingBeregningServiceTest {
    private val flyttingVilkårService = mockk<FlyttingVilkårService>()
    private val vedtaksperiodeValideringService = mockk<VedtaksperiodeValideringService>(relaxed = true)
    private val beregningService = FlyttingBeregningService(flyttingVilkårService, SatsPrivatBilProvider(), vedtaksperiodeValideringService)
    private val behandling = saksbehandling()

    @Test
    fun `velger laveste pris fra flyttebyrå uavhengig av tilbudsrekkefølge`() {
        listOf(
            10_000 to 12_000,
            12_000 to 10_000,
            10_000 to 10_000,
        ).forEach { (tilbud1, tilbud2) ->
            every { flyttingVilkårService.hentVilkårForBehandling(behandling.id) } returns
                listOf(flyttebyrå(tilbud1, tilbud2))

            val resultat = beregningService.beregn(behandling, listOf(vedtaksperiode(1 januar 2026, 31 januar 2026)))

            assertThat(resultat.resultater.single().beløp).isEqualByComparingTo("10000")
        }
    }

    @Test
    fun `beregner egen kjøring med enveisavstand, tillegg og avrunding`() {
        val vilkår = egenKjøring(fakta = FaktaFlytteSelv(250, 1500, 300, 400, 100))
        every { flyttingVilkårService.hentVilkårForBehandling(behandling.id) } returns listOf(vilkår)

        val resultat = beregningService.beregn(behandling, listOf(vedtaksperiode(1 januar 2026, 31 januar 2026)))

        assertThat(resultat.resultater.single().beløp).isEqualByComparingTo("3035")
    }

    @Test
    fun `runder kilometerbeløp HALF_UP og manglende tillegg regnes som null`() {
        val beregnedeBeløp =
            listOf(24 to "71", 26 to "76", 25 to "74").map { (avstand, forventet) ->
                every { flyttingVilkårService.hentVilkårForBehandling(behandling.id) } returns
                    listOf(egenKjøring(fakta = FaktaFlytteSelv(avstand, null, null, null, null)))
                val resultat =
                    beregningService
                        .beregn(behandling, listOf(vedtaksperiode(1 januar 2026, 31 januar 2026)))
                        .resultater
                        .single()
                assertThat(resultat.beløp).isEqualByComparingTo(forventet)
                resultat.beløp
            }

        assertThat(beregnedeBeløp).containsExactly(
            BigDecimal("71"),
            BigDecimal("76"),
            BigDecimal("74"),
        )
    }

    @Test
    fun `velger sats fra vilkårets FOM og ikke fra hele vilkårsperioden`() {
        val vilkår = egenKjøring(fom = 31 desember 2025, tom = 1 januar 2026, fakta = FaktaFlytteSelv(100, null, null, null, null))
        every { flyttingVilkårService.hentVilkårForBehandling(behandling.id) } returns listOf(vilkår)

        val resultat =
            beregningService.beregn(
                behandling,
                listOf(vedtaksperiode(31 desember 2025, 1 januar 2026)),
            )

        assertThat(resultat.resultater.single().beløp).isEqualByComparingTo("288")
        val grunnlag =
            resultat.resultater.single().grunnlag as
                no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagEgenKjøring
        assertThat(grunnlag.sats).isEqualByComparingTo("2.88")
        assertThat(grunnlag.satsBekreftet).isTrue()
    }

    @Test
    fun `bruker videreført ubekreftet sats når vilkåret starter etter siste bekreftede sats`() {
        val vilkår = egenKjøring(fom = 1 januar 2027, tom = 31 januar 2027, fakta = FaktaFlytteSelv(100, null, null, null, null))
        every { flyttingVilkårService.hentVilkårForBehandling(behandling.id) } returns listOf(vilkår)

        val resultat =
            beregningService
                .beregn(
                    behandling,
                    listOf(vedtaksperiode(1 januar 2027, 31 januar 2027)),
                ).resultater
                .single()

        assertThat(resultat.beløp).isEqualByComparingTo("294")
        assertThat((resultat.grunnlag as no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagEgenKjøring).satsBekreftet)
            .isFalse()
    }

    @Test
    fun `lager ett resultat per vilkår selv når samme vilkår dekkes av flere perioder`() {
        val vilkårJanuar = flyttebyrå(10_000, 12_000, 1 januar 2026, 31 januar 2026)
        val vilkårFebruar = flyttebyrå(12_000, 10_000, 1 februar 2026, 28 februar 2026)
        every { flyttingVilkårService.hentVilkårForBehandling(behandling.id) } returns listOf(vilkårFebruar, vilkårJanuar)

        val resultat =
            beregningService.beregn(
                behandling,
                listOf(
                    vedtaksperiode(1 januar 2026, 15 januar 2026),
                    vedtaksperiode(16 januar 2026, 31 januar 2026),
                    vedtaksperiode(1 februar 2026, 28 februar 2026),
                ),
            )

        assertThat(resultat.resultater.map { it.fom }).containsExactly(1 januar 2026, 1 februar 2026)
        assertThat(resultat.resultater.map { it.beløp }).containsExactly(BigDecimal("10000"), BigDecimal("10000"))
    }

    @Test
    fun `avviser manglende pris og manglende dekning av hele vilkåret`() {
        every { flyttingVilkårService.hentVilkårForBehandling(behandling.id) } returns
            listOf(flyttebyrå(10_000, null))
        assertThatThrownBy {
            beregningService.beregn(behandling, listOf(vedtaksperiode(1 januar 2026, 31 januar 2026)))
        }.isInstanceOf(Feil::class.java)

        every { flyttingVilkårService.hentVilkårForBehandling(behandling.id) } returns listOf(flyttebyrå(10_000, 12_000))
        assertThatThrownBy {
            beregningService.beregn(behandling, listOf(vedtaksperiode(1 januar 2026, 30 januar 2026)))
        }.isInstanceOf(Feil::class.java)
    }

    @Test
    fun `ignorerer slettet vilkår og avviser tomt beregningsgrunnlag`() {
        val slettet =
            flyttebyrå(10_000, 12_000).copy(
                resultat = Vilkårsresultat.SLETTET,
                status = VilkårStatus.SLETTET,
                slettetKommentar = "Slettet i test",
            )
        every { flyttingVilkårService.hentVilkårForBehandling(behandling.id) } returns listOf(slettet)

        assertThatThrownBy {
            beregningService.beregn(behandling, listOf(vedtaksperiode(1 januar 2026, 31 januar 2026)))
        }.isInstanceOf(Feil::class.java)
    }

    private fun flyttebyrå(
        tilbud1: Int?,
        tilbud2: Int?,
        fom: java.time.LocalDate = 1 januar 2026,
        tom: java.time.LocalDate = 31 januar 2026,
    ) = vilkår(
        behandlingId = behandling.id,
        type = VilkårType.FLYTTING,
        resultat = Vilkårsresultat.OPPFYLT,
        status = VilkårStatus.NY,
        fom = fom,
        tom = tom,
        fakta =
            FaktaFlyttebyrå(
                tilbud1 = FlyttebyråTilbud("A", tilbud1),
                tilbud2 = FlyttebyråTilbud("B", tilbud2),
            ),
    )

    private fun egenKjøring(
        fom: java.time.LocalDate = 1 januar 2026,
        tom: java.time.LocalDate = 31 januar 2026,
        fakta: FaktaFlytteSelv,
    ) = vilkår(
        behandlingId = behandling.id,
        type = VilkårType.FLYTTING,
        resultat = Vilkårsresultat.OPPFYLT,
        status = VilkårStatus.NY,
        fom = fom,
        tom = tom,
        fakta = fakta,
    )
}
