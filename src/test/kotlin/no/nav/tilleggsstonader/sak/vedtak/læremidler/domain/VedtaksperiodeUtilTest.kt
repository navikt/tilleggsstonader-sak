package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.tilStønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeUtil.validerVedtaksperioder
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.util.UUID

class VedtaksperiodeUtilTest {

    var vedtaksperioder: List<Vedtaksperiode> = emptyList()
    var stønadsperioder: List<StønadsperiodeBeregningsgrunnlag> = emptyList()

    @BeforeEach
    fun `Set up`() {
        vedtaksperioder = listOf(
            Vedtaksperiode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 31),
            ),
            Vedtaksperiode(
                fom = LocalDate.of(2024, 2, 1),
                tom = LocalDate.of(2024, 2, 28),
            ),
        )

        val behandlingId = BehandlingId(UUID.randomUUID())
        stønadsperioder = listOf(
            stønadsperiode(
                behandlingId = behandlingId,
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 2, 28),
            ).tilStønadsperiodeBeregningsgrunnlag(),
        )
    }

    @Test
    fun `Kaster ikke feil ved gyldig data`() {
        assertDoesNotThrow {
            validerVedtaksperioder(vedtaksperioder, stønadsperioder)
        }
    }

    @Test
    fun `Overlappende vedtaksperioder kaster feil`() {
        vedtaksperioder = listOf(
            Vedtaksperiode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 31),
            ),
            Vedtaksperiode(
                fom = LocalDate.of(2024, 1, 30),
                tom = LocalDate.of(2024, 2, 28),
            ),
        )

        assertThatThrownBy {
            validerVedtaksperioder(vedtaksperioder, stønadsperioder)
        }.hasMessageContaining("Foreløbig støtter vi kun en vedtaksperiode per løpende måned")
    }

    @Test
    fun `Flere vedtaksperioder i samme løpende måned kaster feil`() {
        vedtaksperioder = listOf(
            Vedtaksperiode(
                fom = LocalDate.of(2024, 1, 12),
                tom = LocalDate.of(2024, 2, 5),
            ),
            Vedtaksperiode(
                fom = LocalDate.of(2024, 2, 6),
                tom = LocalDate.of(2024, 3, 12),
            ),
        )

        assertThatThrownBy {
            validerVedtaksperioder(vedtaksperioder, stønadsperioder)
        }.hasMessageContaining("Foreløbig støtter vi kun en vedtaksperiode per løpende måned")
    }

    @Test
    fun `Flere vedtaksperioder i samme kalendermåned men forskjellig løpende måned`() {
        vedtaksperioder = listOf(
            Vedtaksperiode(
                fom = LocalDate.of(2024, 1, 15),
                tom = LocalDate.of(2024, 2, 14),
            ),
            Vedtaksperiode(
                fom = LocalDate.of(2024, 2, 15),
                tom = LocalDate.of(2024, 2, 28),
            ),
        )

        assertDoesNotThrow {
            validerVedtaksperioder(vedtaksperioder, stønadsperioder)
        }
    }

    @Test
    fun `Vedtaksperiode ikke innenfor en stønadsperiode kaster feil`() {
        val behandlingId = BehandlingId(UUID.randomUUID())
        stønadsperioder = listOf(
            stønadsperiode(
                behandlingId = behandlingId,
                fom = LocalDate.of(2024, 1, 2),
                tom = LocalDate.of(2024, 1, 31),
            ).tilStønadsperiodeBeregningsgrunnlag(),
        )

        assertThatThrownBy {
            validerVedtaksperioder(vedtaksperioder, stønadsperioder)
        }.hasMessageContaining("Vedtaksperiode er ikke innenfor en stønadsperiode")
    }
}
