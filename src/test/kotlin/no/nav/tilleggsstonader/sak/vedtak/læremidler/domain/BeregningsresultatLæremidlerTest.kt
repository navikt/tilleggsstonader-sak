package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BeregningsresultatLæremidlerTest {
    val beregningsresultat = BeregningsresultatLæremidler(
        perioder = listOf(
            beregningsresultatForMåned(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 31),
                utbetalingsdato = LocalDate.of(2024, 1, 1),
            ),
            beregningsresultatForMåned(
                fom = LocalDate.of(2024, 2, 1),
                tom = LocalDate.of(2024, 2, 29),
                utbetalingsdato = LocalDate.of(2024, 1, 1),
            ),
        ),
    )

    @Test
    fun `filtrerFraOgMed skal filtere vekk perioder før satt dato`() {
        val forventetResultat = BeregningsresultatLæremidler(
            perioder = listOf(
                beregningsresultat.perioder.last(),
            ),
        )
        val result = beregningsresultat.filtrerFraOgMed(LocalDate.of(2024, 2, 21))

        assertThat(result).isEqualTo(forventetResultat)
    }

    @Test
    fun `filtrerFraOgMed skal ikke filtere vekk perioder når inten satt dato`() {
        val result = beregningsresultat.filtrerFraOgMed(LocalDate.of(2024, 1, 1))

        assertThat(result).isEqualTo(beregningsresultat)
    }


    @Test
    fun `perioder før Revurder-fra blir ikke fjernet`() {

        val forrigeVedtak = LæremidlerTestUtil.innvilgelse()
        val kuttePerioderVedOpphør = kuttePerioderVedOpphør(forrigeVedtak, LocalDate.of(2024, 1, 20))

        assertThat(kuttePerioderVedOpphør).isEqualTo(listOf<BeregningsresultatForMåned>(BeregningsresultatForMåned(
            beløp = 875,
            grunnlag = Beregningsgrunnlag(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 7),
                utbetalingsdato = LocalDate.of(2024, 1, 1),
                studienivå = Studienivå.HØYERE_UTDANNING,
                studieprosent = 100,
                sats = 875,
                satsBekreftet = true,
                målgruppe = MålgruppeType.AAP,
            )
        )))
    }
}
