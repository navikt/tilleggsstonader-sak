package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.tilStønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeUtil.validerVedtaksperioder
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.util.UUID

class VedtaksperiodeUtilTest {
    val behandlingId = BehandlingId(UUID.randomUUID())
    val vedtaksperiodeJanuar =
        Vedtaksperiode(
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 1, 31),
        )
    val vedtaksperiodeFebruar =
        Vedtaksperiode(
            fom = LocalDate.of(2024, 2, 1),
            tom = LocalDate.of(2024, 2, 28),
        )

    val stønadsperiodeJanTilFeb =
        stønadsperiode(
            behandlingId = behandlingId,
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 2, 28),
        ).tilStønadsperiodeBeregningsgrunnlag()

    @Nested
    inner class ValiderVedtaksperioder {
        @Test
        fun `Kaster ikke feil ved gyldig data`() {
            val vedtaksperioder = listOf(vedtaksperiodeJanuar, vedtaksperiodeFebruar)
            val stønadsperioder = listOf(stønadsperiodeJanTilFeb)

            assertDoesNotThrow {
                validerVedtaksperioder(vedtaksperioder, stønadsperioder)
            }
        }

        @Test
        fun `Overlappende vedtaksperioder kaster feil`() {
            val vedtaksperioder =
                listOf(
                    vedtaksperiodeJanuar,
                    vedtaksperiodeFebruar.copy(fom = LocalDate.of(2024, 1, 31)),
                )
            val stønadsperioder = listOf(stønadsperiodeJanTilFeb)

            assertThatThrownBy {
                validerVedtaksperioder(vedtaksperioder, stønadsperioder)
            }.hasMessageContaining("overlapper")
        }

        @Test
        fun `Flere vedtaksperioder i samme kalendermåned men forskjellig løpende måned`() {
            val vedtaksperioder =
                listOf(
                    Vedtaksperiode(
                        fom = LocalDate.of(2024, 1, 15),
                        tom = LocalDate.of(2024, 2, 14),
                    ),
                    Vedtaksperiode(
                        fom = LocalDate.of(2024, 2, 15),
                        tom = LocalDate.of(2024, 2, 28),
                    ),
                )
            val stønadsperioder = listOf(stønadsperiodeJanTilFeb)

            assertDoesNotThrow {
                validerVedtaksperioder(vedtaksperioder, stønadsperioder)
            }
        }

        @Test
        fun `Vedtaksperiode ikke innenfor en stønadsperiode kaster feil`() {
            val behandlingId = BehandlingId(UUID.randomUUID())
            val vedtaksperioder = listOf(vedtaksperiodeJanuar, vedtaksperiodeFebruar)
            val stønadsperioder =
                listOf(
                    stønadsperiode(
                        behandlingId = behandlingId,
                        fom = LocalDate.of(2024, 1, 2),
                        tom = LocalDate.of(2024, 1, 31),
                    ).tilStønadsperiodeBeregningsgrunnlag(),
                )

            assertThatThrownBy {
                validerVedtaksperioder(vedtaksperioder, stønadsperioder)
            }.hasMessageContaining("Vedtaksperiode er ikke innenfor en overlappsperiode")
        }
    }
}
