package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.util.UUID

class VedtaksperiodeServiceTest {
    @Nested
    inner class DetFinnesVedtaksperioder {
        val vedtakRepository = mockk<VedtakRepository>()
        val vilkårperiodeService = mockk<VilkårperiodeService>()
        val vilkårService = mockk<VilkårService>()
        val behandlingService = mockk<BehandlingService>()

        val vedtaksperiodeService =
            VedtaksperiodeService(
                vilkårperiodeService = vilkårperiodeService,
                vilkårService = vilkårService,
                vedtakRepository = vedtakRepository,
                behandlingService = behandlingService,
            )

        val behandlingId = BehandlingId.random()
        val saksbehandling = saksbehandling(id = behandlingId)

        val vedtakFørstegangsbehandlingMedVedtaksperioder =
            innvilgetVedtak(
                vedtaksperioder =
                    listOf(
                        Vedtaksperiode(
                            id = UUID.randomUUID(),
                            fom = LocalDate.of(2024, 1, 1),
                            tom = LocalDate.of(2024, 2, 1),
                            målgruppe = MålgruppeType.AAP,
                            aktivitet = AktivitetType.TILTAK,
                        ),
                    ),
            )

        @Test
        fun `skal retunere false hvis det ikke finnes tidligere vedtaksperioder`() {
            every { vedtakRepository.findByIdOrNull(behandlingId) } returns null
            assertThat(
                vedtaksperiodeService.detFinnesVedtaksperioder(
                    behandlingId = behandlingId,
                    forrigeBehandlingId = null,
                ),
            ).isFalse
        }

        @Test
        fun `skal retunere true hvis det finnes vedtaksperioder på dette vedtaket`() {
            every { vedtakRepository.findByIdOrNull(behandlingId) } returns vedtakFørstegangsbehandlingMedVedtaksperioder
            assertThat(
                vedtaksperiodeService.detFinnesVedtaksperioder(
                    behandlingId = behandlingId,
                    forrigeBehandlingId = null,
                ),
            ).isTrue
        }

        @Test
        fun `skal retunere true hvis det ikke finnes vedtaksperioder på dette vedtaket, men på forrgie vedtak`() {
            val denneBehandlingen = BehandlingId.random()
            every { vedtakRepository.findByIdOrNull(behandlingId) } returns vedtakFørstegangsbehandlingMedVedtaksperioder
            every { vedtakRepository.findByIdOrNull(denneBehandlingen) } returns null
            assertThat(
                vedtaksperiodeService.detFinnesVedtaksperioder(
                    behandlingId = denneBehandlingen,
                    forrigeBehandlingId = behandlingId,
                ),
            ).isTrue
        }
    }
}
