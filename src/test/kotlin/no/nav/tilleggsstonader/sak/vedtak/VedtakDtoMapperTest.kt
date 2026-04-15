package no.nav.tilleggsstonader.sak.vedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.avslagVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.opphørVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.AvslagTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.DagligReiseTestUtil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.OpphørDagligReiseResponse
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerResponse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.DagligReiseVilkårService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakDtoMapperTest {
    val vedtakService: VedtakService = mockk()
    val dagligReiseVilkårService: DagligReiseVilkårService = mockk(relaxed = true)
    val vedtakDtoMapper = VedtakDtoMapper(vedtakService, dagligReiseVilkårService)

    @Nested
    inner class TilsynBarn {
        @Test
        fun `skal mappe innvilget vedtak til dto`() {
            val vedtak = innvilgetVedtak()

            val dto = vedtakDtoMapper.toDto(vedtak, forrigeIverksatteBehandlingId = null)

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

            every { vedtakService.hentVedtaksperioder(tidligereInnvilgetVedtak.behandlingId) } returns
                tidligereInnvilgetVedtak.data.vedtaksperioder

            val dto =
                vedtakDtoMapper.toDto(
                    vedtak,
                    forrigeIverksatteBehandlingId = tidligereInnvilgetVedtak.behandlingId,
                )

            assertThat(dto).isInstanceOf(InnvilgelseTilsynBarnResponse::class.java)

            val vedtakResponse = dto as InnvilgelseTilsynBarnResponse
            assertThat(vedtakResponse.vedtaksperioder).hasSize(1)

            val vedtaksperiodeIRespons = vedtakResponse.vedtaksperioder!!.single()
            assertThat(
                vedtaksperiodeIRespons.vedtaksperiodeFraForrigeVedtak,
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

            val dto = vedtakDtoMapper.toDto(vedtak, forrigeIverksatteBehandlingId = null) as AvslagTilsynBarnDto

            assertThat(dto.begrunnelse).isEqualTo(vedtak.data.begrunnelse)
            assertThat(dto.type).isEqualTo(vedtak.type)
        }

        @Test
        fun `skal mappe opphørt vedtak til dto`() {
            val opphørsdato = LocalDate.of(2024, 1, 15)
            val vedtak =
                opphørVedtak(
                    årsaker = listOf(ÅrsakOpphør.ENDRING_UTGIFTER),
                    begrunnelse = "begrunnelse",
                    opphørsdato = opphørsdato,
                )

            val dto = vedtakDtoMapper.toDto(vedtak, forrigeIverksatteBehandlingId = null) as OpphørTilsynBarnResponse

            assertThat(dto.årsakerOpphør).isEqualTo(vedtak.data.årsaker)
            assertThat(dto.begrunnelse).isEqualTo(vedtak.data.begrunnelse)
            assertThat(dto.opphørsdato).isEqualTo(opphørsdato)
            assertThat(dto.type).isEqualTo(TypeVedtak.OPPHØR)
        }
    }

    @Nested
    inner class Læremidler {
        val innvilgelse = LæremidlerTestUtil.innvilgelse()

        @Test
        fun `skal mappe innvilget vedtak til dto`() {
            val dto = vedtakDtoMapper.toDto(innvilgelse, forrigeIverksatteBehandlingId = null)

            assertThat(dto).isInstanceOf(InnvilgelseLæremidlerResponse::class.java)

            val innvilgetDto = dto as InnvilgelseLæremidlerResponse
            assertThat(innvilgetDto.gjelderFraOgMed).isEqualTo(LocalDate.of(2024, 1, 1))
            assertThat(innvilgetDto.gjelderTilOgMed).isEqualTo(LocalDate.of(2024, 1, 7))
        }

        @Test
        fun `skal mappe revurdert innvilget vedtak til dto`() {
            val dto =
                vedtakDtoMapper.toDto(
                    innvilgelse.copy(tidligsteEndring = LocalDate.of(2024, 1, 3)),
                    forrigeIverksatteBehandlingId = null,
                )

            val innvilgetDto = dto as InnvilgelseLæremidlerResponse
            assertThat(innvilgetDto.gjelderFraOgMed).isEqualTo(LocalDate.of(2024, 1, 3))
            assertThat(innvilgetDto.gjelderTilOgMed).isEqualTo(LocalDate.of(2024, 1, 7))
        }
    }

    @Nested
    inner class DagligReise {
        @Test
        fun `skal mappe opphørt vedtak til dto`() {
            val opphørsdato = LocalDate.of(2024, 1, 15)
            val vedtak =
                GeneriskVedtak(
                    behandlingId = BehandlingId.random(),
                    type = TypeVedtak.OPPHØR,
                    data =
                        OpphørDagligReise(
                            vedtaksperioder = DagligReiseTestUtil.defaultVedtaksperioder,
                            beregningsresultat = DagligReiseTestUtil.defaultBeregningsresultat,
                            rammevedtakPrivatBil = null,
                            årsaker = listOf(ÅrsakOpphør.ANNET),
                            begrunnelse = "begrunnelse",
                            beregningsplan = Beregningsplan(Beregningsomfang.FRA_DATO, opphørsdato),
                        ),
                    gitVersjon = Applikasjonsversjon.versjon,
                    tidligsteEndring = null,
                    opphørsdato = opphørsdato,
                )

            val dto = vedtakDtoMapper.toDto(vedtak, forrigeIverksatteBehandlingId = null) as OpphørDagligReiseResponse

            assertThat(dto.årsakerOpphør).isEqualTo(vedtak.data.årsaker)
            assertThat(dto.begrunnelse).isEqualTo(vedtak.data.begrunnelse)
            assertThat(dto.opphørsdato).isEqualTo(opphørsdato)
            assertThat(dto.type).isEqualTo(TypeVedtak.OPPHØR)
        }
    }
}
