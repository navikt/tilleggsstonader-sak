package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeLæremidlerMapper.VedtaksperiodeLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtaksperiodeLæremidlerMapperTest {
    val førsteJanuar: LocalDate = LocalDate.of(2024, 1, 1)
    val sisteJanuar: LocalDate = LocalDate.of(2024, 1, 2)

    val beregningsresultatPeriode1 =
        beregningsresultatForMåned(
            fom = førsteJanuar,
            tom = førsteJanuar,
            utbetalingsdato = førsteJanuar,
        )

    val beregningsresultatPeriode2 =
        beregningsresultatForMåned(
            fom = sisteJanuar,
            tom = sisteJanuar,
            utbetalingsdato = sisteJanuar,
        )

    @Test
    fun `skal slå sammen perioder hvis de er like og sammenhengende`() {
        val vedtaksperioder =
            VedtaksperiodeLæremidlerMapper.mapTilVedtaksperiode(
                beregningsresultatForMåned =
                    listOf(
                        beregningsresultatPeriode1,
                        beregningsresultatPeriode2,
                    ),
            )

        assertThat(vedtaksperioder).containsExactly(
            VedtaksperiodeLæremidler(
                fom = beregningsresultatPeriode1.grunnlag.fom,
                tom = beregningsresultatPeriode2.grunnlag.tom,
                målgruppe = beregningsresultatPeriode1.grunnlag.målgruppe,
                studienivå = beregningsresultatPeriode1.grunnlag.studienivå,
            ),
        )
    }

    @Test
    fun `skal ikke slå sammen perioder hvis de ikke har samme målgruppe`() {
        val vedtaksperioder =
            VedtaksperiodeLæremidlerMapper.mapTilVedtaksperiode(
                beregningsresultatForMåned =
                    listOf(
                        beregningsresultatPeriode1,
                        beregningsresultatPeriode2.copy(
                            grunnlag = beregningsresultatPeriode2.grunnlag.copy(målgruppe = MålgruppeType.OVERGANGSSTØNAD),
                        ),
                    ),
            )

        assertThat(vedtaksperioder).hasSize(2)
    }

    @Test
    fun `skal ikke slå sammen perioder hvis de ikke har samme studienivå`() {
        val vedtaksperioder =
            VedtaksperiodeLæremidlerMapper.mapTilVedtaksperiode(
                beregningsresultatForMåned =
                    listOf(
                        beregningsresultatPeriode1,
                        beregningsresultatPeriode2.copy(
                            grunnlag = beregningsresultatPeriode2.grunnlag.copy(studienivå = Studienivå.VIDEREGÅENDE),
                        ),
                    ),
            )

        assertThat(vedtaksperioder).hasSize(2)
    }

    @Test
    fun `skal ikke slå sammen perioder hvis de ikke er sammenhengende`() {
        val fjerdeJanuar: LocalDate = LocalDate.of(2024, 1, 4)
        val vedtaksperioder =
            VedtaksperiodeLæremidlerMapper.mapTilVedtaksperiode(
                beregningsresultatForMåned =
                    listOf(
                        beregningsresultatPeriode1,
                        beregningsresultatPeriode2.copy(
                            grunnlag =
                                beregningsresultatPeriode2.grunnlag.copy(
                                    fom = fjerdeJanuar,
                                    tom = fjerdeJanuar,
                                ),
                        ),
                    ),
            )

        assertThat(vedtaksperioder).hasSize(2)
    }
}
