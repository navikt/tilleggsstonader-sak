package no.nav.tilleggsstonader.sak.vedtak.boutgifter

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TypeAndel
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.vedtak.UtgiftBeregningDato
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month.APRIL
import java.time.Month.FEBRUARY
import java.time.Month.MARCH
import java.time.Month.MAY

class BoutgifterAndelTilkjentYtelseMapperTest {
    @Test
    fun `fom og tom på andel tilkjent ytelse skal være første hverdag i måneden`() {
        val mandag10Mars = LocalDate.of(2025, MARCH, 10)
        val mandag17Mars = LocalDate.of(2025, MARCH, 17)

        val beregningsgrunnlag =
            lagBeregningsgrunnlagMedEnkeltutgift(
                fom = mandag10Mars,
                tom = mandag17Mars, // TODO: Sjekk om dette er et reelt beregningsgrunnlag
            )

        val andeler = finnAndelTilkjentYtelse(beregningsgrunnlag)

        val førsteUkedagIMars = LocalDate.of(2025, MARCH, 3)
        with(andeler.single()) {
            assertThat(fom).isEqualTo(førsteUkedagIMars)
            assertThat(tom).isEqualTo(førsteUkedagIMars)
        }
    }

    @Test
    fun `utbetalingsdato på andel tilkjent ytelse skal være første ukedag i utbetalingsperioden`() {
        val lørdag8Mars = LocalDate.of(2025, MARCH, 8)
        val søndag9Mars = LocalDate.of(2025, MARCH, 9)
        val mandag10Mars = LocalDate.of(2025, MARCH, 10)

        val beregningsgrunnlag =
            lagBeregningsgrunnlagMedEnkeltutgift(
                fom = lørdag8Mars,
                tom = søndag9Mars,
                utbetalingsdato = mandag10Mars,
            )

        val andeler = finnAndelTilkjentYtelse(beregningsgrunnlag)

        assertThat(andeler.single().utbetalingsdato).isEqualTo(mandag10Mars)
    }

    @Test
    fun `ved flere utbetalingsperioder skal vi få én andel tilkjent ytelse per utbetalingsperiode`() {
        val tirsdag1April = LocalDate.of(2025, APRIL, 1)
        val onsdag30April = LocalDate.of(2025, APRIL, 30)
        val beregningsgrunnlagApril =
            lagBeregningsgrunnlagMedEnkeltutgift(
                fom = tirsdag1April,
                tom = onsdag30April,
            )

        val torsdag1Mai = LocalDate.of(2025, MAY, 1)
        val lørdag31Mai = LocalDate.of(2025, MAY, 31)
        val beregningsgrunnlagMai =
            lagBeregningsgrunnlagMedEnkeltutgift(
                fom = torsdag1Mai,
                tom = lørdag31Mai,
            )

        val andeler = finnAndelTilkjentYtelse(beregningsgrunnlagApril, beregningsgrunnlagMai)

        assertThat { andeler.size == 2 }

        with(andeler.first()) {
            assertThat(fom).isEqualTo(tirsdag1April)
            assertThat(tom).isEqualTo(tirsdag1April)
            assertThat(utbetalingsdato).isEqualTo(tirsdag1April)
        }

        with(andeler.last()) {
            assertThat(fom).isEqualTo(torsdag1Mai)
            assertThat(tom).isEqualTo(torsdag1Mai)
            assertThat(utbetalingsdato).isEqualTo(torsdag1Mai)
        }
    }

    @Test
    fun `Utbetalingsperioder med NEDSATT_ARBEIDSEVNE summeres opp til én andel så lenge de har samme utbetalingsdato`() {
        val mandag10Februar = LocalDate.of(2025, FEBRUARY, 10)
        val torsdag27Feb = LocalDate.of(2025, FEBRUARY, 27)
        val fredag7Mars = LocalDate.of(2025, MARCH, 7)

        val aap =
            lagBeregningsgrunnlagMedEnkeltutgift(
                fom = mandag10Februar,
                utbetalingsdato = mandag10Februar,
            ).copy(målgruppe = NEDSATT_ARBEIDSEVNE)

        val uføretrygd =
            lagBeregningsgrunnlagMedEnkeltutgift(
                fom = torsdag27Feb,
                utbetalingsdato = mandag10Februar,
            ).copy(målgruppe = NEDSATT_ARBEIDSEVNE)

        val nedsattArbeidsevne =
            lagBeregningsgrunnlagMedEnkeltutgift(
                fom = fredag7Mars,
                utbetalingsdato = mandag10Februar,
            ).copy(målgruppe = NEDSATT_ARBEIDSEVNE)

        val andel = finnAndelTilkjentYtelse(aap, uføretrygd, nedsattArbeidsevne)

        with(andel.single()) {
            assertThat { fom == mandag10Februar }
            assertThat { tom == mandag10Februar }
            assertThat { type == TypeAndel.BOUTGIFTER_AAP }
            assertThat { beløp == 1000 * 3 }
        }
    }

