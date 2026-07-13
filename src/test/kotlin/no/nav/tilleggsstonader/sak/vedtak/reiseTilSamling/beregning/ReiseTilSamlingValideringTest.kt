package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning

import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.util.vedtaksperiodeForBeregning
import no.nav.tilleggsstonader.sak.util.vilkårReiseTilSamling
import no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.beregning.ReiseTilSamlingValidering.filtrerBortUtgifterSomIkkeOverlapperVedtaksperioder
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseTilSamling.domain.VilkårReiseTilSamling
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import java.time.Month.JANUARY

class ReiseTilSamlingValideringTest {
    private val førsteJanuar: LocalDate = LocalDate.of(2025, JANUARY, 1)
    private val sisteJanuar: LocalDate = LocalDate.of(2025, JANUARY, 31)
    private val andreJanuar: LocalDate = førsteJanuar.plusDays(1)
    private val tiendeJanuar: LocalDate = førsteJanuar.plusDays(9)
    private val førsteFebruar: LocalDate = LocalDate.of(2025, Month.FEBRUARY, 1)

    private val ingenUtgifter = emptyList<VilkårReiseTilSamling>()
    private val behandling = saksbehandling()

    @Nested
    inner class ValiderUtgiftHeleVedtaksperioden {
        @Test
        fun `Er fornøyd hvis vi mangler både utgifter og vedtaksperioder`() {
            ReiseTilSamlingValidering.validerUtgiftHeleVedtaksperioden(
                vedtaksperioder = emptyList(),
                utgifter = ingenUtgifter,
            )
        }

        @Test
        fun `Er fornøyd hvis vi har én vedtaksperiode og én utgift som dekker hele perioden`() {
            val vedtaksperiode = vedtaksperiode(førsteJanuar, andreJanuar)
            val utgift =
                vilkårReiseTilSamling(
                    behandlingId = behandling.id,
                    fom = førsteJanuar,
                    tom = andreJanuar,
                )
            ReiseTilSamlingValidering.validerUtgiftHeleVedtaksperioden(
                vedtaksperioder = listOf(vedtaksperiode),
                utgifter = listOf(utgift),
            )
        }

        @Test
        fun `Skal feile hvis vi har vedtaksperiode men ingen utgift`() {
            val vedtaksperiode = vedtaksperiode(førsteJanuar, andreJanuar)

            assertThatThrownBy {
                ReiseTilSamlingValidering.validerUtgiftHeleVedtaksperioden(
                    vedtaksperioder = listOf(vedtaksperiode),
                    utgifter = ingenUtgifter,
                )
            }.hasMessageContaining("Vedtaksperioden 01.01.2025–02.01.2025")
        }

        @Test
        fun `Skal feile hvis utgift bare dekker deler av vedtaksperioden`() {
            val vedtaksperiode =
                vedtaksperiode(
                    fom = førsteJanuar,
                    tom = sisteJanuar,
                )

            val utgift =
                vilkårReiseTilSamling(
                    behandlingId = behandling.id,
                    fom = førsteJanuar,
                    tom = tiendeJanuar,
                )

            assertThatThrownBy {
                ReiseTilSamlingValidering.validerUtgiftHeleVedtaksperioden(
                    vedtaksperioder = listOf(vedtaksperiode),
                    utgifter = listOf(utgift),
                )
            }.hasMessageContaining("Vedtaksperioden")
        }
    }

    @Nested
    inner class ValiderUtgifterStrekkerSegUtenforVedtaksperiodene {
        @Test
        fun `Er fornøyd hvis utgift ligger innenfor vedtaksperioden`() {
            val vedtaksperiode =
                vedtaksperiodeForBeregning(
                    førsteJanuar,
                    sisteJanuar,
                )

            val utgift =
                vilkårReiseTilSamling(
                    behandlingId = behandling.id,
                    fom = førsteJanuar,
                    tom = sisteJanuar,
                )

            ReiseTilSamlingValidering.validerUtgifterStrekkerSegUtenforVedtaksperiodene(
                utgifter = listOf(utgift),
                vedtaksperioder = listOf(vedtaksperiode),
            )
        }

        @Test
        fun `Skal feile hvis utgift ligger utenfor vedtaksperioden`() {
            val vedtaksperiode =
                vedtaksperiodeForBeregning(
                    førsteJanuar,
                    andreJanuar,
                )

            val utgift =
                vilkårReiseTilSamling(
                    behandlingId = behandling.id,
                    fom = førsteJanuar,
                    tom = sisteJanuar,
                )

            assertThatThrownBy {
                ReiseTilSamlingValidering.validerUtgifterStrekkerSegUtenforVedtaksperiodene(
                    utgifter = listOf(utgift),
                    vedtaksperioder = listOf(vedtaksperiode),
                )
            }.hasMessageContaining(
                "Vi har foreløpig ikke støtte for å beregne når samlingsperioder strekker seg utenfor vedtaksperiodene.",
            )
        }

        @Nested
        inner class FiltrerBortUtgifter {
            @Test
            fun `skal beholde utgift som overlapper vedtaksperiode`() {
                val vedtaksperiode =
                    vedtaksperiodeForBeregning(
                        førsteJanuar,
                        tiendeJanuar,
                    )

                val utgift =
                    vilkårReiseTilSamling(
                        behandlingId = behandling.id,
                        fom = førsteJanuar,
                        tom = tiendeJanuar,
                    )

                val resultat =
                    listOf(utgift).filtrerBortUtgifterSomIkkeOverlapperVedtaksperioder(
                        listOf(vedtaksperiode),
                    )

                assertThat(resultat).containsExactly(utgift)
            }

            @Test
            fun `skal fjerne utgift som ikke overlapper vedtaksperiode`() {
                val vedtaksperiode =
                    vedtaksperiodeForBeregning(
                        førsteJanuar,
                        andreJanuar,
                    )

                val utgift =
                    vilkårReiseTilSamling(
                        behandlingId = behandling.id,
                        fom = tiendeJanuar,
                        tom = førsteFebruar,
                    )

                val resultat =
                    listOf(utgift).filtrerBortUtgifterSomIkkeOverlapperVedtaksperioder(
                        listOf(vedtaksperiode),
                    )

                assertThat(resultat).isEmpty()
            }
        }
    }
}
