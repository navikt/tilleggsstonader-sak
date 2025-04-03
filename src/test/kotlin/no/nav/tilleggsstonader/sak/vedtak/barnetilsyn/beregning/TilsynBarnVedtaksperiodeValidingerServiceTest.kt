package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.beregning

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Fødsel
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlag
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Grunnlagsdata
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.GrunnlagsdataService
import no.nav.tilleggsstonader.sak.opplysninger.grunnlag.Navn
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.VedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingAktivitetTilsynBarn
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class TilsynBarnVedtaksperiodeValidingerServiceTest {
    val vilkårperiodeService = mockk<VilkårperiodeService>()
    val grunnlagsdataService = mockk<GrunnlagsdataService>()
    val vedtakRepository = mockk<VedtakRepository>()
    val tilsynBarnVedtaksperiodeValidingerService =
        TilsynBarnVedtaksperiodeValidingerService(
            vilkårperiodeService = vilkårperiodeService,
            vedtakRepository = vedtakRepository,
        )

    val behandling = saksbehandling()

    val målgrupper =
        listOf(
            målgruppe(
                faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.AAP),
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 2, 28),
            ),
        )
    val aktiviteter =
        listOf(
            aktivitet(
                faktaOgVurdering = faktaOgVurderingAktivitetTilsynBarn(type = AktivitetType.TILTAK),
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 2, 28),
            ),
        )

    val utgifter: Map<BarnId, List<UtgiftBeregning>> =
        mapOf(
            BarnId.random() to
                listOf(
                    UtgiftBeregning(
                        fom = YearMonth.of(2025, 1),
                        tom = YearMonth.of(2025, 2),
                        utgift = 1000,
                    ),
                ),
        )

    @BeforeEach
    fun setup() {
        every { vilkårperiodeService.hentVilkårperioder(any()) } returns
            Vilkårperioder(
                målgrupper = målgrupper,
                aktiviteter = aktiviteter,
            )
        every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns lagGrunnlagsdata()
    }

    @Test
    fun `skal ikke kaste feil for gyldig vedtaksperiode`() {
        val vedtaksperiode = lagVedtaksperiode()

        assertThatCode {
            tilsynBarnVedtaksperiodeValidingerService.validerVedtaksperioder(
                vedtaksperioder = listOf(vedtaksperiode),
                behandling = behandling,
                utgifter = utgifter,
                typeVedtak = TypeVedtak.INNVILGELSE,
            )
        }.doesNotThrowAnyException()
    }

    private fun lagGrunnlagsdata(fødeslsdato: LocalDate = LocalDate.of(1990, 1, 1)) =
        Grunnlagsdata(
            behandlingId = behandling.id,
            grunnlag =
                Grunnlag(
                    navn = Navn("fornavn", "mellomnavn", "etternavn"),
                    fødsel = Fødsel(fødeslsdato, fødeslsdato.year),
                    barn = emptyList(),
                    arena = null,
                ),
        )

    private fun lagVedtaksperiode(
        fom: LocalDate = LocalDate.of(2025, 1, 1),
        tom: LocalDate = LocalDate.of(2025, 1, 31),
        målgruppe: MålgruppeType = MålgruppeType.AAP,
        aktivitet: AktivitetType = AktivitetType.TILTAK,
    ) = Vedtaksperiode(
        id = UUID.randomUUID(),
        fom = fom,
        tom = tom,
        målgruppe = målgruppe,
        aktivitet = aktivitet,
    )
}
