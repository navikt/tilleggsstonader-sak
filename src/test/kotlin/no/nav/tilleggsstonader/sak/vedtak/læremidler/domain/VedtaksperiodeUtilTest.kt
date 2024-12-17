package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.tilGrunnlagStønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeUtil.validerVedtaksperioder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class VedtaksperiodeUtilTest {

    var vedtaksperioder: List<Vedtaksperiode> = emptyList()
    var stønadsperioder: List<Stønadsperiode> = emptyList()
    var feil: Exception? = null

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
            ).tilGrunnlagStønadsperiode(),
        )
        feil = null
    }

    @Test
    fun `Kaster ikke feil ved gyldig data`() {
        try {
            validerVedtaksperioder(vedtaksperioder, stønadsperioder)
        } catch (e: Exception) {
            feil = e
        }

        assertThat(feil).isNull()
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

        var feil: Exception? = null

        try {
            validerVedtaksperioder(vedtaksperioder, stønadsperioder)
        } catch (e: Exception) {
            feil = e
        }

        assertThat(feil).hasMessageContaining("overlapper")
    }

    @Test
    fun `Vedtaksperiode ikke innenfor en stønadsperiode kaster feil`() {
        val behandlingId = BehandlingId(UUID.randomUUID())
        stønadsperioder = listOf(
            stønadsperiode(
                behandlingId = behandlingId,
                fom = LocalDate.of(2024, 1, 2),
                tom = LocalDate.of(2024, 1, 31),
            ).tilGrunnlagStønadsperiode(),
        )

        var feil: Exception? = null

        try {
            validerVedtaksperioder(vedtaksperioder, stønadsperioder)
        } catch (e: Exception) {
            feil = e
        }

        assertThat(feil).hasMessageContaining("Vedtaksperiode er ikke innenfor en stønadsperiode")
    }
}
