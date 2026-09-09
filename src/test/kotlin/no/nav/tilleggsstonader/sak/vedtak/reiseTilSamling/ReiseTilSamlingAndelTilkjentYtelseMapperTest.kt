package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.feil.Feil
import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

class ReiseTilSamlingAndelTilkjentYtelseMapperTest {
    val saksbehandling = saksbehandling(fagsak(stønadstype = Stønadstype.REISE_TIL_SAMLING_TSO))
    val saksbehandlingTsr = saksbehandling(fagsak(stønadstype = Stønadstype.REISE_TIL_SAMLING_TSR))

    @Nested
    inner class OffentligTransport {
        @Test
        fun `fom og tom på andel tilkjent ytelse skal være lik fom til reisen hvis det er en ukedag`() {
            val mandag = 1 september 2025
            val belopOffentlig = BigDecimal.valueOf(123)

            val andel =
                lagBeregningsresultatForOffentligTransport(mandag, beløp = belopOffentlig)
                    .mapTilAndelTilkjentYtelse(saksbehandling, tiltaksvariant = null)

            with(andel) {
                assertThat(fom).isEqualTo(mandag)
                assertThat(tom).isEqualTo(mandag)
                assertThat(utbetalingsdato).isEqualTo(mandag)
                assertThat(beløp).isEqualTo(belopOffentlig.toInt())
            }
        }

        @Test
        fun `for TSR skal TypeAndel utledes fra tiltaksvariant`() {
            val mandag = 1 september 2025

            val andel =
                lagBeregningsresultatForOffentligTransport(fom = mandag, brukersNavKontor = "1234")
                    .mapTilAndelTilkjentYtelse(saksbehandlingTsr, tiltaksvariant = TypeAktivitet.ARBFORB)

            assertThat(andel.type).isEqualTo(TypeAndel.REISE_TIL_SAMLING_TILTAK_ARBEIDSFORBEREDENDE)
        }

        @Test
        fun `for TSR kastes det feil dersom tiltaksvariant mangler`() {
            val mandag = 1 september 2025
            val beregningsresultat = lagBeregningsresultatForOffentligTransport(fom = mandag, brukersNavKontor = "1234")

            assertThrows<Feil> {
                beregningsresultat.mapTilAndelTilkjentYtelse(saksbehandlingTsr, tiltaksvariant = null)
            }
        }
    }

    @Nested
    inner class PrivatBil {
        @Test
        fun `fom og tom på andel tilkjent ytelse skal være lik fom til reisen hvis det er en ukedag`() {
            val mandag = 1 september 2025
            val belopPrivat = BigDecimal.valueOf(456)

            val andel =
                lagBeregningsresultatForPrivatBil(mandag, beløp = belopPrivat)
                    .mapTilAndelTilkjentYtelse(saksbehandling, tiltaksvariant = null)

            with(andel) {
                assertThat(fom).isEqualTo(mandag)
                assertThat(tom).isEqualTo(mandag)
                assertThat(utbetalingsdato).isEqualTo(mandag)
                assertThat(beløp).isEqualTo(belopPrivat.toInt())
            }
        }

        @Test
        fun `for TSR skal TypeAndel utledes fra tiltaksvariant`() {
            val mandag = 1 september 2025

            val andel =
                lagBeregningsresultatForPrivatBil(fom = mandag, brukersNavKontor = "1234")
                    .mapTilAndelTilkjentYtelse(saksbehandlingTsr, tiltaksvariant = TypeAktivitet.HOYEREUTD)

            assertThat(andel.type).isEqualTo(TypeAndel.REISE_TIL_SAMLING_TILTAK_HØYERE_UTDANNING)
        }
    }

    @Nested
    inner class FinnTypeAndelFraTiltaksvariant {
        @Test
        fun `mapper kjent tiltaksvariant til riktig TypeAndel`() {
            assertThat(finnTypeAndelFraTiltaksvariantReiseTilSamling(TypeAktivitet.ARBFORB))
                .isEqualTo(TypeAndel.REISE_TIL_SAMLING_TILTAK_ARBEIDSFORBEREDENDE)
        }

        @Test
        fun `kaster Feil med brukervendt melding når tiltaksvariant ikke er mappet til en TypeAndel`() {
            val tiltaksvariantUtenMapping =
                TypeAktivitet.entries.first { it !in tiltaksvariantTilTypeAndelMapReiseTilSamlingTsr }

            val feil =
                assertThrows<Feil> {
                    finnTypeAndelFraTiltaksvariantReiseTilSamling(tiltaksvariantUtenMapping)
                }

            assertThat(feil.message).contains("Ta kontakt med utviklerteamet")
        }
    }
}
