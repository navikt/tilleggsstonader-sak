package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtak
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

            assertThat(dto).isInstanceOf(InnvilgelseTilsynBarnDto::class.java)
        }

        @Test
        fun `skal mappe avslått vedtak til dto`() {
            val vedtak = Vedtak(
                behandlingId = BehandlingId.random(),
                type = TypeVedtak.AVSLAG,
                avslagBegrunnelse = "begrunnelse",
                årsakerAvslag = ÅrsakAvslag.Wrapper(årsaker = listOf(ÅrsakAvslag.INGEN_AKTIVITET)),
            )

            val dto = VedtakDtoMapper.toDto(vedtak, revurderFra = null) as AvslagTilsynBarnDto

            assertThat(dto.begrunnelse).isEqualTo(vedtak.avslagBegrunnelse)
            assertThat(dto.type).isEqualTo(vedtak.type)
        }
    }
}
