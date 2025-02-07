package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtaksperiodeStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class VedtaksperiodeStatusMapperTest {
    @Test
    fun `vedtaksperiodeStatus blir NY hvis førstegangsbehandling`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val vedtaksperioder =
            listOf(
                Vedtaksperiode(
                    vedtaksperiodeId,
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 1, 31),
                    VedtaksperiodeStatus.NY,
                ),
            )
        val vedtaksperioderMedOppdatertStatus = VedtaksperiodeStatusMapper.settStatusPåVedtaksperioder(vedtaksperioder, null)
        assertThat(vedtaksperioderMedOppdatertStatus.single().status).isEqualTo(VedtaksperiodeStatus.NY)
    }

    @Test
    fun `vedtaksperiodeStatus blir ENDRET hvis vedtaksperioden har blitt endret i revurdering`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val forrigeVedtaksperioder =
            listOf(
                Vedtaksperiode(
                    vedtaksperiodeId,
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 1, 31),
                    VedtaksperiodeStatus.NY,
                ),
            )
        val endretVedtaksperioder =
            listOf(
                Vedtaksperiode(
                    vedtaksperiodeId,
                    LocalDate.of(2024, 1, 5),
                    LocalDate.of(2024, 1, 31),
                    VedtaksperiodeStatus.NY,
                ),
            )

        val vedtaksperioderMedOppdatertStatus =
            VedtaksperiodeStatusMapper.settStatusPåVedtaksperioder(
                endretVedtaksperioder,
                forrigeVedtaksperioder,
            )
        assertThat(vedtaksperioderMedOppdatertStatus.single().status).isEqualTo(VedtaksperiodeStatus.ENDRET)
    }

    @Test
    fun `vedtaksperiodeStatus blir UENDRET hvis vedtaksperioden har ikke blitt endret i revurdering`() {
        val vedtaksperiodeId = UUID.randomUUID()
        val forrigeVedtaksperioder =
            listOf(
                Vedtaksperiode(
                    vedtaksperiodeId,
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 1, 31),
                    VedtaksperiodeStatus.NY,
                ),
            )
        val endretVedtaksperioder =
            listOf(
                Vedtaksperiode(
                    vedtaksperiodeId,
                    LocalDate.of(2024, 1, 1),
                    LocalDate.of(2024, 1, 31),
                    VedtaksperiodeStatus.NY,
                ),
            )

        val vedtaksperioderMedOppdatertStatus =
            VedtaksperiodeStatusMapper.settStatusPåVedtaksperioder(
                endretVedtaksperioder,
                forrigeVedtaksperioder,
            )

        assertThat(vedtaksperioderMedOppdatertStatus.single().status).isEqualTo(VedtaksperiodeStatus.UENDRET)
    }
}
