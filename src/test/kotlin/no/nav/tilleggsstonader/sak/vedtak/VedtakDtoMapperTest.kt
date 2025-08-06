package no.nav.tilleggsstonader.sak.vedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.avslagVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeStatus
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakDtoMapperTest {
    val vedtaksperiodeService: VedtaksperiodeService = mockk()
    val vedtakDtoMapper = VedtakDtoMapper(vedtaksperiodeService)

    @Nested
    inner class TilsynBarn {
        @Test
        fun `skal mappe innvilget vedtak til dto`() {
            val vedtak = innvilgetVedtak()

            val dto = vedtakDtoMapper.toDto(vedtak, revurderFra = null, forrigeIverksatteBehandlingId = null)

            assertThat(dto).isInstanceOf(InnvilgelseTilsynBarnResponse::class.java)
        }

        @Test
        fun `skal mappe innvilget vedtak til dto med riktig statuser`() {
            val vedtaksperiode = vedtaksperiode()
            val tidligereInnvilgetVedtak =
                innvilgetVedtak(
                    vedtaksperioder = listOf(vedtaksperiode),
                ).copy(behandlingId = BehandlingId.random())

            val vedtak = innvilgetVedtak(vedtaksperioder = listOf(vedtaksperiode))

            every { vedtaksperiodeService.finnVedtaksperioderForBehandling(tidligereInnvilgetVedtak.behandlingId, any()) } returns
                tidligereInnvilgetVedtak.data.vedtaksperioder

            val dto =
                vedtakDtoMapper.toDto(
                    vedtak,
                    revurderFra = null,
                    forrigeIverksatteBehandlingId = tidligereInnvilgetVedtak.behandlingId,
                )

            assertThat(dto).isInstanceOf(InnvilgelseTilsynBarnResponse::class.java)

            val vedtakResponse = dto as InnvilgelseTilsynBarnResponse
            assertThat(vedtakResponse.vedtaksperioder).hasSize(1)

            val vedtaksperiodeIRespons = vedtakResponse.vedtaksperioder!!.single()
            assertThat(
                vedtaksperiodeIRespons.forrigeVedtaksperiode,
            ).isEqualTo(
                tidligereInnvilgetVedtak.data.vedtaksperioder
                    .single()
                    .tilDto(),
            )
        }

        @Test
        fun `skal mappe avslått vedtak til dto`() {
            val vedtak =
                avslagVedtak(
                    behandlingId = BehandlingId.random(),
                    årsaker = listOf(ÅrsakAvslag.INGEN_AKTIVITET),
                    begrunnelse = "begrunnelse",
                )

            val dto = vedtakDtoMapper.toDto(vedtak, revurderFra = null, forrigeIverksatteBehandlingId = null) as AvslagTilsynBarnDto

            assertThat(dto.begrunnelse).isEqualTo(vedtak.data.begrunnelse)
            assertThat(dto.type).isEqualTo(vedtak.type)
        }
    }

    @Nested
    inner class Læremidler {
        val innvilgelse = LæremidlerTestUtil.innvilgelse()

        @Test
        fun `skal mappe innvilget vedtak til dto`() {
            val dto = vedtakDtoMapper.toDto(innvilgelse, revurderFra = null, forrigeIverksatteBehandlingId = null)

            assertThat(dto).isInstanceOf(InnvilgelseLæremidlerResponse::class.java)

            val innvilgetDto = dto as InnvilgelseLæremidlerResponse
            assertThat(innvilgetDto.gjelderFraOgMed).isEqualTo(LocalDate.of(2024, 1, 1))
            assertThat(innvilgetDto.gjelderTilOgMed).isEqualTo(LocalDate.of(2024, 1, 7))
        }

        @Test
        fun `skal mappe revurdert innvilget vedtak til dto`() {
            val dto = vedtakDtoMapper.toDto(innvilgelse, revurderFra = LocalDate.of(2024, 1, 3), forrigeIverksatteBehandlingId = null)

            val innvilgetDto = dto as InnvilgelseLæremidlerResponse
            assertThat(innvilgetDto.gjelderFraOgMed).isEqualTo(LocalDate.of(2024, 1, 3))
            assertThat(innvilgetDto.gjelderTilOgMed).isEqualTo(LocalDate.of(2024, 1, 7))
        }
    }
}
