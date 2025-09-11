package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Beregningsresultat
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DagligReiseAndelTilkjentYtelseMapperTest {
    val behandlingId = BehandlingId.random()

    @Test
    fun `fom og tom på andel tilkjent ytelse skal være lik fom til reisen hvis det er en ukedag`() {
        val mandag = 1 september 2025
        val beregningsresultat =
            Beregningsresultat(
                reiser = listOf(lagBeregningsresultatForReise(mandag)),
            )
        val andeler = beregningsresultat.mapTilAndelTilkjentYtelse(behandlingId)
        with(andeler.single()) {
            assertThat(fom).isEqualTo(mandag)
            assertThat(tom).isEqualTo(mandag)
            assertThat(utbetalingsdato).isEqualTo(mandag)
        }
    }

    @Test
    fun `flere reiser med samme fom aggregeres til én andel med summert beløp`() {
        val mandag = 1 september 2025
        val beregningsresultat =
            Beregningsresultat(
                reiser =
                    listOf(
                        lagBeregningsresultatForReise(mandag, beløp = 100),
                        lagBeregningsresultatForReise(mandag, beløp = 200),
                    ),
            )
        val andeler = beregningsresultat.mapTilAndelTilkjentYtelse(behandlingId)
        assertThat(andeler).hasSize(1)
        assertThat(andeler.single().beløp).isEqualTo(300)
    }

    @Test
    fun `reiser med ulike fom-datoer gir en andel per dato`() {
        val mandag = 1 september 2025
        val tirsdag = 2 september 2025
        val beregningsresultat =
            Beregningsresultat(
                reiser =
                    listOf(
                        lagBeregningsresultatForReise(mandag, beløp = 100),
                        lagBeregningsresultatForReise(tirsdag, beløp = 200),
                    ),
            )
        val andeler = beregningsresultat.mapTilAndelTilkjentYtelse(behandlingId)
        assertThat(andeler).hasSize(2)
        with(andeler.first()) {
            assertThat(fom).isEqualTo(mandag)
            assertThat(tom).isEqualTo(mandag)
            assertThat(utbetalingsdato).isEqualTo(mandag)
        }
        with(andeler.last()) {
            assertThat(fom).isEqualTo(tirsdag)
            assertThat(tom).isEqualTo(tirsdag)
            assertThat(utbetalingsdato).isEqualTo(tirsdag)
        }
    }

    @Test
    fun `to reiser som starter på ulike helgedager, skal begge fredagen på de respektive helgedagene`() {
        val lørdag = 6 september 2025
        val søndag = 7 september 2025
        val beregningsresultat =
            Beregningsresultat(
                reiser = listOf(lagBeregningsresultatForReise(lørdag), lagBeregningsresultatForReise(søndag)),
            )

        val andeler = beregningsresultat.mapTilAndelTilkjentYtelse(behandlingId)

        assertThat(andeler).hasSize(2)
        with(andeler.first()) {
            assertThat(fom).isEqualTo(lørdag)
            assertThat(tom).isEqualTo(lørdag)
            assertThat(beløp).isEqualTo(100)
            assertThat(utbetalingsdato).isEqualTo(lørdag)
        }
        with(andeler.last()) {
            assertThat(fom).isEqualTo(søndag)
            assertThat(tom).isEqualTo(søndag)
            assertThat(beløp).isEqualTo(100)
            assertThat(utbetalingsdato).isEqualTo(søndag)
        }
    }

    @Test
    fun `to ulike målgrupper på samme dag er ikke støttet enda, og skal derfor feile`() {
        val fredag = 5 september 2025
        val vedtaksperiodeEnsligForsørger = listOf(lagVedtaksperiodeGrunnlag(fredag, målgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER))
        val vedtaksperioderNedsattArbeidsevne =
            listOf(
                lagVedtaksperiodeGrunnlag(fredag, målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE),
            )
        val beregningsresultat =
            Beregningsresultat(
                reiser =
                    listOf(
                        lagBeregningsresultatForReise(
                            fredag,
                            beregningsgrunnlag =
                                lagBeregningsgrunnlag(
                                    fom = fredag,
                                    vedtaksperioder = vedtaksperiodeEnsligForsørger,
                                ),
                        ),
                        lagBeregningsresultatForReise(
                            fredag,
                            beregningsgrunnlag =
                                lagBeregningsgrunnlag(
                                    fom = fredag,
                                    vedtaksperioder = vedtaksperioderNedsattArbeidsevne,
                                ),
                        ),
                    ),
            )

        val message = assertThrows<IllegalArgumentException> { beregningsresultat.mapTilAndelTilkjentYtelse(behandlingId) }.message
        assertThat(message).isEqualTo("Støtter foreløpig ikke ulike målgrupper på samme utbetalingsdato")
    }
}
