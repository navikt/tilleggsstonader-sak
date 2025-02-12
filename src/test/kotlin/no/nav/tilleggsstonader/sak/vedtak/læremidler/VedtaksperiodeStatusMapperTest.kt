package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtaksperiodeStatusMapperTest {
    val førsteJan: LocalDate = LocalDate.of(2024, 1, 1)
    val trettiførsteJan: LocalDate = LocalDate.of(2024, 1, 31)
    val dummyVedtaksperiode =
        Vedtaksperiode(
            fom = førsteJan,
            tom = trettiførsteJan,
        )

    @Test
    fun `vedtaksperiodeStatus blir NY i førstegangsbehandling`() {
        val vedtaksperioder = listOf(dummyVedtaksperiode)
        val vedtaksperioderMedOppdatertStatus =
            VedtaksperiodeStatusMapper.settStatusPåVedtaksperioder(
                vedtaksperioder = vedtaksperioder,
                vedtaksperioderForrigeBehandling = null,
            )
        assertThat(vedtaksperioderMedOppdatertStatus.single().status).isEqualTo(VedtaksperiodeStatus.NY)
    }

    @Test
    fun `settVedtaksperiodeStatus returnerer tom liste hvis input er tomme lister`() {
        val vedtaksperioderMedOppdatertStatus =
            VedtaksperiodeStatusMapper.settStatusPåVedtaksperioder(
                vedtaksperioder = emptyList(),
                vedtaksperioderForrigeBehandling = emptyList(),
            )
        assertThat(vedtaksperioderMedOppdatertStatus.isEmpty())
    }

    @Test
    fun `vedtaksperiodeStatus blir ENDRET hvis fra-og-med-datoen har blitt endret på i revurderingen`() {
        val vedtaksperioderMedOppdatertStatus =
            VedtaksperiodeStatusMapper.settStatusPåVedtaksperioder(
                vedtaksperioder = listOf(dummyVedtaksperiode),
                vedtaksperioderForrigeBehandling =
                    listOf(dummyVedtaksperiode.copy(fom = førsteJan.plusDays(1))),
            )
        assertThat(vedtaksperioderMedOppdatertStatus.single().status).isEqualTo(VedtaksperiodeStatus.ENDRET)
    }

    @Test
    fun `vedtaksperiodeStatus blir ENDRET hvis til-og-med-datoen har blitt endret på i revurderingen`() {
        val vedtaksperioderMedOppdatertStatus =
            VedtaksperiodeStatusMapper.settStatusPåVedtaksperioder(
                vedtaksperioder = listOf(dummyVedtaksperiode),
                vedtaksperioderForrigeBehandling =
                    listOf(dummyVedtaksperiode.copy(tom = trettiførsteJan.plusMonths(1))),
            )
        assertThat(vedtaksperioderMedOppdatertStatus.single().status).isEqualTo(VedtaksperiodeStatus.ENDRET)
    }

    @Test
    fun `vedtaksperiodeStatus blir UENDRET hvis vedtaksperiodene ikke ahr blitt endret på i revurderingen`() {
        val vedtaksperioderMedOppdatertStatus =
            VedtaksperiodeStatusMapper.settStatusPåVedtaksperioder(
                vedtaksperioder = listOf(dummyVedtaksperiode),
                vedtaksperioderForrigeBehandling = listOf(dummyVedtaksperiode),
            )

        assertThat(vedtaksperioderMedOppdatertStatus.single().status).isEqualTo(VedtaksperiodeStatus.UENDRET)
    }
}
