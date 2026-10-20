package no.nav.tilleggsstonader.sak.vedtak.flytting.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlytteSelv
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlyttebyrå
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsgrunnlagFlyttebyråTilbud
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlyttevilkår
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.BeregningsresultatFlytting
import no.nav.tilleggsstonader.sak.vedtak.flytting.domain.Flyttemåte
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class DetaljertVedtaksperiodeFlyttingTest {
    @Test
    fun `mapper flyttemåte og utbetalingsstatus og beholder separate perioder`() {
        val flyttedato = 1 januar 2026
        val tom = 2 januar 2026
        val resultat =
            BeregningsresultatFlytting(
                resultater =
                    listOf(
                        resultat(flyttedato, tom, BigDecimal("100"), dokumentert = true),
                        resultat(flyttedato, tom, BigDecimal("200"), dokumentert = false),
                        resultat(flyttedato, tom, BigDecimal("300"), flytteSelv = true),
                    ),
            )

        val perioder = resultat.tilDetaljerteVedtaksperioder(Stønadstype.FLYTTING_TSO)

        assertThat(perioder)
            .containsExactly(
                DetaljertVedtaksperiodeFlytting(
                    stønadstype = Stønadstype.FLYTTING_TSO,
                    flyttedato = flyttedato,
                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                    beløp = BigDecimal("100"),
                    flyttemåte = Flyttemåte.FLYTTEBYRÅ,
                    skalUtbetales = true,
                ),
                DetaljertVedtaksperiodeFlytting(
                    stønadstype = Stønadstype.FLYTTING_TSO,
                    flyttedato = flyttedato,
                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                    beløp = BigDecimal("200"),
                    flyttemåte = Flyttemåte.FLYTTEBYRÅ,
                    skalUtbetales = false,
                ),
                DetaljertVedtaksperiodeFlytting(
                    stønadstype = Stønadstype.FLYTTING_TSO,
                    flyttedato = flyttedato,
                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                    beløp = BigDecimal("300"),
                    flyttemåte = Flyttemåte.FLYTTE_SELV,
                    skalUtbetales = true,
                ),
            )
    }

    private fun resultat(
        fom: java.time.LocalDate,
        tom: java.time.LocalDate,
        beløp: BigDecimal,
        dokumentert: Boolean = true,
        flytteSelv: Boolean = false,
    ) = BeregningsresultatFlyttevilkår(
        fom = fom,
        tom = tom,
        beløp = beløp,
        målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
        grunnlag =
            if (flytteSelv) {
                BeregningsgrunnlagFlytteSelv(
                    avstandEnVei = 100,
                    sats = BigDecimal("2.94"),
                    satsBekreftet = true,
                    henger = BigDecimal.ZERO,
                    bompenger = BigDecimal.ZERO,
                    ferge = BigDecimal.ZERO,
                    parkering = BigDecimal.ZERO,
                )
            } else {
                BeregningsgrunnlagFlyttebyrå(
                    tilbud =
                        listOf(
                            BeregningsgrunnlagFlyttebyråTilbud("Byrå A", BigDecimal("100")),
                            BeregningsgrunnlagFlyttebyråTilbud("Byrå B", BigDecimal("100")),
                        ),
                    valgtTilbud = BeregningsgrunnlagFlyttebyråTilbud("Byrå A", BigDecimal("100")),
                    erBetalingDokumentert = dokumentert,
                )
            },
    )
}
