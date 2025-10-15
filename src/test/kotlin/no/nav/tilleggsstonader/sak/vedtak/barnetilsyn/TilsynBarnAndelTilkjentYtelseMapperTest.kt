package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.november
import no.nav.tilleggsstonader.libs.utils.dato.oktober
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.vedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningTestUtil.vedtaksperiodeBeregning
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.YearMonth

class TilsynBarnAndelTilkjentYtelseMapperTest {
    @Test
    fun `finnPeriodeFraAndel finner riktig vedtaksperiode gitt andel tilkjent ytelse`() {
        val beregningsresultatForSeptember =
            beregningsresultatForMåned(
                måned = YearMonth.of(2025, 9),
                beløpsperioder =
                    listOf(
                        Beløpsperiode(
                            dato = 1 september 2025,
                            beløp = 1000,
                            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                        ),
                    ),
            )
        val beregningsresultatForOktober =
            beregningsresultatForMåned(
                måned = YearMonth.of(2025, 10),
                beløpsperioder =
                    listOf(
                        Beløpsperiode(
                            dato = 1 oktober 2025,
                            beløp = 1000,
                            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                        ),
                    ),
            )
        val beregningsresultat =
            BeregningsresultatTilsynBarn(perioder = listOf(beregningsresultatForSeptember, beregningsresultatForOktober))

        val andeler = beregningsresultat.mapTilAndelTilkjentYtelse(saksbehandling())
        assertThat(andeler).hasSize(2)

        andeler.forEachIndexed { index, andel ->
            val periodeFraAndel = finnPeriodeFraAndel(beregningsresultat, andel)
            val forventetMåned = beregningsresultat.perioder[index]
            assertThat(
                periodeFraAndel.fom,
            ).isEqualTo(
                forventetMåned.grunnlag.vedtaksperiodeGrunnlag
                    .single()
                    .vedtaksperiode.fom,
            )
            assertThat(
                periodeFraAndel.tom,
            ).isEqualTo(
                forventetMåned.grunnlag.vedtaksperiodeGrunnlag
                    .single()
                    .vedtaksperiode.tom,
            )
        }
    }

    @Test
    fun `finnPeriodeFraAndel med to beløpsperioder på samme dag og en beløpsperiode har beløp lik 0, filtrerer ut andel med beløp 0`() {
        val november = YearMonth.of(2024, 11)
        val desember = YearMonth.of(2024, 12)
        val beregningsresultatForNovember =
            beregningsresultatForMåned(
                måned = november,
                grunnlag =
                    beregningsgrunnlag(
                        november,
                        vedtaksperioder =
                            listOf(
                                vedtaksperiodeGrunnlag(
                                    vedtaksperiodeBeregning(fom = 1 november 2024, 29 november 2024),
                                ),
                                vedtaksperiodeGrunnlag(
                                    vedtaksperiodeBeregning(fom = 30 november 2024, 30 november 2024),
                                ),
                            ),
                    ),
                beløpsperioder =
                    listOf(
                        Beløpsperiode(
                            dato = 1 november 2024,
                            beløp = 1000,
                            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                        ),
                        // Utbetalingsdato blir 2. desember 2024 og beløp 0kr, pga 30 november er en lørdag
                        Beløpsperiode(
                            dato = 2 desember 2024,
                            beløp = 0,
                            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                        ),
                    ),
            )

        val beregningsresultatForDesember =
            beregningsresultatForMåned(
                måned = desember,
                grunnlag =
                    beregningsgrunnlag(
                        desember,
                        vedtaksperioder =
                            listOf(
                                vedtaksperiodeGrunnlag(
                                    vedtaksperiodeBeregning(1 desember 2024, 31 desember 2024),
                                ),
                            ),
                    ),
                beløpsperioder =
                    listOf(
                        Beløpsperiode(
                            dato = 2 desember 2024,
                            beløp = 500,
                            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                        ),
                    ),
            )

        val beregningsresultat =
            BeregningsresultatTilsynBarn(
                perioder = listOf(beregningsresultatForNovember, beregningsresultatForDesember),
            )

        val andeler = beregningsresultat.mapTilAndelTilkjentYtelse(saksbehandling())
        assertThat(andeler).hasSize(3)
        assertThat(andeler.filter { it.beløp == 0 }).hasSize(1)

        val vedtaksperioderFraAndelerUten0Beløp =
            andeler
                .filter { it.beløp > 0 }
                .map { finnPeriodeFraAndel(beregningsresultat, it) }

        assertThat(vedtaksperioderFraAndelerUten0Beløp).containsExactly(
            Datoperiode(1 november 2024, 29 november 2024),
            Datoperiode(30 november 2024, 31 desember 2024),
        )
    }
}
