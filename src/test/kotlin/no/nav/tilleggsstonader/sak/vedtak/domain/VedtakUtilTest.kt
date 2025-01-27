package no.nav.tilleggsstonader.sak.vedtak.domain

import no.nav.tilleggsstonader.sak.vedtak.domain.VedtakUtil.withTypeOrThrow
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.avslag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.innvilgelse
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.opphør
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class VedtakUtilTest {

    @Nested
    inner class WithTypeOrThrow {

        @Test
        fun `innvilgelse er av type InnvilgelseEllerOpphørLæremidler`() {
            val innvilgelse = innvilgelse()

            innvilgelse.assertGyldigKombinasjon<InnvilgelseEllerOpphørLæremidler>()
            innvilgelse.assertGyldigKombinasjon<InnvilgelseLæremidler>()

            innvilgelse.assertIkkeGyldigKombinasjon<OpphørLæremidler>()
            innvilgelse.assertIkkeGyldigKombinasjon<AvslagLæremidler>()
        }

        @Test
        fun `opphør er av type InnvilgelseEllerOpphørLæremidler`() {
            val opphør = opphør()

            opphør.assertGyldigKombinasjon<InnvilgelseEllerOpphørLæremidler>()
            opphør.assertGyldigKombinasjon<OpphørLæremidler>()

            opphør.assertIkkeGyldigKombinasjon<InnvilgelseLæremidler>()
            opphør.assertIkkeGyldigKombinasjon<AvslagLæremidler>()
        }

        @Test
        fun `avslag er av type AvslagLæremidler`() {
            val avslag = avslag()
            avslag.assertGyldigKombinasjon<AvslagLæremidler>()

            avslag.assertIkkeGyldigKombinasjon<InnvilgelseEllerOpphørLæremidler>()
            avslag.assertIkkeGyldigKombinasjon<InnvilgelseLæremidler>()
            avslag.assertIkkeGyldigKombinasjon<OpphørLæremidler>()
        }

        private inline fun <reified T : VedtakLæremidler> GeneriskVedtak<out Vedtaksdata>.assertGyldigKombinasjon() {
            assertDoesNotThrow { this.withTypeOrThrow<T>() }
        }

        private inline fun <reified T : VedtakLæremidler> GeneriskVedtak<out Vedtaksdata>.assertIkkeGyldigKombinasjon() {
            assertThatThrownBy { this.withTypeOrThrow<T>() }
                .hasMessageContaining("Ugyldig data")
        }
    }
}
