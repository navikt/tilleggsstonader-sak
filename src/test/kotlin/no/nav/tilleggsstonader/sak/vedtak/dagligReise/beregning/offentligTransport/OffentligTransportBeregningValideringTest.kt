package no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport

import no.nav.tilleggsstonader.libs.utils.dato.april
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.infrastruktur.exception.ApiFeil
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.util.vedtaksperiode
import no.nav.tilleggsstonader.sak.util.vilkårDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.VilkårDagligReise
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.assertj.core.api.Assertions.assertThatNoException
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID.randomUUID

class OffentligTransportBeregningValideringTest {
    val førsteJanuar = 1 januar 2025
    val sisteJanuar = 31 januar 2025

    val førsteFebruar = 1 februar 2025
    val sisteFebruar = 28 februar 2025

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

    @Nested
    inner class ValiderEndringAvAlleredeUtbetaltPeriode {
        private val januar2026 = 1 januar 2026
        private val mars2026 = 31 mars 2026
        private val april2026 = 1 april 2026

        @Test
        fun `skal varsle hvis allerede utbetalt periode endres fra enkeltbilletter til månedskort`() {
            val førstegangsbehandling =
                offentligTransportReise(fom = januar2026, tom = mars2026, beløp = 1500, antallDager = 20)
            val revurdering =
                BeregningsresultatOffentligTransport(
                    reiser =
                        listOf(
                            offentligTransportReise(
                                fom = januar2026,
                                tom = april2026,
                                beløp = 1806,
                                antallDager = 60,
                            ),
                        ),
                )

            assertThatExceptionOfType(ApiFeil::class.java)
                .isThrownBy { validerEndringAvAlleredeUtbetaltPeriode(revurdering, listOf(førstegangsbehandling)) }
                .withMessageContaining("allerede utbetalt periode med enkeltbilletter")
        }

        @Test
        fun `skal ikke varsle hvis revurdering ikke endrer billetttype for utbetalt periode`() {
            val førstegangsbehandling =
                offentligTransportReise(fom = januar2026, tom = mars2026, beløp = 1500, antallDager = 20)
            val revurdering =
                BeregningsresultatOffentligTransport(
                    reiser =
                        listOf(
                            offentligTransportReise(
                                fom = januar2026,
                                tom = april2026,
                                beløp = 1500,
                                antallDager = 50,
                            ),
                        ),
                )

            assertThatNoException()
                .isThrownBy { validerEndringAvAlleredeUtbetaltPeriode(revurdering, listOf(førstegangsbehandling)) }
        }

        private fun offentligTransportReise(
            fom: LocalDate,
            tom: LocalDate,
            beløp: Int,
            antallDager: Int,
        ) = BeregningsresultatForReise(
            reiseId = dummyReiseId,
            perioder =
                listOf(
                    BeregningsresultatForPeriode(
                        grunnlag =
                            BeregningsgrunnlagOffentligTransport(
                                fom = fom,
                                tom = tom,
                                prisEnkeltbillett = 75,
                                prisSyvdagersbillett = 662,
                                pris30dagersbillett = 1806,
                                antallReisedagerPerUke = 5,
                                vedtaksperioder =
                                    listOf(
                                        vedtaksperiodeGrunnlag(
                                            fom = fom,
                                            tom = tom,
                                            antallDager = antallDager,
                                        ),
                                    ),
                                antallReisedager = antallDager,
                                brukersNavKontor = null,
                            ),
                        beløp = beløp,
                        billettdetaljer = emptyMap(),
                    ),
                ),
        )

        private fun vedtaksperiodeGrunnlag(
            fom: LocalDate,
            tom: LocalDate,
            antallDager: Int,
        ) = VedtaksperiodeGrunnlag(
            id = randomUUID(),
            fom = fom,
            tom = tom,
            aktivitet = AktivitetType.TILTAK,
            typeAktivitet = null,
            målgruppe = MålgruppeType.AAP.faktiskMålgruppe(),
            antallReisedagerIVedtaksperioden = antallDager,
        )
    }
}
