package no.nav.tilleggsstonader.sak.vedtak

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil.rammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.DagligReiseTestUtil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.InnvilgelseDagligReiseResponse
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.OpphørDagligReiseResponse
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.dto.tilDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.InnvilgelseLæremidlerResponse
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.PassAvBarnTestUtil.avslagVedtak
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.PassAvBarnTestUtil.innvilgetVedtak
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.PassAvBarnTestUtil.opphørVedtak
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto.AvslagPassAvBarnDto
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto.InnvilgelsePassAvBarnResponse
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto.OpphørPassAvBarnResponse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.DagligReiseVilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakDtoMapperTest {
    val vedtakService: VedtakService = mockk()
    val dagligReiseVilkårService: DagligReiseVilkårService = mockk(relaxed = true)
    val vedtakDtoMapper = VedtakDtoMapper(vedtakService, dagligReiseVilkårService)

    @Nested
    inner class PassAvBarn {
        @Test
        fun `skal mappe innvilget vedtak til dto`() {
            val vedtak = innvilgetVedtak()

            val dto = vedtakDtoMapper.toDto(vedtak, forrigeIverksatteBehandlingId = null)

            assertThat(dto).isInstanceOf(InnvilgelsePassAvBarnResponse::class.java)
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

            assertThat(dto).isInstanceOf(InnvilgelsePassAvBarnResponse::class.java)

            val vedtakResponse = dto as InnvilgelsePassAvBarnResponse
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

            val dto = vedtakDtoMapper.toDto(vedtak, forrigeIverksatteBehandlingId = null) as AvslagPassAvBarnDto

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

            val dto = vedtakDtoMapper.toDto(vedtak, forrigeIverksatteBehandlingId = null) as OpphørPassAvBarnResponse

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
        fun `skal kun flagge rammevedtak-reiser som er avsluttet før tidligste endring`() {
            val reiseFørEndring = rammeForReiseMedPrivatBil(reiseId = ReiseId.random(), fom = 1 januar 2026, tom = 31 januar 2026)
            val reiseSomDekkerTidligsteEndring =
                rammeForReiseMedPrivatBil(reiseId = ReiseId.random(), fom = 20 januar 2026, tom = 8 februar 2026)
            val reiseLikTidligsteEndring =
                rammeForReiseMedPrivatBil(reiseId = ReiseId.random(), fom = 1 februar 2026, tom = 28 februar 2026)
            val reiseEtterEndring =
                rammeForReiseMedPrivatBil(reiseId = ReiseId.random(), fom = 1 mars 2026, tom = 31 mars 2026)
            val vedtak =
                DagligReiseTestUtil.innvilgelse(
                    data =
                        DagligReiseTestUtil.defaultInnvilgelseDagligReise.copy(
                            rammevedtakPrivatBil =
                                RammevedtakPrivatBil(
                                    reiser =
                                        listOf(
                                            reiseFørEndring,
                                            reiseSomDekkerTidligsteEndring,
                                            reiseLikTidligsteEndring,
                                            reiseEtterEndring,
                                        ),
                                ),
                            beregningsplan =
                                Beregningsplan(
                                    omfang = Beregningsomfang.FRA_DATO,
                                    fraDato = 1 februar 2026,
                                    tidligsteEndring = 1 februar 2026,
                                ),
                        ),
                )

            val dto = vedtakDtoMapper.toDto(vedtak, forrigeIverksatteBehandlingId = null) as InnvilgelseDagligReiseResponse

            val reiser = dto.rammevedtakPrivatBil!!.reiser
            assertThat(reiser.map { it.reiseId }).containsExactlyInAnyOrder(
                reiseFørEndring.reiseId,
                reiseSomDekkerTidligsteEndring.reiseId,
                reiseLikTidligsteEndring.reiseId,
                reiseEtterEndring.reiseId,
            )
            assertThat(reiser.single { it.reiseId == reiseFørEndring.reiseId }.fraTidligereVedtak).isTrue()
            assertThat(reiser.single { it.reiseId == reiseSomDekkerTidligsteEndring.reiseId }.fraTidligereVedtak).isFalse()
            assertThat(reiser.single { it.reiseId == reiseLikTidligsteEndring.reiseId }.fraTidligereVedtak).isFalse()
            assertThat(reiser.single { it.reiseId == reiseEtterEndring.reiseId }.fraTidligereVedtak).isFalse()
        }

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
