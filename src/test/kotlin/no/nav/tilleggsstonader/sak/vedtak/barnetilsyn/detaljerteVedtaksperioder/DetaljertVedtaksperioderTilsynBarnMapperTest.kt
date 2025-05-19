package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.util.toYearMonth
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.detaljerteVedtaksperioder.DetaljertVedtaksperioderTilsynBarnMapper.finnDetaljerteVedtaksperioder
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class DetaljertVedtaksperioderTilsynBarnMapperTest {
    private val februarStart = LocalDate.of(2024, 2, 1)
    private val februarSlutt = LocalDate.of(2024, 2, 29)
    private val marsSlutt = LocalDate.of(2024, 3, 31)
    private val aktivitet = AktivitetType.TILTAK
    private val målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE

    private val detaljertVedtaksperiode =
        DetaljertVedtaksperiodeTilsynBarn(
            fom = februarStart,
            tom = februarSlutt,
            aktivitet = aktivitet,
            målgruppe = målgruppe,
            antallBarn = 1,
            totalMånedsUtgift = 2000,
        )

    private val vedtakForSisteIverksatteBehandling =
        InnvilgelseTilsynBarn(
            vedtaksperioder = emptyList(),
            beregningsresultat =
                BeregningsresultatTilsynBarn(
                    perioder =
                        listOf(
                            lagBeregningsresultatForMåned(
                                antallBarn = 1,
                                utgifterTotal = 2000,
                                fom = februarStart,
                                tom = februarSlutt,
                            ),
                        ),
                ),
        )

    @Test
    fun `skal finne vedtaksperiode for siste iverksatte behandling`() {
        val res = vedtakForSisteIverksatteBehandling.finnDetaljerteVedtaksperioder()
        assertThat(res).isEqualTo(listOf(detaljertVedtaksperiode))
    }

    @Test
    fun `skal ikke slå sammen vedtaksperioder med ulike verdier`() {
        val res =
            vedtakForSisteIverksatteBehandling
                .copy(
                    beregningsresultat =
                        BeregningsresultatTilsynBarn(
                            perioder =
                                listOf(
                                    lagBeregningsresultatForMåned(
                                        antallBarn = 2,
                                        utgifterTotal = 4000,
                                        fom = februarStart,
                                        tom = marsSlutt,
                                    ),
                                ),
                        ),
                ).finnDetaljerteVedtaksperioder()

        assertThat(res).isEqualTo(
            listOf(
                detaljertVedtaksperiode.copy(
                    fom = februarStart,
                    tom = marsSlutt,
                    antallBarn = 2,
                    totalMånedsUtgift = 4000,
                ),
            ),
        )
    }

    private fun lagVedtaksperiodeGrunnlag(
        fom: LocalDate,
        tom: LocalDate,
        antallDager: Int = 1,
        aktivitet: AktivitetType = this.aktivitet,
        målgruppe: FaktiskMålgruppe = this.målgruppe,
    ) = VedtaksperiodeGrunnlag(
        aktiviteter = emptyList(),
        antallDager = antallDager,
        vedtaksperiode = VedtaksperiodeBeregning(fom, tom, målgruppe, aktivitet),
    )

    private fun lagBeregningsresultatForMåned(
        antallBarn: Int,
        utgifterTotal: Int,
        fom: LocalDate,
        tom: LocalDate,
    ) = BeregningsresultatForMåned(
        dagsats = BigDecimal.valueOf(88.6),
        månedsbeløp = 89,
        grunnlag =
            Beregningsgrunnlag(
                måned = fom.toYearMonth(),
                makssats = 6248,
                utgifter = emptyList(),
                antallBarn = antallBarn,
                utgifterTotal = utgifterTotal,
                vedtaksperiodeGrunnlag =
                    listOf(
                        lagVedtaksperiodeGrunnlag(fom, tom),
                    ),
            ),
        beløpsperioder = emptyList(),
    )
}
