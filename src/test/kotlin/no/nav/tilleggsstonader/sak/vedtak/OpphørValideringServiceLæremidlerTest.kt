package no.nav.tilleggsstonader.sak.vedtak

import io.mockk.mockk
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.YearMonth

class OpphørValideringServiceLæremidlerTest {
    private val vilkårperiodeService = mockk<VilkårperiodeService>()
    private val vilkårService = mockk<VilkårService>()

    val opphørValideringService = OpphørValideringService(vilkårperiodeService, vilkårService)

    private val førsteJanuar = YearMonth.of(2025, 1).atDay(1)
    private val sisteJanuar = YearMonth.of(2025, 1).atEndOfMonth()

    private val førsteFebruar = YearMonth.of(2025, 2).atDay(1)
    private val sisteFebruar = YearMonth.of(2025, 2).atEndOfMonth()

    private val vedtaksperiodeJanuar = vedtaksperiode(fom = førsteJanuar, tom = sisteJanuar)
    private val vedtaksperiodeFebruar = vedtaksperiode(fom = førsteFebruar, tom = sisteFebruar)

    private val førsteMars = YearMonth.of(2025, 3).atDay(1)

    @Nested
    inner class `Valider at vedtaksperioden er avkortet ved opphør` {
        @Test
        fun `Kaster ikke feil når vedtaksperioden er avkortet`() {
            assertThatCode {
                opphørValideringService.validerVedtaksperioderAvkortetVedOpphørLæremidler(
                    forrigeBehandlingsVedtaksperioder = listOf(vedtaksperiodeJanuar, vedtaksperiodeFebruar),
                    opphørsdato = sisteFebruar,
                )
            }.doesNotThrowAnyException()
        }

        @Test
        fun `Kaster feil når vedtaksperioden ikke er avkortet`() {
            assertThatThrownBy {
                opphørValideringService.validerVedtaksperioderAvkortetVedOpphørLæremidler(
                    forrigeBehandlingsVedtaksperioder = listOf(vedtaksperiodeJanuar, vedtaksperiodeFebruar),
                    opphørsdato = førsteMars,
                )
            }.hasMessage(
                "Opphør er et ugyldig valg fordi ønsket opphørsdato ikke korter ned vedtaket.",
            )
        }
    }
}
