package no.nav.tilleggsstonader.sak.statistikk.vedtak.domene

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month.JANUARY

class MakssatsDvhUtilTest {
    @Test
    fun `støtte til tilsyn barn som ikke treffer makssatsen, skal vises riktig i statistikken`() {
        val førsteJanuar = LocalDate.of(2025, JANUARY, 1)
        val sisteJanuar = LocalDate.of(2025, JANUARY, 31)

        val utgifterTotaltJanuar = 4000
        val stønadseløpMindreEnnMakssats = 2560
        val makssats = 3000 // makssatsen vil ikke treffe her, fordi 64% av 4000 er 2560

        val (innvilgelseTilsynBarn, andelTilkjentYtelse) =
            lagTilsynBarnInnvilgelseMedBeløp(
                fom = førsteJanuar,
                tom = sisteJanuar,
                månedsbeløp = stønadseløpMindreEnnMakssats,
                makssats = makssats,
                utgift = utgifterTotaltJanuar,
            )

        val resultat = MakssatsDvhUtil.finnMakssats(andelTilkjentYtelse = andelTilkjentYtelse, vedtaksdata = innvilgelseTilsynBarn)

        val forventetResultat =
            MakssatsDvhUtil(
                makssats = makssats,
                beløpErBegrensetAvMakssats = false,
            )

        assertThat(resultat).isEqualTo(forventetResultat)
    }

    @Test
    fun `støtte til tilsyn barn som blir begrenset av makssatsen, skal vises riktig i statistikken`() {
        val førsteJanuar = LocalDate.of(2025, JANUARY, 1)
        val sisteJanuar = LocalDate.of(2025, JANUARY, 31)

        val utgifterTotaltJanuar = 5000
        val makssats = 3000

        val (innvilgelseTilsynBarn, andelTilkjentYtelse) =
            lagTilsynBarnInnvilgelseMedBeløp(
                fom = førsteJanuar,
                tom = sisteJanuar,
                månedsbeløp = makssats,
                makssats = makssats,
                utgift = utgifterTotaltJanuar,
            )

        val resultat = MakssatsDvhUtil.finnMakssats(andelTilkjentYtelse = andelTilkjentYtelse, vedtaksdata = innvilgelseTilsynBarn)

        val forventetResultat =
            MakssatsDvhUtil(
                makssats = makssats,
                beløpErBegrensetAvMakssats = true,
            )

        assertThat(resultat).isEqualTo(forventetResultat)
    }

    @Test
    fun `støtte til boutgifter som ikke lir begrenset av makssatsen, skal vises riktig i statistikken`() {
        val førsteJanuar = LocalDate.of(2025, JANUARY, 1)
        val makssats = 4300
        val utgiftJanuar = 2000
        val støtteJanuar = utgiftJanuar

        val (vedtaksdata, andelTilkjentYtelse) =
            lagBoutgifterInnvilgelseMedBeløp(
                fom = førsteJanuar,
                tom = førsteJanuar,
                månedsbeløp = støtteJanuar,
                makssats = makssats,
                utgift = utgiftJanuar,
            )

        val resultat = MakssatsDvhUtil.finnMakssats(andelTilkjentYtelse = andelTilkjentYtelse, vedtaksdata = vedtaksdata)

        val forventetResultat =
            MakssatsDvhUtil(
                makssats = makssats,
                beløpErBegrensetAvMakssats = false,
            )

        assertThat(resultat).isEqualTo(forventetResultat)
    }

    @Test
    fun `støtte til boutgifter som blir begrenset av makssatsen, skal vises riktig i statistikken`() {
        val førsteJanuar = LocalDate.of(2025, JANUARY, 1)
        val sisteJanuar = LocalDate.of(2025, JANUARY, 31)
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

        val resultat = MakssatsDvhUtil.finnMakssats(andelTilkjentYtelse = andelTilkjentYtelse, vedtaksdata = vedtaksdata)

        val forventetResultat =
            MakssatsDvhUtil(
                makssats = makssats,
                beløpErBegrensetAvMakssats = true,
            )

        assertThat(resultat).isEqualTo(forventetResultat)
    }
}
