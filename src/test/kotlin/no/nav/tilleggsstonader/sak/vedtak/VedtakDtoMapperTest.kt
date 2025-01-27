package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

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
                GeneriskVedtak(
                    behandlingId = BehandlingId.random(),
                    data =
                        AvslagTilsynBarn(
                            begrunnelse = "begrunnelse",
                            årsaker = listOf(ÅrsakAvslag.INGEN_AKTIVITET),
                        ),
                )

            val dto = VedtakDtoMapper.toDto(vedtak, revurderFra = null) as AvslagTilsynBarnDto

            assertThat(dto.begrunnelse).isEqualTo(vedtak.data.begrunnelse)
            assertThat(dto.type).isEqualTo(vedtak.type)
        }
    }
}
