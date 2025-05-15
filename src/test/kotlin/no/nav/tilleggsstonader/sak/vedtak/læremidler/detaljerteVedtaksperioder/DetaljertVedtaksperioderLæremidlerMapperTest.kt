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
    private val aktivitet = AktivitetType.UTDANNING
    private val målgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER
    private val studienivå = Studienivå.HØYERE_UTDANNING
    private val studieprosent = 100
    private val månedsbeløp = 901

    private val januarFom = LocalDate.of(2024, 1, 1)
    private val januarTom = LocalDate.of(2024, 1, 31)
    private val februarFom = LocalDate.of(2024, 2, 1)
    private val februarTom = LocalDate.of(2024, 2, 29)
    private val aprilFom = LocalDate.of(2024, 4, 1)
    private val aprilTom = LocalDate.of(2024, 4, 30)

    private fun beregningsgrunnlag(
        fom: LocalDate,
        tom: LocalDate,
    ) = Beregningsgrunnlag(
        fom = fom,
        tom = tom,
        utbetalingsdato = tom,
        aktivitet = aktivitet,
        målgruppe = målgruppe,
        studienivå = studienivå,
        studieprosent = studieprosent,
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

    private val vedtaksperiode =
        DetaljertVedtaksperiodeLæremidler(
            fom = februarFom,
            tom = februarTom,
            aktivitet = aktivitet,
            målgruppe = målgruppe,
            antallMåneder = 1,
            studienivå = studienivå,
            studieprosent = studieprosent,
            månedsbeløp = månedsbeløp,
        )

    private val sortertOgSammenslåttVedtaksperiode =
        vedtaksperiode.copy(
            fom = januarFom,
            tom = februarTom,
            antallMåneder = 2,
        )

    private val beregningsresultatForJan = beregningsresultatForMåned(januarFom, januarTom)
    private val beregningsresultatForFeb = beregningsresultatForMåned(februarFom, februarTom)

    private val vedtakForSisteIverksatteBehandling =
        InnvilgelseLæremidler(
            vedtaksperioder = emptyList(),
            beregningsresultat =
                BeregningsresultatLæremidler(
                    listOf(
                        beregningsresultatForJan,
                        beregningsresultatForFeb,
                    ),
                ),
        )

    @Test
    fun `skal finne samenslått vedtaksperiode for siste iverksatte behandling`() {
        val res = vedtakForSisteIverksatteBehandling.finnDetaljerteVedtaksperioder()
        assertThat(res).isEqualTo(listOf(sortertOgSammenslåttVedtaksperiode))
    }

    @Test
    fun `skal ikke slå sammen vedtaksperioder for siste iverksatte behandling`() {
        val vedtaksperiodeFeb = vedtaksperiode
        val vedtaksperiodeApril =
            vedtaksperiode.copy(
                fom = aprilFom,
                tom = aprilTom,
            )

        val beregningsresultatForApril = beregningsresultatForMåned(aprilFom, aprilTom)

        val res =
            vedtakForSisteIverksatteBehandling
                .copy(
                    beregningsresultat =
                        BeregningsresultatLæremidler(
                            perioder =
                                listOf(
                                    beregningsresultatForFeb,
                                    beregningsresultatForApril,
                                ),
                        ),
                ).finnDetaljerteVedtaksperioder()

        assertThat(res).isEqualTo(listOf(vedtaksperiodeFeb, vedtaksperiodeApril))
    }
}
