package no.nav.tilleggsstonader.sak.vedtak.reiseTilSamling.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseEllerOpphørReiseTilSamling

object DetaljertVedtaksperioderReiseTilSamlingMapper {
    fun InnvilgelseEllerOpphørReiseTilSamling.finnDetaljerteVedtaksperioder(): List<DetaljertVedtaksperiodeReiseTilSamling> =
        (
            beregningsresultat.offentligTransport.map { resultat ->
                DetaljertVedtaksperiodeReiseTilSamling(
                    fom = resultat.grunnlag.fom,
                    tom = resultat.grunnlag.tom,
                    beløp = resultat.beløp,
                )
            } +
                beregningsresultat.privatBil.map { resultat ->
                    DetaljertVedtaksperiodeReiseTilSamling(
                        fom = resultat.grunnlag.fom,
                        tom = resultat.grunnlag.tom,
                        beløp = resultat.beløp,
                    )
                }
        ).sortedBy { it.fom }
            .mergeSammenhengende(
                skalMerges = { p1, p2 -> p1.overlapper(p2) },
                merge = { p1, p2 -> p1.copy(fom = minOf(p1.fom, p2.fom), tom = maxOf(p1.tom, p2.tom), beløp = p1.beløp + p2.beløp) },
            ).sortedByDescending { it.fom }
}
