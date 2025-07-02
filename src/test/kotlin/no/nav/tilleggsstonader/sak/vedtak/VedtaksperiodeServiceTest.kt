package no.nav.tilleggsstonader.sak.vedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.innvilgelse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.util.UUID
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak as innvilgetVedtakTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode as VedtaksperiodeLæremidler

class VedtaksperiodeServiceTest {
    val vedtakRepository = mockk<VedtakRepository>()

    val vedtaksperiodeService =
        VedtaksperiodeService(vedtakRepository = vedtakRepository)

    val vedtaksperiode1 =
        Vedtaksperiode(
            id = UUID.randomUUID(),
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 1, 31),
            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
            aktivitet = AktivitetType.TILTAK,
        )
    val vedtaksperiode2 =
        Vedtaksperiode(
            id = UUID.randomUUID(),
            fom = LocalDate.of(2024, 2, 1),
            tom = LocalDate.of(2024, 2, 28),
            målgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER,
            aktivitet = AktivitetType.UTDANNING,
        )

    val behandlingId = BehandlingId.random()

    @Test
    fun `skal finne alle vedtaksperioder i en pass barn behandling`() {
        every { vedtakRepository.findByIdOrNull(behandlingId) } returns
            innvilgetVedtakTilsynBarn(
                behandlingId = behandlingId,
                vedtaksperioder = listOf(vedtaksperiode1, vedtaksperiode2),
            )

        val res =
            vedtaksperiodeService.finnVedtaksperioderForBehandling(behandlingId = behandlingId, revurdererFra = null)

        assertThat(res).isEqualTo(listOf(vedtaksperiode1, vedtaksperiode2))
    }

    @Test
    fun `skal finne alle vedtaksperioder i en læremidler behandling`() {
        val vedtaksperioderLæremidler =
            listOf(
                VedtaksperiodeLæremidler(
                    id = UUID.randomUUID(),
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 1, 31),
                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                    aktivitet = AktivitetType.TILTAK,
                ),
                VedtaksperiodeLæremidler(
                    id = UUID.randomUUID(),
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 1, 31),
                    målgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER,
                    aktivitet = AktivitetType.UTDANNING,
                ),
            )

        every { vedtakRepository.findByIdOrNull(behandlingId) } returns
            innvilgelse(
                behandlingId = behandlingId,
                vedtaksperioder = vedtaksperioderLæremidler,
            )

        val res =
            vedtaksperiodeService.finnVedtaksperioderForBehandling(behandlingId = behandlingId, revurdererFra = null)

        assertThat(res).isEqualTo(vedtaksperioderLæremidler.map { it.tilFellesDomeneVedtaksperiode() })
    }

    @Test
    fun `skal kun hente vedtaksperioder som er etter revurder fra`() {
        every { vedtakRepository.findByIdOrNull(behandlingId) } returns
            innvilgetVedtakTilsynBarn(
                behandlingId = behandlingId,
                vedtaksperioder = listOf(vedtaksperiode1, vedtaksperiode2),
            )

        val revurderFra = LocalDate.of(2024, 1, 14)
        val res =
            vedtaksperiodeService.finnVedtaksperioderForBehandling(
                behandlingId = behandlingId,
                revurdererFra = revurderFra,
            )

        assertThat(res).isEqualTo(listOf(vedtaksperiode1.copy(fom = revurderFra), vedtaksperiode2))
    }
}
