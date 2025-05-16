package no.nav.tilleggsstonader.sak.vedtak.læremidler.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.detaljerteVedtaksperioder.DetaljertVedtaksperioderLæremidlerMapper.finnDetaljerteVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DetaljertVedtaksperioderLæremidlerMapperTest {
    private val defaultAktivitet = AktivitetType.UTDANNING
    private val defaultMålgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER
    private val defaultStudienivå = Studienivå.HØYERE_UTDANNING
    private val defaultProsent = 100
    private val månedsbeløp = 901

    private val førsteJan = LocalDate.of(2024, 1, 1)
    private val sisteJan = LocalDate.of(2024, 1, 31)
    private val førsteFeb = LocalDate.of(2024, 2, 1)
    private val sisteFeb = LocalDate.of(2024, 2, 29)
    private val førsteApril = LocalDate.of(2024, 4, 1)
    private val sisteApril = LocalDate.of(2024, 4, 30)

    @Test
    fun `skal slå sammen sammenhengende vedtaksperioder med like verdier`() {
        val vedtak =
            innvilgetVedtak(
                beregningsresulat =
                    listOf(
                        beregningsresultatForMåned(førsteJan, sisteJan),
                        beregningsresultatForMåned(førsteFeb, sisteFeb),
                    ),
            )

        val res = vedtak.finnDetaljerteVedtaksperioder()
        val forventetRes = listOf(detaljertVedtaksperiodeLæremidler(fom = førsteJan, tom = sisteFeb, antallMåneder = 2))
        assertThat(res).isEqualTo(forventetRes)
    }

    @Test
    fun `skal ikke slå sammen vedtaksperioder som ikke overlapper`() {
        val beregningsresultatForApril = beregningsresultatForMåned(førsteJan, sisteFeb)

        val vedtak =
            innvilgetVedtak(
                beregningsresulat =
                    listOf(
                        beregningsresultatForMåned(førsteApril, sisteApril),
                        beregningsresultatForMåned(førsteFeb, sisteFeb),
                    ),
            )

        val res = vedtak.finnDetaljerteVedtaksperioder()

        val forventetRes =
            listOf(
                detaljertVedtaksperiodeLæremidler(fom = førsteFeb, tom = sisteFeb),
                detaljertVedtaksperiodeLæremidler(fom = førsteApril, tom = sisteApril),
            )

        assertThat(res).isEqualTo(forventetRes)
    }

    private fun beregningsgrunnlag(
        fom: LocalDate,
        tom: LocalDate,
    ) = Beregningsgrunnlag(
        fom = fom,
        tom = tom,
        utbetalingsdato = tom,
        aktivitet = defaultAktivitet,
        målgruppe = defaultMålgruppe,
        studienivå = defaultStudienivå,
        studieprosent = defaultProsent,
        satsBekreftet = true,
        sats = månedsbeløp,
    )

    private fun beregningsresultatForMåned(
        fom: LocalDate,
        tom: LocalDate,
    ) = BeregningsresultatForMåned(
        beløp = månedsbeløp,
        grunnlag = beregningsgrunnlag(fom, tom),
    )

    private fun detaljertVedtaksperiodeLæremidler(
        fom: LocalDate,
        tom: LocalDate,
        antallMåneder: Int = 1,
        aktivitet: AktivitetType = defaultAktivitet,
        målgruppe: FaktiskMålgruppe = defaultMålgruppe,
    ) = DetaljertVedtaksperiodeLæremidler(
        fom = fom,
        tom = tom,
        aktivitet = aktivitet,
        målgruppe = målgruppe,
        antallMåneder = antallMåneder,
        studienivå = defaultStudienivå,
        studieprosent = defaultProsent,
        månedsbeløp = månedsbeløp,
    )

    private fun innvilgetVedtak(beregningsresulat: List<BeregningsresultatForMåned>) =
        InnvilgelseLæremidler(
            vedtaksperioder = emptyList(),
            beregningsresultat =
                BeregningsresultatLæremidler(
                    perioder = beregningsresulat,
                ),
        )
}
