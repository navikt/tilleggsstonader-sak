package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.libs.utils.dato.september
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.Beregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class DagligReiseAndelTilkjentYtelseMapperTest {
    val behandlingId = BehandlingId.random()

    @Test
    fun `fom og tom på andel tilkjent ytelse skal være lik fom til reisen hvis det er en ukedag`() {
        val mandag = 1 september 2025 // Mandag
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
    fun `utbetalingsdato settes til fredagen før dersom fom er i helg`() {
        val lørdag = 6 september 2025 // Lørdag
        val fredag = 5 september 2025
        val beregningsresultat =
            Beregningsresultat(
                reiser = listOf(lagBeregningsresultatForReise(lørdag)),
            )
        val andeler = beregningsresultat.mapTilAndelTilkjentYtelse(behandlingId)
        with(andeler.single()) {
            assertThat(fom).isEqualTo(fredag)
            assertThat(tom).isEqualTo(fredag)
            assertThat(utbetalingsdato).isEqualTo(fredag)
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
}

private fun lagBeregningsresultatForReise(
    fom: LocalDate,
    beløp: Int = 100,
): BeregningsresultatForReise =
    BeregningsresultatForReise(
        perioder =
            listOf(
                BeregningsresultatForPeriode(
                    grunnlag = lagBeregningsgrunnlag(fom),
                    beløp = beløp,
                ),
            ),
    )

private fun lagBeregningsgrunnlag(fom: LocalDate): Beregningsgrunnlag =
    Beregningsgrunnlag(
        fom = fom,
        tom = fom.plusWeeks(1),
        prisEnkeltbillett = 50,
        pris30dagersbillett = 1000,
        antallReisedagerPerUke = 5,
        vedtaksperioder =
            listOf(
                VedtaksperiodeGrunnlag(
                    id = UUID.randomUUID(),
                    fom = fom,
                    tom = fom,
                    målgruppe = NEDSATT_ARBEIDSEVNE,
                    aktivitet = AktivitetType.TILTAK,
                    antallReisedagerIVedtaksperioden = 5,
                ),
            ),
        antallReisedager = 20,
    )
