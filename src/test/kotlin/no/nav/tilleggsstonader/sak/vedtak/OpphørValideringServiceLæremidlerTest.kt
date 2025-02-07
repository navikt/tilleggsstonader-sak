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

    val februar = YearMonth.of(2025, 2)

    val førsteJanuar = YearMonth.of(2025, 1).atDay(1)
    val sisteJanuar = YearMonth.of(2025, 1).atEndOfMonth()

    val førsteFebruar = YearMonth.of(2025, 2).atDay(1)
    val sisteFebruar = YearMonth.of(2025, 2).atEndOfMonth()

    val vedtaksperiodeJanuar = Vedtaksperiode(førsteJanuar, sisteJanuar)
    val vedtaksperiodeFebruar = Vedtaksperiode(førsteFebruar, sisteFebruar)

    val førsteMars = YearMonth.of(2025, 3).atDay(1)

    val opphørValideringService = OpphørValideringService(vilkårperiodeService, vilkårService)

    val beregningsgrunnlag =
        Beregningsgrunnlag(
            fom = førsteJanuar,
            tom = sisteJanuar,
            utbetalingsdato = førsteJanuar,
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
                    fom = førsteFebruar,
                    tom = sisteFebruar,
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

    @Nested
    inner class `Valider beregningsresultat er avkortet ved opphør` {
        @Test
        fun `Kaster ikke feil når beregningsresultat i forrige behandling avkortes`() {
            assertThatCode {
                opphørValideringService.validerBeregningsresultatErAvkortetVedOpphør(
                    forrigeBeregningsresultat = beregningsresultat.perioder,
                    revurderFraDato = sisteFebruar,
                )
            }.doesNotThrowAnyException()
        }

        @Test
        fun `Kaster feil når nytt beregningsresultat ikke er avkortet`() {
            assertThatThrownBy {
                opphørValideringService.validerBeregningsresultatErAvkortetVedOpphør(
                    forrigeBeregningsresultat = beregningsresultat.perioder,
                    revurderFraDato = førsteMars,
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
                    forrigeBehandlingsVedtaksperioder = listOf(vedtaksperiodeJanuar, vedtaksperiodeFebruar),
                    revurderFraDato = sisteFebruar
                )
            }.doesNotThrowAnyException()
        }

        @Test
        fun `Kaster feil når vedtaksperioden ikke er avkortet`() {
            assertThatThrownBy {
                opphørValideringService.validerVedtaksperioderAvkortetVedOpphør(
                    forrigeBehandlingsVedtaksperioder = listOf(vedtaksperiodeJanuar, vedtaksperiodeFebruar),
                    revurderFraDato = førsteMars,
                )
            }.hasMessage("Opphør er et ugyldig vedtaksresultat fordi ingen vedtaksperioder har blitt avkortet")
        }
    }
}
