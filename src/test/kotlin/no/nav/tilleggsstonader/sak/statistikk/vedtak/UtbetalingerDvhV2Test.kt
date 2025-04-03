package no.nav.tilleggsstonader.sak.statistikk.vedtak

import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.AndelstypeDvh
import no.nav.tilleggsstonader.sak.statistikk.vedtak.domene.UtbetalingerDvhV2
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.defaultVedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.vedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.innvilgelse as innvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.innvilgelse as innvilgelseLæremidler

class UtbetalingerDvhV2Test {
    @Test
    fun `fraDomene mapper til UtbetalingerDvh for tillsynBarn ikke makssats`() {
        val innvilgelse =
            innvilgelseTilsynBarn(
                InnvilgelseTilsynBarn(
                    vedtaksperioder = listOf(defaultVedtaksperiode),
                    beregningsresultat =
                        BeregningsresultatTilsynBarn(
                            perioder =
                                listOf(
                                    beregningsresultatForMåned(
                                        vedtaksperioder = listOf(vedtaksperiodeGrunnlag()),
                                        utgifterTotal = 4000,
                                    ),
                                ),
                        ),
                ),
            )
        val andlelerTilkjentYtelse = listOf(andelTilkjentYtelse(fom = LocalDate.of(2024, 1, 1), beløp = 1000))

        val resultat = UtbetalingerDvhV2.fraDomene(andlelerTilkjentYtelse, innvilgelse)

        val forventetResultat =
            UtbetalingerDvhV2.JsonWrapper(
                utbetalinger =
                    listOf(
                        UtbetalingerDvhV2(
                            fraOgMed = LocalDate.of(2024, 1, 1),
                            tilOgMed = LocalDate.of(2024, 1, 1),
                            type = AndelstypeDvh.TILSYN_BARN_AAP,
                            beløp = 1000,
                            makssats = 3000,
                            beløpErBegrensetAvMakssats = false,
                        ),
                    ),
            )

        assertThat(resultat).isEqualTo(forventetResultat)
    }

    @Test
    fun `fraDomene mapper til UtbetalingerDvh for tillsynBarn makssats`() {
        val innvilgelse = innvilgelseTilsynBarn()

        val andlelerTilkjentYtelse = listOf(andelTilkjentYtelse(fom = LocalDate.of(2024, 1, 1)))

        val resultat = UtbetalingerDvhV2.fraDomene(andlelerTilkjentYtelse, innvilgelse)

        val forventetResultat =
            UtbetalingerDvhV2.JsonWrapper(
                utbetalinger =
                    listOf(
                        UtbetalingerDvhV2(
                            fraOgMed = LocalDate.of(2024, 1, 1),
                            tilOgMed = LocalDate.of(2024, 1, 1),
                            type = AndelstypeDvh.TILSYN_BARN_AAP,
                            beløp = 11554,
                            makssats = 3000,
                            beløpErBegrensetAvMakssats = true,
                        ),
                    ),
            )

        assertThat(resultat).isEqualTo(forventetResultat)
    }

    @Test
    fun `fraDomene mapper til UtebetalingerDvh for læremidler`() {
        val innvilgelse = innvilgelseLæremidler()
        val andlelerTilkjentYtelse = listOf(andelTilkjentYtelse(fom = LocalDate.of(2024, 1, 1)))

        val resultat = UtbetalingerDvhV2.fraDomene(andlelerTilkjentYtelse, innvilgelse)

        val forventetResultat =
            UtbetalingerDvhV2.JsonWrapper(
                utbetalinger =
                    listOf(
                        UtbetalingerDvhV2(
                            fraOgMed = LocalDate.of(2024, 1, 1),
                            tilOgMed = LocalDate.of(2024, 1, 1),
                            type = AndelstypeDvh.TILSYN_BARN_AAP,
                            beløp = 11554,
                        ),
                    ),
            )

        assertThat(resultat).isEqualTo(forventetResultat)
    }
}
