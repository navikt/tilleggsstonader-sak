package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.avslagVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakDtoMapperTest {
    @Nested
    inner class TilsynBarn {
        @Test
        fun `skal mappe innvilget vedtak til dto`() {
            val vedtak = innvilgetVedtak()

            val dto = VedtakDtoMapper.toDto(vedtak, revurderFra = null)

            assertThat(dto).isInstanceOf(InnvilgelseTilsynBarnResponse::class.java)
        }

        @Test
        fun `skal mappe avslått vedtak til dto`() {
            val vedtak =
                avslagVedtak(
                    behandlingId = BehandlingId.random(),
                    årsaker = listOf(ÅrsakAvslag.INGEN_AKTIVITET),
                    begrunnelse = "begrunnelse",
                )

            val dto = VedtakDtoMapper.toDto(vedtak, revurderFra = null) as AvslagTilsynBarnDto

            assertThat(dto.begrunnelse).isEqualTo(vedtak.data.begrunnelse)
            assertThat(dto.type).isEqualTo(vedtak.type)
        }
    }

    @Nested
    inner class Læremidler {
        val innvilgelse = LæremidlerTestUtil.innvilgelse()

        @Test
        fun `skal mappe innvilget vedtak til dto`() {
            val dto = VedtakDtoMapper.toDto(innvilgelse, revurderFra = null)

            assertThat(dto).isInstanceOf(InnvilgelseLæremidlerResponse::class.java)

            val innvilgetDto = dto as InnvilgelseLæremidlerResponse
            assertThat(innvilgetDto.gjelderFraOgMed).isEqualTo(LocalDate.of(2024, 1, 1))
            assertThat(innvilgetDto.gjelderTilOgMed).isEqualTo(LocalDate.of(2024, 1, 7))
        }

        @Test
        fun `skal mappe revurdert innvilget vedtak til dto`() {
            val dto = VedtakDtoMapper.toDto(innvilgelse, revurderFra = LocalDate.of(2024, 1, 3))

            val innvilgetDto = dto as InnvilgelseLæremidlerResponse
            assertThat(innvilgetDto.gjelderFraOgMed).isEqualTo(LocalDate.of(2024, 1, 3))
            assertThat(innvilgetDto.gjelderTilOgMed).isEqualTo(LocalDate.of(2024, 1, 7))
        }
    }
}
