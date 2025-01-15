package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain

import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.stønadsperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtaksperiodeTilsynBarnMapperTest {

    val periode1 = StønadsperiodeBeregningsgrunnlag(
        fom = LocalDate.of(2024, 1, 1),
        tom = LocalDate.of(2024, 1, 1),
        målgruppe = MålgruppeType.AAP,
        aktivitet = AktivitetType.TILTAK,
    )

    val periode2 = StønadsperiodeBeregningsgrunnlag(
        fom = LocalDate.of(2024, 1, 2),
        tom = LocalDate.of(2024, 1, 2),
        målgruppe = MålgruppeType.AAP,
        aktivitet = AktivitetType.TILTAK,
    )

    @Test
    fun `skal slå sammen perioder hvis de er like og sammenhengende`() {
        val vedtaksperioder = VedtaksperiodeTilsynBarnMapper.mapTilVedtaksperiode(listOf(beregningsresultat(periode1, periode2)))

        assertThat(vedtaksperioder).containsExactly(
            VedtaksperiodeTilsynBarn(
                fom = periode1.fom,
                tom = periode2.tom,
                målgruppe = periode1.målgruppe,
                aktivitet = periode1.aktivitet,
                antallBarn = 2,
            ),
        )
    }

    /**
     * I tilfelle man har stønadsperioder i ulike måneder får man en [BeregningsresultatForMåned] per måned
     */
    @Test
    fun `skal slå sammen perioder tvers ulike måneder`() {
        val periode2Tom = LocalDate.of(2024, 2, 10)
        val vedtaksperioder = VedtaksperiodeTilsynBarnMapper.mapTilVedtaksperiode(
            listOf(
                beregningsresultat(periode1.copy(fom = LocalDate.of(2024, 1, 1), tom = LocalDate.of(2024, 1, 31))),
                beregningsresultat(periode1.copy(fom = LocalDate.of(2024, 2, 1), tom = periode2Tom)),
            ),
        )

        assertThat(vedtaksperioder).containsExactly(
            VedtaksperiodeTilsynBarn(
                fom = periode1.fom,
                tom = periode2Tom,
                målgruppe = periode1.målgruppe,
                aktivitet = periode1.aktivitet,
                antallBarn = 2,
            ),
        )
    }

    @Test
    fun `skal ikke slå sammen perioder hvis de ikke har samme målgruppe`() {
        val vedtaksperioder = VedtaksperiodeTilsynBarnMapper.mapTilVedtaksperiode(
            listOf(
                beregningsresultat(
                    periode1,
                    periode2.copy(målgruppe = MålgruppeType.OVERGANGSSTØNAD),
                ),
            ),
        )

        assertThat(vedtaksperioder).hasSize(2)
    }

    @Test
    fun `skal ikke slå sammen perioder hvis de ikke har samme aktivitet`() {
        val vedtaksperioder = VedtaksperiodeTilsynBarnMapper.mapTilVedtaksperiode(
            listOf(
                beregningsresultat(
                    periode1,
                    periode2.copy(aktivitet = AktivitetType.UTDANNING),
                ),
            ),
        )

        assertThat(vedtaksperioder).hasSize(2)
    }

    @Test
    fun `skal ikke slå sammen perioder hvis de ikke er sammenhengende`() {
        val vedtaksperioder = VedtaksperiodeTilsynBarnMapper.mapTilVedtaksperiode(
            listOf(
                beregningsresultat(
                    periode1,
                    periode2.copy(fom = periode1.fom.plusDays(2), tom = periode1.fom.plusDays(2)),
                ),
            ),
        )

        assertThat(vedtaksperioder).hasSize(2)
    }

    private fun beregningsresultat(
        vararg stønadsperioder: StønadsperiodeBeregningsgrunnlag,
        antallBarn: Int = 2,
    ): BeregningsresultatForMåned {
        return beregningsresultatForMåned(
            stønadsperioder = stønadsperioder.map { stønadsperiodeGrunnlag(stønadsperiode = it) },
        ).let { it.copy(grunnlag = it.grunnlag.copy(antallBarn = antallBarn)) }
    }
}
