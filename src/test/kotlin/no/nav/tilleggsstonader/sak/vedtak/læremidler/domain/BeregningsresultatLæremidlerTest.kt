package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil.beregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BeregningsresultatLæremidlerTest {
    val beregningsresultat =
        BeregningsresultatLæremidler(
            perioder =
                listOf(
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
        val januar =
            beregningsresultatForMåned(
                fom = LocalDate.of(2025, 1, 1),
                tom = LocalDate.of(2025, 1, 31),
            )
        val februar =
            beregningsresultatForMåned(
                fom = LocalDate.of(2025, 2, 1),
                tom = LocalDate.of(2025, 2, 28),
            )
        val mars =
            beregningsresultatForMåned(
                fom = LocalDate.of(2025, 3, 1),
                tom = LocalDate.of(2025, 3, 31),
            )
        val result =
            BeregningsresultatLæremidler(perioder = listOf(januar, februar, mars))
                .filtrerFraOgMed(LocalDate.of(2025, 2, 15))

        val avkortetFebruar = februar.copy(grunnlag = februar.grunnlag.copy(fom = LocalDate.of(2025, 2, 15)))
        val forventetResultat = BeregningsresultatLæremidler(perioder = listOf(avkortetFebruar, mars))

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
        val revurderFra = LocalDate.of(2024, 1, 20)
        val kuttePerioderVedOpphør = avkortBeregningsresultatVedOpphør(forrigeVedtak, revurderFra).perioder

        assertThat(kuttePerioderVedOpphør).isEqualTo(
            listOf(
                BeregningsresultatForMåned(
                    beløp = 875,
                    grunnlag =
                        Beregningsgrunnlag(
                            fom = LocalDate.of(2024, 1, 1),
                            tom = LocalDate.of(2024, 1, 7),
                            utbetalingsdato = LocalDate.of(2024, 1, 1),
                            studienivå = Studienivå.HØYERE_UTDANNING,
                            studieprosent = 100,
                            sats = 875,
                            satsBekreftet = true,
                            målgruppe = MålgruppeType.AAP,
                        ),
                ),
            ),
        )
    }

    @Test
    fun `perioder før Revurder-fra blir ikke kuttet - tester med lengre periode`() {
        val innvilgelseLæremidlerMedLangPeriode =
            InnvilgelseLæremidler(
                vedtaksperioder =
                    listOf(
                        Vedtaksperiode(
                            fom = LocalDate.of(2024, 1, 1),
                            tom = LocalDate.of(2024, 1, 31),
                        ),
                        Vedtaksperiode(
                            fom = LocalDate.of(2024, 2, 1),
                            tom = LocalDate.of(2024, 2, 29),
                        ),
                        Vedtaksperiode(
                            fom = LocalDate.of(2024, 3, 1),
                            tom = LocalDate.of(2024, 3, 31),
                        ),
                        Vedtaksperiode(
                            fom = LocalDate.of(2024, 4, 1),
                            tom = LocalDate.of(2024, 4, 30),
                        ),
                    ),
                beregningsresultat =
                    BeregningsresultatLæremidler(
                        perioder =
                            listOf(
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
                                beregningsresultatForMåned(
                                    fom = LocalDate.of(2024, 3, 1),
                                    tom = LocalDate.of(2024, 3, 31),
                                    utbetalingsdato = LocalDate.of(2024, 1, 1),
                                ),
                                beregningsresultatForMåned(
                                    fom = LocalDate.of(2024, 4, 1),
                                    tom = LocalDate.of(2024, 4, 30),
                                    utbetalingsdato = LocalDate.of(2024, 1, 1),
                                ),
                            ),
                    ),
            )

        val forrigeVedtak = LæremidlerTestUtil.innvilgelse(innvilgelseLæremidlerMedLangPeriode)

        val revurderFra = LocalDate.of(2024, 6, 1)
        val kuttePerioderVedOpphør = avkortBeregningsresultatVedOpphør(forrigeVedtak, revurderFra).perioder

        assertThat(kuttePerioderVedOpphør).isEqualTo(
            listOf<BeregningsresultatForMåned>(
                BeregningsresultatForMåned(
                    beløp = 875,
                    grunnlag =
                        Beregningsgrunnlag(
                            fom = LocalDate.of(2024, 1, 1),
                            tom = LocalDate.of(2024, 1, 31),
                            utbetalingsdato = LocalDate.of(2024, 1, 1),
                            studienivå = Studienivå.HØYERE_UTDANNING,
                            studieprosent = 100,
                            sats = 875,
                            satsBekreftet = true,
                            målgruppe = MålgruppeType.AAP,
                        ),
                ),
                BeregningsresultatForMåned(
                    beløp = 875,
                    grunnlag =
                        Beregningsgrunnlag(
                            fom = LocalDate.of(2024, 2, 1),
                            tom = LocalDate.of(2024, 2, 29),
                            utbetalingsdato = LocalDate.of(2024, 1, 1),
                            studienivå = Studienivå.HØYERE_UTDANNING,
                            studieprosent = 100,
                            sats = 875,
                            satsBekreftet = true,
                            målgruppe = MålgruppeType.AAP,
                        ),
                ),
                BeregningsresultatForMåned(
                    beløp = 875,
                    grunnlag =
                        Beregningsgrunnlag(
                            fom = LocalDate.of(2024, 3, 1),
                            tom = LocalDate.of(2024, 3, 31),
                            utbetalingsdato = LocalDate.of(2024, 1, 1),
                            studienivå = Studienivå.HØYERE_UTDANNING,
                            studieprosent = 100,
                            sats = 875,
                            satsBekreftet = true,
                            målgruppe = MålgruppeType.AAP,
                        ),
                ),
                BeregningsresultatForMåned(
                    beløp = 875,
                    grunnlag =
                        Beregningsgrunnlag(
                            fom = LocalDate.of(2024, 4, 1),
                            tom = LocalDate.of(2024, 4, 30),
                            utbetalingsdato = LocalDate.of(2024, 1, 1),
                            studienivå = Studienivå.HØYERE_UTDANNING,
                            studieprosent = 100,
                            sats = 875,
                            satsBekreftet = true,
                            målgruppe = MålgruppeType.AAP,
                        ),
                ),
            ),
        )
    }

    @Test
    fun `perioder midt i Revurder-fra blir kuttet`() {
        val forrigeVedtak = LæremidlerTestUtil.innvilgelse()
        val revurderFra = LocalDate.of(2024, 1, 5)
        val kuttePerioderVedOpphør = avkortBeregningsresultatVedOpphør(forrigeVedtak, revurderFra).perioder

        assertThat(kuttePerioderVedOpphør).isEqualTo(
            listOf<BeregningsresultatForMåned>(
                BeregningsresultatForMåned(
                    beløp = 875,
                    grunnlag =
                        Beregningsgrunnlag(
                            fom = LocalDate.of(2024, 1, 1),
                            tom = LocalDate.of(2024, 1, 4),
                            utbetalingsdato = LocalDate.of(2024, 1, 1),
                            studienivå = Studienivå.HØYERE_UTDANNING,
                            studieprosent = 100,
                            sats = 875,
                            satsBekreftet = true,
                            målgruppe = MålgruppeType.AAP,
                        ),
                ),
            ),
        )
    }

    @Test
    fun `perioder midt i Revurder-fra blir kuttet - tester med lengre periode - kutter i maanedsskiftet februar-mars i skuddåret 2024`() {
        val innvilgelseLæremidlerMedLangPeriode =
            InnvilgelseLæremidler(
                vedtaksperioder =
                    listOf(
                        Vedtaksperiode(
                            fom = LocalDate.of(2024, 1, 1),
                            tom = LocalDate.of(2024, 1, 31),
                        ),
                        Vedtaksperiode(
                            fom = LocalDate.of(2024, 2, 1),
                            tom = LocalDate.of(2024, 2, 29),
                        ),
                        Vedtaksperiode(
                            fom = LocalDate.of(2024, 3, 1),
                            tom = LocalDate.of(2024, 3, 31),
                        ),
                        Vedtaksperiode(
                            fom = LocalDate.of(2024, 4, 1),
                            tom = LocalDate.of(2024, 4, 30),
                        ),
                    ),
                beregningsresultat =
                    BeregningsresultatLæremidler(
                        perioder =
                            listOf(
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
                                beregningsresultatForMåned(
                                    fom = LocalDate.of(2024, 3, 1),
                                    tom = LocalDate.of(2024, 3, 31),
                                    utbetalingsdato = LocalDate.of(2024, 1, 1),
                                ),
                                beregningsresultatForMåned(
                                    fom = LocalDate.of(2024, 4, 1),
                                    tom = LocalDate.of(2024, 4, 30),
                                    utbetalingsdato = LocalDate.of(2024, 1, 1),
                                ),
                            ),
                    ),
            )

        val forrigeVedtak = LæremidlerTestUtil.innvilgelse(innvilgelseLæremidlerMedLangPeriode)

        val revurderFra = LocalDate.of(2024, 3, 1)
        val kuttePerioderVedOpphør = avkortBeregningsresultatVedOpphør(forrigeVedtak, revurderFra).perioder

        assertThat(kuttePerioderVedOpphør).isEqualTo(
            listOf<BeregningsresultatForMåned>(
                BeregningsresultatForMåned(
                    beløp = 875,
                    grunnlag =
                        Beregningsgrunnlag(
                            fom = LocalDate.of(2024, 1, 1),
                            tom = LocalDate.of(2024, 1, 31),
                            utbetalingsdato = LocalDate.of(2024, 1, 1),
                            studienivå = Studienivå.HØYERE_UTDANNING,
                            studieprosent = 100,
                            sats = 875,
                            satsBekreftet = true,
                            målgruppe = MålgruppeType.AAP,
                        ),
                ),
                BeregningsresultatForMåned(
                    beløp = 875,
                    grunnlag =
                        Beregningsgrunnlag(
                            fom = LocalDate.of(2024, 2, 1),
                            tom = LocalDate.of(2024, 2, 29),
                            utbetalingsdato = LocalDate.of(2024, 1, 1),
                            studienivå = Studienivå.HØYERE_UTDANNING,
                            studieprosent = 100,
                            sats = 875,
                            satsBekreftet = true,
                            målgruppe = MålgruppeType.AAP,
                        ),
                ),
            ),
        )
    }

    @Test
    fun `perioder etter Revurder-fra blir fjernet`() {
        val forrigeVedtak = LæremidlerTestUtil.innvilgelse()
        val revurderFra = LocalDate.of(2023, 12, 1)
        val kuttePerioderVedOpphør = avkortBeregningsresultatVedOpphør(forrigeVedtak, revurderFra).perioder

        assertThat(kuttePerioderVedOpphør).isEqualTo(emptyList<BeregningsresultatForMåned>())
    }
}
