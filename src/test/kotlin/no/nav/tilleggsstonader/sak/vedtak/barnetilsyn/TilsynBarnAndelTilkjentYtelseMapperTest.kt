package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.libs.utils.dato.oktober
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
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
}
