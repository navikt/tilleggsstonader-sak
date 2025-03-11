package no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.faktagrunnlag.FaktaGrunnlagBarnAndreForeldreSaksinformasjonMapper.mapBarnAndreForeldreSaksinformasjon
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.vedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.vedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.UtgiftBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FaktaGrunnlagBarnAndreForeldreSaksinformasjonMapperTest {
    val identBarn1 = "barn1"
    val identBarn2 = "barn2"
    val identAnnenForeldre1 = "forelder"

    val barnId1 = BarnId.random()
    val barnId2 = BarnId.random()
    val mapBarnIdTilIdent = mapOf(barnId1 to identBarn1, barnId2 to identBarn2)

    val behandlingId = BehandlingId.random()

    @Test
    fun `skal mappe behandlingsinformasjon til FaktaGrunnlag og slå sammen vedtaksperioder hvis de er påfølgende`() {
        val barnAnnenForelder = mapOf(identBarn1 to listOf(identAnnenForeldre1))

        val vedtak = lagVedtak()

        val behandlingsinformasjonAnnenForelder =
            BehandlingsinformasjonAnnenForelder(
                identForelder = identAnnenForeldre1,
                finnesIkkeFerdigstiltBehandling = true,
                iverksattBehandling =
                    BehandlingsinformasjonAnnenForelder.IverksattBehandlingForelder(
                        barn = mapBarnIdTilIdent,
                        vedtak = vedtak,
                    ),
            )
        val resultat =
            mapBarnAndreForeldreSaksinformasjon(
                behandlingId = behandlingId,
                barnAnnenForelder = barnAnnenForelder,
                behandlingsinformasjonAnnenForelder = listOf(behandlingsinformasjonAnnenForelder),
            )
        with(resultat.single()) {
            assertThat(this.type).isEqualTo(TypeFaktaGrunnlag.BARN_ANDRE_FORELDRE_SAKSINFORMASJON)
            assertThat(this.typeId).isEqualTo(identBarn1)
            assertThat(this.data.identBarn).isEqualTo(identBarn1)
            with(this.data.andreForeldre.single()) {
                assertThat(this.ident).isEqualTo(identAnnenForeldre1)
                assertThat(this.harBehandlingUnderArbeid).isTrue()
                assertThat(this.vedtaksperioderBarn)
                    .containsExactly(Datoperiode(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 4)))
            }
        }
    }

    @Test
    fun `skal mappe alle barn hvis det finnes flere barn`() {
        val barnAnnenForelder =
            mapOf(
                identBarn1 to listOf(identAnnenForeldre1),
                identBarn2 to listOf(identAnnenForeldre1),
            )

        val vedtak = lagVedtak()

        val behandlingsinformasjonAnnenForelder =
            BehandlingsinformasjonAnnenForelder(
                identForelder = identAnnenForeldre1,
                finnesIkkeFerdigstiltBehandling = true,
                iverksattBehandling =
                    BehandlingsinformasjonAnnenForelder.IverksattBehandlingForelder(
                        barn = mapBarnIdTilIdent,
                        vedtak = vedtak,
                    ),
            )
        val resultat =
            mapBarnAndreForeldreSaksinformasjon(
                behandlingId = behandlingId,
                barnAnnenForelder = barnAnnenForelder,
                behandlingsinformasjonAnnenForelder = listOf(behandlingsinformasjonAnnenForelder),
            )

        assertThat(resultat).hasSize(2)
    }

    @Test
    fun `skal returnere tom liste med vedtaksperioder hvis annen forelder ikke har vedtak`() {
        val barnAnnenForelder =
            mapOf(
                identBarn1 to listOf(identAnnenForeldre1),
            )
        val resultat =
            mapBarnAndreForeldreSaksinformasjon(
                behandlingId = behandlingId,
                barnAnnenForelder = barnAnnenForelder,
                behandlingsinformasjonAnnenForelder = emptyList(),
            )
        assertThat(resultat).hasSize(1)
        assertThat(resultat.single().typeId).isEqualTo(identBarn1)
        with(
            resultat
                .single()
                .data.andreForeldre
                .single(),
        ) {
            assertThat(this.ident).isEqualTo(identAnnenForeldre1)
            assertThat(this.harBehandlingUnderArbeid).isFalse()
            assertThat(this.vedtaksperioderBarn).isEmpty()
        }
    }

    @Test
    fun `skal returnere tom liste med vedtaksperioder hvis annen forelder har vedtak men på annet barn`() {
        val barnAnnenForelder =
            mapOf(
                identBarn2 to listOf(identAnnenForeldre1),
            )
        val vedtak = lagVedtak()
        val behandlingsinformasjonAnnenForelder =
            BehandlingsinformasjonAnnenForelder(
                identForelder = identAnnenForeldre1,
                finnesIkkeFerdigstiltBehandling = false,
                iverksattBehandling =
                    BehandlingsinformasjonAnnenForelder.IverksattBehandlingForelder(
                        barn = mapBarnIdTilIdent,
                        vedtak = vedtak,
                    ),
            )
        val resultat =
            mapBarnAndreForeldreSaksinformasjon(
                behandlingId = behandlingId,
                barnAnnenForelder = barnAnnenForelder,
                behandlingsinformasjonAnnenForelder = listOf(behandlingsinformasjonAnnenForelder),
            )
        assertThat(resultat).hasSize(1)
        assertThat(resultat.single().typeId).isEqualTo(identBarn2)
        with(resultat.single().data) {
            assertThat(this.andreForeldre).hasSize(1)
            assertThat(this.andreForeldre.single().harBehandlingUnderArbeid).isFalse()
            assertThat(this.andreForeldre.single().vedtaksperioderBarn).isEmpty()
        }
    }

    @Test
    fun `skal returnere tom liste med andreForeldre hvis man ikke har andre foreldre`() {
        val barnAnnenForelder =
            mapOf(
                identBarn1 to emptyList<String>(),
            )
        val resultat =
            mapBarnAndreForeldreSaksinformasjon(
                behandlingId = behandlingId,
                barnAnnenForelder = barnAnnenForelder,
                behandlingsinformasjonAnnenForelder = emptyList(),
            )
        assertThat(resultat).hasSize(1)
        assertThat(resultat.single().data.andreForeldre).isEmpty()
    }

    private fun lagVedtak(): InnvilgelseTilsynBarn {
        val vedtaksperioder =
            listOf(
                lagVedtaksperiodeGrunnlag(fom = LocalDate.of(2025, 1, 1), tom = LocalDate.of(2025, 1, 1)),
                lagVedtaksperiodeGrunnlag(fom = LocalDate.of(2025, 1, 2), tom = LocalDate.of(2025, 1, 3)),
                lagVedtaksperiodeGrunnlag(fom = LocalDate.of(2025, 1, 3), tom = LocalDate.of(2025, 1, 4)),
            )
        val beregningsgrunnlag =
            beregningsgrunnlag(
                vedtaksperioder = vedtaksperioder,
                utgifter = listOf(UtgiftBarn(barnId = barnId1, 100)),
            )
        val beregningsresultat = beregningsresultatForMåned(grunnlag = beregningsgrunnlag)
        return InnvilgelseTilsynBarn(
            beregningsresultat = BeregningsresultatTilsynBarn(listOf(beregningsresultat)),
            vedtaksperioder = emptyList(),
        )
    }

    private fun lagVedtaksperiodeGrunnlag(
        fom: LocalDate,
        tom: LocalDate,
    ) = vedtaksperiodeGrunnlag(vedtaksperiodeBeregning(fom = fom, tom = tom))
}
