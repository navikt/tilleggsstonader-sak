package no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning

import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.lagBeregningsresultatMåned
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.BoutgifterTestUtil.lagUtgiftBeregningBoutgifter
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.beregning.MarkerSomDelAvTidligereUtbetlingUtils.markerSomDelAvTidligereUtbetaling
import no.nav.tilleggsstonader.sak.vedtak.boutgifter.domain.BeregningsresultatForLøpendeMåned
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeBoutgift
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MarkerSomDelAvTidligereUtbetlingUtilsTest {
    val utgift1 =
        mapOf(
            TypeBoutgift.UTGIFTER_OVERNATTING to
                listOf(
                    lagUtgiftBeregningBoutgifter(
                        fom = LocalDate.of(2025, 1, 1),
                        tom = LocalDate.of(2025, 1, 4),
                        utgift = 3000,
                    ),
                ),
        )
    val utgift2 =
        mapOf(
            TypeBoutgift.UTGIFTER_OVERNATTING to
                listOf(
                    lagUtgiftBeregningBoutgifter(
                        fom = LocalDate.of(2025, 2, 7),
                        tom = LocalDate.of(2025, 2, 13),
                        utgift = 3000,
                    ),
                ),
        )

    private val periode1 =
        lagBeregningsresultatMåned(
            fom = LocalDate.of(2025, 1, 1),
            tom = LocalDate.of(2025, 1, 31),
            utgifter = utgift1,
        )

    private val periode2 =
        lagBeregningsresultatMåned(
            fom = LocalDate.of(2025, 2, 7),
            tom = LocalDate.of(2025, 3, 6),
            utgifter = utgift2,
        )

    @Test
    fun `Marker perioder fra forrige behandling hvis utgiften er før dagens dato`() {
        val perioder = listOf(periode1, periode2)
        val result = perioder.markerSomDelAvTidligereUtbetaling()
        assertThat(result[0].delAvTidligereUtbetaling).isTrue
        assertThat(result[1].delAvTidligereUtbetaling).isTrue
    }

    @Test
    fun `Ikke marker perioder fra forrgie behandling hvis utgiften er etter dagens dato`() {
        val dagensDato = LocalDate.of(2025, 2, 5)
        val perioder = listOf(periode1, periode2)
        val result =
            perioder.markerSomDelAvTidligereUtbetaling(
                perioderFraForrigeVedtak = null,
                dagensDato = dagensDato,
            )

        assertThat(result[0].delAvTidligereUtbetaling).isTrue
        assertThat(result[1].delAvTidligereUtbetaling).isFalse
    }

    // Marker nye perioder hvis det finnes overlappende tidligere beregningsperiode og
    // det finnes en utgift i den beregningsperiode som er før dagens dato
    @Test
    fun `Marker nye perioder hvis krav oppfylt`() {
        val perioder = listOf(periode1, periode2)
        val result = perioder.markerSomDelAvTidligereUtbetaling(listOf(periode1))

        assertThat(result[0].delAvTidligereUtbetaling).isTrue
        assertThat(result[1].delAvTidligereUtbetaling).isFalse
    }

    // Ikke marker nye perioder når det finnes overlappende tidligere beregningsperiode og
    // det ikke finnes en utgift i den beregningsperiode som er før dagens dato
    @Test
    fun `Ikke marker nye perioder hvis datokrav ikke oppfylt`() {
        val dagensDato = LocalDate.of(2024, 12, 5)

        val perioder = listOf(periode1, periode2)
        val result =
            perioder.markerSomDelAvTidligereUtbetaling(
                perioderFraForrigeVedtak = listOf(periode1),
                dagensDato = dagensDato,
            )

        assertThat(result[0].delAvTidligereUtbetaling).isFalse
        assertThat(result[1].delAvTidligereUtbetaling).isFalse
    }

    @Test
    fun `Håndterer tom liste`() {
        val result = emptyList<BeregningsresultatForLøpendeMåned>().markerSomDelAvTidligereUtbetaling()

        assertThat(result).isEmpty()
    }
}
