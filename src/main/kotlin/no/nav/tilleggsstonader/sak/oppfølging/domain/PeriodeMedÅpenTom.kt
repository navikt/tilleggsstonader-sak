package no.nav.tilleggsstonader.sak.oppfølging.domain

import no.nav.tilleggsstonader.kontrakter.felles.Datoperiode
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import java.time.LocalDate

data class PeriodeMedÅpenTom(
    val fom: LocalDate,
    val tom: LocalDate?,
) {
    fun overlapperEllerPåfølgesAv(neste: PeriodeMedÅpenTom): Boolean = tom != null && neste.fom <= tom.plusDays(1)

    fun merge(other: PeriodeMedÅpenTom): PeriodeMedÅpenTom =
        copy(
            fom = minOf(fom, other.fom),
            tom = if (tom != null && other.tom != null) maxOf(tom, other.tom) else null,
        )

    fun beregnSnitt(periode: Periode<LocalDate>): Datoperiode? {
        val snittFom = maxOf(fom, periode.fom)
        val snittTom = if (tom != null) minOf(tom, periode.tom) else periode.tom
        return if (snittFom <= snittTom) Datoperiode(snittFom, snittTom) else null
    }
}

fun List<PeriodeMedÅpenTom>.mergeSammenhengende(): List<PeriodeMedÅpenTom> =
    this
        .sortedBy { it.fom }
        .fold(mutableListOf()) { resultat, periode ->
            val forrige = resultat.lastOrNull()
            if (forrige != null && forrige.overlapperEllerPåfølgesAv(periode)) {
                resultat.removeLast()
                resultat.add(forrige.merge(periode))
            } else {
                resultat.add(periode)
            }
            resultat
        }
