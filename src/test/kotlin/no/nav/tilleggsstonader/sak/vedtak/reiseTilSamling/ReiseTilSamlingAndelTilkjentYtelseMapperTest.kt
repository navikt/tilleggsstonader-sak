package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.domain.BeregningsresultatReiseTilSamling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ReiseTilSamlingAndelTilkjentYtelseMapperTest {
    val saksbehandling = saksbehandling(fagsak(stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO))

    @Nested
    inner class OffentligTransport {
        @Test
        fun `fom og tom på andel tilkjent ytelse skal være lik fom til reisen hvis det er en ukedag`() {
            val mandag = 1 september 2025
            val belopOffentlig = BigDecimal.valueOf(123)
            val beregningsresultat =
                BeregningsresultatReiseTilSamling(
                    offentligTransport = listOf(lagBeregningsresultatForOffentligTransport(mandag, beløp = belopOffentlig)),
                    privatBil = emptyList(),
                )
            val andeler = beregningsresultat.mapTilAndelTilkentYtelse(saksbehandling)
            with(andeler.single()) {
                assertThat(fom).isEqualTo(mandag)
                assertThat(tom).isEqualTo(mandag)
                assertThat(utbetalingsdato).isEqualTo(mandag)
                assertThat(beløp).isEqualTo(belopOffentlig.toInt())
            }
        }

        @Test
        fun `reiser med ulike fom-datoer gir en andel per dato`() {
            val mandag = 1 september 2025
            val tirsdag = 2 september 2025

            val offentlig1 = lagBeregningsresultatForOffentligTransport(fom = mandag, tom = mandag)
            val offentlig2 = lagBeregningsresultatForOffentligTransport(fom = tirsdag, tom = tirsdag)

            val beregningsresultat =
                BeregningsresultatReiseTilSamling(
                    offentligTransport = listOf(offentlig1, offentlig2),
                    privatBil = emptyList(),
                )

            val andeler = beregningsresultat.mapTilAndelTilkentYtelse(saksbehandling)
            assertThat(andeler.map { it.fom }).containsExactlyInAnyOrder(mandag, tirsdag)
            assertThat(andeler.size).isEqualTo(2)
        }

        @Test
        fun `når beregningsresultat ikke har offentlig transport returneres ingen andeler`() {
            val beregningsresultat =
                BeregningsresultatReiseTilSamling(
                    offentligTransport = emptyList(),
                    privatBil = emptyList(),
                )

            val andeler = beregningsresultat.mapTilAndelTilkentYtelse(saksbehandling)
            assertThat(andeler).isEmpty()
        }
    }

    @Nested
    inner class PrivatBil {
        @Test
        fun `fom og tom på andel tilkjent ytelse skal være lik fom til reisen hvis det er en ukedag`() {
            val mandag = 1 september 2025
            val belopPrivat = BigDecimal.valueOf(456)
            val beregningsresultat =
                BeregningsresultatReiseTilSamling(
                    offentligTransport = emptyList(),
                    privatBil = listOf(lagBeregningsresultatForPrivatBil(mandag, beløp = belopPrivat)),
                )
            val andeler = beregningsresultat.mapTilAndelTilkentYtelse(saksbehandling)
            with(andeler.single()) {
                assertThat(fom).isEqualTo(mandag)
                assertThat(tom).isEqualTo(mandag)
                assertThat(utbetalingsdato).isEqualTo(mandag)
                assertThat(beløp).isEqualTo(belopPrivat.toInt())
            }
        }

// TODO legge til sjekk på verdi på beløp
        @Test
        fun `når beregningsresultat ikke har privat bil returneres ingen andeler`() {
            val beregningsresultat =
                BeregningsresultatReiseTilSamling(
                    offentligTransport = emptyList(),
                    privatBil = emptyList(),
                )

            val andeler = beregningsresultat.mapTilAndelTilkentYtelse(saksbehandling)
            assertThat(andeler).isEmpty()
        }
    }
}