    @Test
    fun `Utbetalingsperioder med AAP og OVERGANGSSTØNAD grupperes til to ulike andeler`() {
        val mandag10Februar = LocalDate.of(2025, FEBRUARY, 10)

        val aap =
            lagBeregningsgrunnlagMedEnkeltutgift(
                fom = mandag10Februar,
            ).copy(målgruppe = NEDSATT_ARBEIDSEVNE)

        val overgangsstønad =
            lagBeregningsgrunnlagMedEnkeltutgift(
                fom = mandag10Februar,
            ).copy(målgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER)

        val andel = finnAndelTilkjentYtelse(aap, overgangsstønad)

        assertThat { andel.size == 2 }

        with(andel.first()) {
            assertThat { type == TypeAndel.BOUTGIFTER_AAP }
            assertThat { beløp == 1000 }
        }

        with(andel.last()) {
            assertThat { type == TypeAndel.BOUTGIFTER_ENSLIG_FORSØRGER }
            assertThat { beløp == 1000 }
        }
    }

    @Test
    fun `statusIverksetting skal være UBEHANDLET for beregningsgrunnlag med bekreftet sats`() {
        val tirsdag1April = LocalDate.of(2025, APRIL, 1)
        val onsdag30April = LocalDate.of(2025, APRIL, 30)

        val beregningsgrunnlag =
            lagBeregningsgrunnlagMedEnkeltutgift(
                fom = tirsdag1April,
                tom = onsdag30April,
            )

        val andel = finnAndelTilkjentYtelse(beregningsgrunnlag)
        assertThat(andel.single().statusIverksetting).isEqualTo(StatusIverksetting.UBEHANDLET)
    }

    @Test
    fun `statusIverksetting skal være VENTER_PÅ_SATS_ENDRING for beregningsgrunnlag uten bekreftet sats`() {
        val tirsdag1April = LocalDate.of(2025, APRIL, 1)
        val onsdag30April = LocalDate.of(2025, APRIL, 30)

        val beregningsgrunnlag =
            lagBeregningsgrunnlagMedEnkeltutgift(
                fom = tirsdag1April,
                tom = onsdag30April,
            ).copy(makssatsBekreftet = false)

        val andel = finnAndelTilkjentYtelse(beregningsgrunnlag)
        assertThat(andel.single().statusIverksetting).isEqualTo(StatusIverksetting.VENTER_PÅ_SATS_ENDRING)
    }
}

private fun finnAndelTilkjentYtelse(vararg beregningsgrunnlag: Beregningsgrunnlag): List<AndelTilkjentYtelse> {
    val fagsak = fagsak(stønadstype = Stønadstype.BOUTGIFTER)
    val behandling = behandling(fagsak = fagsak, steg = StegType.BEREGNE_YTELSE)
    val saksbehandling = saksbehandling(fagsak = fagsak, behandling = behandling)

    val beregningsresultat =
        BeregningsresultatBoutgifter(
            perioder =
                beregningsgrunnlag.map {
                    BeregningsresultatForLøpendeMåned(grunnlag = it)
                },
        )
    return BoutgifterAndelTilkjentYtelseMapper.finnAndelTilkjentYtelse(saksbehandling, beregningsresultat)
}

private fun lagBeregningsgrunnlagMedEnkeltutgift(
    fom: LocalDate,
    tom: LocalDate = fom,
    utbetalingsdato: LocalDate = fom,
) = Beregningsgrunnlag(
    fom = fom,
    tom = tom,
    utbetalingsdato = utbetalingsdato,
    utgifter =
        mapOf(
            TypeBoutgift.UTGIFTER_OVERNATTING to
                listOf(
                    UtgiftBeregningDato(
                        fom = fom,
                        tom = tom,
                        utgift = 1000,
                    ),
                ),
        ),
    makssats = 4953,
    makssatsBekreftet = true,
    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
    aktivitet = AktivitetType.TILTAK,
)
