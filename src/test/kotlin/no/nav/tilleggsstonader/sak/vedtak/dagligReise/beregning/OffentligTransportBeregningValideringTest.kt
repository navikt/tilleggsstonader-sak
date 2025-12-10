package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning

import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.util.vilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatNoException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class OffentligTransportBeregningValideringTest {
    val førsteJanuar = LocalDate.of(2025, 1, 1)
    val sisteJanuar = LocalDate.of(2025, 1, 31)

    val førsteFebruar = LocalDate.of(2025, 2, 1)
    val sisteFebruar = LocalDate.of(2025, 2, 28)

    val vedtaksperiodeJanFeb = listOf(vedtaksperiode(fom = førsteJanuar, tom = sisteFebruar))
    val vedtaksperioderJanFebMedSplitt =
        listOf(
            vedtaksperiode(fom = førsteJanuar, tom = sisteJanuar),
            vedtaksperiode(fom = førsteFebruar, tom = sisteFebruar),
        )

    val forventetFeilmeldingIkkeVilkårForAlleVedtaksperioder =
        "Kan ikke innvilge for valgte perioder fordi det ikke finnes vilkår for reise for alle vedtaksperioder."

    @Nested
    inner class ValiderReiser {
        @Test
        fun `skal ikke kaste feil dersom vilkår og vedtaksperioder matcher`() {
            val vilkår = listOf(vilkårDagligReise(fom = førsteJanuar, tom = sisteFebruar))

            assertThatNoException().isThrownBy { validerReiser(vilkår, vedtaksperiodeJanFeb) }
            assertThatNoException().isThrownBy { validerReiser(vilkår, vedtaksperioderJanFebMedSplitt) }
        }

        @Test
        fun `skal slå sammen to vilkår`() {
            val vilkår =
                listOf(
                    vilkårDagligReise(fom = førsteJanuar, tom = sisteJanuar),
                    vilkårDagligReise(fom = førsteFebruar, tom = sisteFebruar),
                )

            assertThatNoException().isThrownBy { validerReiser(vilkår, vedtaksperiodeJanFeb) }
            assertThatNoException().isThrownBy { validerReiser(vilkår, vedtaksperioderJanFebMedSplitt) }
        }

        @Test
        fun `skal kaste feil dersom det ikke er lagt inn noen reiser`() {
            val vilkår = emptyList<VilkårDagligReise>()

            assertThatExceptionOfType(ApiFeil::class.java)
                .isThrownBy { validerReiser(vilkår, vedtaksperiodeJanFeb) }
                .withMessage("Innvilgelse er ikke et gyldig vedtaksresultat når det ikke er lagt inn perioder med reise")
        }

        @Test
        fun `skal kaste feil dersom vedtaksperiode er lenger enn vilkår`() {
            val vilkår = listOf(vilkårDagligReise(fom = førsteJanuar, tom = sisteJanuar))

            assertThatExceptionOfType(ApiFeil::class.java)
                .isThrownBy { validerReiser(vilkår, vedtaksperiodeJanFeb) }
                .withMessage(forventetFeilmeldingIkkeVilkårForAlleVedtaksperioder)

            assertThatExceptionOfType(ApiFeil::class.java)
                .isThrownBy { validerReiser(vilkår, vedtaksperioderJanFebMedSplitt) }
                .withMessage(forventetFeilmeldingIkkeVilkårForAlleVedtaksperioder)
        }

        @Test
        fun `skal kaste feil dersom vedtaksperiode går på tvers av et opphold i vilkår`() {
            val vilkår =
                listOf(
                    vilkårDagligReise(fom = førsteJanuar, tom = sisteJanuar),
                    vilkårDagligReise(fom = LocalDate.of(2025, 3, 1), tom = LocalDate.of(2025, 3, 31)),
                )

            val vedtaksperioder = listOf(vedtaksperiode(fom = førsteJanuar, tom = LocalDate.of(2025, 3, 31)))

            assertThatExceptionOfType(ApiFeil::class.java)
                .isThrownBy { validerReiser(vilkår, vedtaksperioder) }
                .withMessage(forventetFeilmeldingIkkeVilkårForAlleVedtaksperioder)
        }
    }
}
