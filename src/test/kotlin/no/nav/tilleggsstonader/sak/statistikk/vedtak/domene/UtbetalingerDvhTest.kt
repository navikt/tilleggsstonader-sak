package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.TilkjentYtelseUtil.andelTilkjentYtelse
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnTestUtil.defaultBehandling
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.læremidler.LæremidlerTestUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UtbetalingerDvhTest {
    @Test
    fun `mappes riktig for tilsyn barn`() {
        val førsteJanuar = 1 januar 2025

        val utgifterTotaltJanuar = 6000
        val stønadsbeløp = 3840
        val makssats = 8000

        val (vedtaksdata, andelTilkjentYtelse) =
            lagTilsynBarnInnvilgelseMedBeløp(
                fom = førsteJanuar,
                tom = førsteJanuar,
                månedsbeløp = stønadsbeløp,
                makssats = makssats,
                utgift = utgifterTotaltJanuar,
            )

        val innvilgelseTilsynBarn = TilsynBarnTestUtil.innvilgelse(vedtaksdata)
        val resultat = UtbetalingerDvh.fraDomene(setOf(andelTilkjentYtelse), innvilgelseTilsynBarn)

        val forventetResultat =
            UtbetalingerDvh.JsonWrapper(
                utbetalinger =
                    listOf(
                        UtbetalingerDvh(
                            fraOgMed = førsteJanuar,
                            tilOgMed = førsteJanuar,
                            type = AndelstypeDvh.TILSYN_BARN_AAP,
                            beløp = stønadsbeløp,
                            makssats = makssats,
                            beløpErBegrensetAvMakssats = false,
                        ),
                    ),
            )

        assertThat(resultat).isEqualTo(forventetResultat)
    }

    @Test
    fun `mappes riktig for læremidler`() {
        val innvilgelse = LæremidlerTestUtil.innvilgelse()
        val andlelerTilkjentYtelse = setOf(andelTilkjentYtelse(fom = 1 januar 2024))

        val resultat = UtbetalingerDvh.fraDomene(andlelerTilkjentYtelse, innvilgelse)

        val forventetResultat =
            UtbetalingerDvh.JsonWrapper(
                utbetalinger =
                    listOf(
                        UtbetalingerDvh(
                            fraOgMed = 1 januar 2024,
                            tilOgMed = 1 januar 2024,
                            type = AndelstypeDvh.TILSYN_BARN_AAP,
                            beløp = 11554,
                            makssats = null,
                            beløpErBegrensetAvMakssats = null,
                        ),
                    ),
            )

        assertThat(resultat).isEqualTo(forventetResultat)
    }

    @Test
    fun `mappes riktig for daglig reise`() {
        val førsteJanuar = 1 januar 2025
        val sisteJanuar = 31 januar 2025

        val stønadsbeløp = 3840

        val (vedtaksdata, andelTilkjentYtelse) =
            lagDagligReiseInnvilgelseMedBeløp(
                fom = førsteJanuar,
                tom = førsteJanuar,
                beløp = stønadsbeløp,
            )

        val innvilgelseDagligReise =
            GeneriskVedtak(
                behandlingId = defaultBehandling.id,
                type = TypeVedtak.INNVILGELSE,
                data = vedtaksdata,
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = null,
                opphørsdato = null,
            )

        val resultat = UtbetalingerDvh.fraDomene(setOf(andelTilkjentYtelse), innvilgelseDagligReise)

        val forventetResultat =
            UtbetalingerDvh.JsonWrapper(
                utbetalinger =
                    listOf(
                        UtbetalingerDvh(
                            fraOgMed = førsteJanuar,
                            tilOgMed = førsteJanuar,
                            type = AndelstypeDvh.DAGLIG_REISE_AAP,
                            beløp = stønadsbeløp,
                            beløpErBegrensetAvMakssats = null,
                        ),
                    ),
            )

        assertThat(resultat).isEqualTo(forventetResultat)
    }

    @Test
    fun `mappes riktig for boutgifter`() {
        val førsteJanuar = 1 januar 2025
        val makssats = 4953
        val støtteJanuar = makssats
        val utgiftJanuar = 7000

        val (vedtaksdata, andelTilkjentYtelse) =
            lagBoutgifterInnvilgelseMedBeløp(
                fom = førsteJanuar,
                tom = førsteJanuar,
                månedsbeløp = støtteJanuar,
                makssats = makssats,
                utgift = utgiftJanuar,
            )

        val innvilgelse =
            GeneriskVedtak(
                behandlingId = defaultBehandling.id,
                type = TypeVedtak.INNVILGELSE,
                data = vedtaksdata,
                gitVersjon = Applikasjonsversjon.versjon,
                tidligsteEndring = null,
                opphørsdato = null,
            )

        val resultat = UtbetalingerDvh.fraDomene(setOf(andelTilkjentYtelse), innvilgelse)

        val forventetResultat =
            UtbetalingerDvh.JsonWrapper(
                utbetalinger =
                    listOf(
                        UtbetalingerDvh(
                            fraOgMed = førsteJanuar,
                            tilOgMed = førsteJanuar,
                            type = AndelstypeDvh.BOUTGIFTER_AAP,
                            beløp = makssats,
                            makssats = makssats,
                            beløpErBegrensetAvMakssats = true,
                        ),
                    ),
            )

        assertThat(resultat).isEqualTo(forventetResultat)
    }

    // Se kommentar i UtbetalingerDvh.fraDomene
    @Test
    fun `gyldige andeler med beløp 0 blir fjernet`() {
        val innvilgelse = LæremidlerTestUtil.innvilgelse()
        val andlelerTilkjentYtelse = setOf(andelTilkjentYtelse(fom = 1 januar 2024, beløp = 0))

        val resultat = UtbetalingerDvh.fraDomene(andlelerTilkjentYtelse, innvilgelse)

        val forventetResultat =
            UtbetalingerDvh.JsonWrapper(
                utbetalinger = emptyList(),
            )

        assertThat(resultat).isEqualTo(forventetResultat)
    }
}
