package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import java.time.LocalDate
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeLæremidlerMapper.VedtaksperiodeLæremidler
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class VedtaksperiodeLæremidlerMapperTest {

    val FØRSTE_JAN: LocalDate = LocalDate.of(2024, 1, 1)
    val ANDRE_JAN: LocalDate = LocalDate.of(2024, 1, 2)

    val beregningsresultatPeriode1 = beregningsresultatForMåned(
        fom = FØRSTE_JAN,
        tom = FØRSTE_JAN,
        utbetalingsdato = FØRSTE_JAN
    )

    val beregningsresultatPeriode2 = beregningsresultatForMåned(
        fom = ANDRE_JAN,
        tom = ANDRE_JAN,
        utbetalingsdato = ANDRE_JAN
    )

    @Test
    fun `skal slå sammen perioder hvis de er like og sammenhengende`() {
        val vedtaksperioder =
            VedtaksperiodeLæremidlerMapper.mapTilVedtaksperiode(
                beregningsresultatForMåned = listOf(
                    beregningsresultatPeriode1,
                    beregningsresultatPeriode2,
                )
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
        val vedtaksperioder = VedtaksperiodeLæremidlerMapper.mapTilVedtaksperiode(
            beregningsresultatForMåned = listOf(
                beregningsresultatPeriode1,
                beregningsresultatPeriode2.copy(grunnlag = beregningsresultatPeriode2.grunnlag.copy(målgruppe = MålgruppeType.OVERGANGSSTØNAD)),
            ),
        )

        assertThat(vedtaksperioder).hasSize(2)
    }

    @Test
    fun `skal ikke slå sammen perioder hvis de ikke har samme studienivå`() {
        val vedtaksperioder = VedtaksperiodeLæremidlerMapper.mapTilVedtaksperiode(
            beregningsresultatForMåned = listOf(
                beregningsresultatPeriode1,
                beregningsresultatPeriode2.copy(grunnlag = beregningsresultatPeriode2.grunnlag.copy(studienivå = Studienivå.VIDEREGÅENDE)),
            ),
        )

        assertThat(vedtaksperioder).hasSize(2)
    }

    @Test
    fun `skal ikke slå sammen perioder hvis de ikke er sammenhengende`() {
        val FJERDE_JAN: LocalDate = LocalDate.of(2024, 1, 4)
        val vedtaksperioder = VedtaksperiodeLæremidlerMapper.mapTilVedtaksperiode(
            beregningsresultatForMåned = listOf(
                beregningsresultatPeriode1,
                beregningsresultatPeriode2.copy(
                    grunnlag = beregningsresultatPeriode2.grunnlag.copy(
                        fom = FJERDE_JAN,
                        tom = FJERDE_JAN,
                    )
                ),
            ),
        )

        assertThat(vedtaksperioder).hasSize(2)
    }
}
