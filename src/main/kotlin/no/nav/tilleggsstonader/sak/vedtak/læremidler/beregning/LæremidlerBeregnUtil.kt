package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.splitPerÅr
import no.nav.tilleggsstonader.sak.util.datoEllerNesteMandagHvisLørdagEllerSøndag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning.LæremidlerSplitPerLøpendeMånedUtil.splitPerLøpendeMåneder
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import java.time.LocalDate

object LæremidlerBeregnUtil {

    /**
     * Splitter vedtaksperiode per år. Sånn at man får en periode for høstterminen og en for vårterminen
     * Dette for å kunne lage en periode for våren som ikke utbetales direkte, men når satsen for det nye året er satt.
     * Eks 2024-08-15 - 2025-06-20 blir 2024-08-15 - 2024-12-31 og 2025-01-01 - 2025-06-20
     */
    fun Periode<LocalDate>.delTilUtbetalingsPerioder(): List<GrunnlagForUtbetalingPeriode> =
        splitPerÅr { fom, tom -> Vedtaksperiode(fom, tom) }
            .flatMap { periode ->
                periode.splitPerLøpendeMåneder { fom, tom ->
                    GrunnlagForUtbetalingPeriode(
                        fom = fom,
                        tom = tom,
                        utbetalingsdato = periode.fom.datoEllerNesteMandagHvisLørdagEllerSøndag(),
                    )
                }
            }
}
