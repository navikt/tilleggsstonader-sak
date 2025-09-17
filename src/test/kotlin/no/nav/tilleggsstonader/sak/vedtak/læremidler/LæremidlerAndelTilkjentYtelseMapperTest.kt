package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.tilleggsstonader.libs.utils.dato.august
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.november
import no.nav.tilleggsstonader.libs.utils.dato.oktober
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LæremidlerAndelTilkjentYtelseMapperTest {
    @Test
    fun `lag andeler fra beregningsresultat og finn perioder i beregningsresultat fra andeler`() {
        val beregningsresultat =
            BeregningsresultatLæremidler(
                perioder =
                    listOf(
                        beregningsresultatForMåned(
                            fom = 1 september 2025,
                            tom = 30 september 2025,
                            utbetalingsdato = 15 august 2025,
                        ),
                        beregningsresultatForMåned(
                            fom = 1 oktober 2025,
                            tom = 31 oktober 2025,
                            utbetalingsdato = 15 august 2025,
                        ),
                        beregningsresultatForMåned(
                            fom = 1 november 2025,
                            tom = 30 november 2025,
                            utbetalingsdato = 15 august 2025,
                        ),
                        beregningsresultatForMåned(
                            fom = 1 januar 2026,
                            tom = 31 januar 2026,
                            utbetalingsdato = 1 januar 2026,
                        ),
                        beregningsresultatForMåned(
                            fom = 1 februar 2026,
                            tom = 28 februar 2026,
                            utbetalingsdato = 1 januar 2026,
                        ),
                    ),
            )

        val andeler = beregningsresultat.mapTilAndelTilkjentYtelse(behandlingId = BehandlingId.random())

        assertThat(andeler).hasSize(2)
        with(andeler[0]) {
            assertThat(fom).isEqualTo(15 august 2025)
            assertThat(tom).isEqualTo(15 august 2025)
            assertThat(utbetalingsdato).isEqualTo(15 august 2025)
            val perioderFraAndel = finnPerioderFraAndel(beregningsresultat, this)
            assertThat(perioderFraAndel).hasSize(3)
            assertThat(perioderFraAndel.minOf { it.fom }).isEqualTo(1 september 2025)
            assertThat(perioderFraAndel.maxOf { it.tom }).isEqualTo(30 november 2025)
        }
        with(andeler[1]) {
            assertThat(fom).isEqualTo(1 januar 2026)
            assertThat(tom).isEqualTo(1 januar 2026)
            assertThat(utbetalingsdato).isEqualTo(1 januar 2026)
            val perioderFraAndel = finnPerioderFraAndel(beregningsresultat, this)
            assertThat(perioderFraAndel).hasSize(2)
            assertThat(perioderFraAndel.minOf { it.fom }).isEqualTo(1 januar 2026)
            assertThat(perioderFraAndel.maxOf { it.tom }).isEqualTo(28 februar 2026)
        }
    }
}
