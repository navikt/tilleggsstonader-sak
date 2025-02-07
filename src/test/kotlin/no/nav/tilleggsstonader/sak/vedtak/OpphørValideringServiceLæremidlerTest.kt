package no.nav.tilleggsstonader.sak.vedtak

import io.mockk.mockk
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth

class OpphørValideringServiceLæremidlerTest {
    private val vilkårperiodeService = mockk<VilkårperiodeService>()
    private val vilkårService = mockk<VilkårService>()

    val måned = YearMonth.of(2025, 1)
    val fom = måned.atDay(1)
    val tom = måned.atEndOfMonth()

    val vedtaksperiodeJanuar = Vedtaksperiode(fom, tom)
    val vedtaksperiodeFebruar = Vedtaksperiode(fom.plusMonths(1), tom.plusMonths(1))

    val opphørValideringService = OpphørValideringService(vilkårperiodeService, vilkårService)

    val beregningsgrunnlag =
        Beregningsgrunnlag(
            fom = fom,
            tom = tom,
            utbetalingsdato = fom,
            studienivå = Studienivå.HØYERE_UTDANNING,
            studieprosent = 100,
            sats = 100,
            satsBekreftet = true,
            målgruppe = MålgruppeType.AAP,
        )

    val beregningsresultatForJanuar =
        BeregningsresultatForMåned(
            beløp = 100,
            grunnlag = beregningsgrunnlag,
        )

    val beregningsresultatForFebruar =
        BeregningsresultatForMåned(
            beløp = 100,
            grunnlag =
                beregningsgrunnlag.copy(
                    fom = fom.plusMonths(1),
                    tom = tom.plusMonths(1),
                ),
        )

    val beregningsresultat =
        BeregningsresultatLæremidler(
            perioder =
                listOf(
                    beregningsresultatForJanuar,
                    beregningsresultatForFebruar,
                ),
        )

    val avkortetBeregningsresultat =
        BeregningsresultatLæremidler(
            perioder =
                listOf(
                    beregningsresultatForJanuar,
                ),
        )

    @Nested
    inner class `Valider beregningsresultat er avkortet ved opphør` {
        @Test
        fun `Kaster ikke feil når beregningsresultatet i forrige behandling avkortes`() {
            assertThatCode {
                opphørValideringService.validerBeregningsresultatErAvkortetVedOpphør(
                    beregningsresultatEtterOpphør = avkortetBeregningsresultat.perioder,
                    forrigeBeregningsresultat = beregningsresultat.perioder,
                )
            }.doesNotThrowAnyException()
        }

        @Test
        fun `Kaster feil når nytt beregeningsresultat ikke er avkortet`() {
            assertThatThrownBy {
                opphørValideringService.validerBeregningsresultatErAvkortetVedOpphør(
                    beregningsresultatEtterOpphør = beregningsresultat.perioder,
                    forrigeBeregningsresultat = beregningsresultat.perioder,
                )
            }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi ingen utbetalinger blir avkortet")
        }
    }

    @Nested
    inner class `Valider at vedtaksperioden er avkortet ved opphør` {
        @Test
        fun `Kaster ikke feil når vedtaksperioden er avkortet`() {
            assertThatCode {
                opphørValideringService.validerVedtaksperioderAvkortetVedOpphør(
                    vedtaksperioderEtterOpphør = listOf(vedtaksperiodeJanuar),
                    forrigeBehandlingsVedtaksperioder = listOf(vedtaksperiodeJanuar, vedtaksperiodeFebruar),
                )
            }.doesNotThrowAnyException()
        }

        @Test
        fun `Kaster feil når vedtaksperioden ikke er avkortet`() {
            assertThatThrownBy {
                opphørValideringService.validerVedtaksperioderAvkortetVedOpphør(
                    vedtaksperioderEtterOpphør = listOf(vedtaksperiodeJanuar, vedtaksperiodeFebruar),
                    forrigeBehandlingsVedtaksperioder = listOf(vedtaksperiodeJanuar, vedtaksperiodeFebruar),
                )
            }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi ingen vedtaksperioder har blitt avkortet")
        }
    }
}
