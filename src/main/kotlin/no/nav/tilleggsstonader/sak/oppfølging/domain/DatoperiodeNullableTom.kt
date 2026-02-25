package no.nav.tilleggsstonader.sak.oppfølging.domain

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import java.time.LocalDate

data class DatoperiodeNullableTom(
    val fom: LocalDate,
    val tom: LocalDate?,
) {
    fun overlapperEllerPåfølgesAv(neste: DatoperiodeNullableTom): Boolean = tom != null && neste.fom <= tom.plusDays(1)

    fun merge(other: DatoperiodeNullableTom): DatoperiodeNullableTom =
        copy(
            fom = minOf(fom, other.fom),
            tom = if (tom != null && other.tom != null) maxOf(tom, other.tom) else null,
        )

    fun inneholder(periode: Periode<LocalDate>): Boolean = fom <= periode.fom && (tom == null || tom >= periode.tom)

    fun beregnSnitt(periode: Periode<LocalDate>): Datoperiode? {
        val snittFom = maxOf(fom, periode.fom)
        val snittTom = if (tom != null) minOf(tom, periode.tom) else periode.tom
        return if (snittFom <= snittTom) Datoperiode(snittFom, snittTom) else null
    }
}
