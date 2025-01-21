package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import java.time.LocalDate
import kotlin.collections.plus
import kotlin.collections.sortedBy

object SlåSammenPeriodeGrunnlagYtelseUtil {

    fun List<PeriodeGrunnlagYtelse>.slåSammenOverlappendeEllerPåfølgende(): List<PeriodeGrunnlagYtelse> {
        val (perioderMedTom, perioderUtenTom) = this.partition { it.tom != null }

        val sammenslåttePerioder = perioderMedTom
            .map { PeriodeGrunnlagYtelseHolder(it) }
            .sortedWith(compareBy({ it.ytelse.type }, { it }))
            .mergeSammenhengende(
                skalMerges = { v1, v2 -> v1.kanSlåsSammen(v2) },
                merge = { v1, v2 -> v1.slåSammen(v2) },
            )
            .map { it.ytelse }

        return (sammenslåttePerioder + perioderUtenTom).sortedBy { it.fom }
    }

    private data class PeriodeGrunnlagYtelseHolder(
        val ytelse: PeriodeGrunnlagYtelse,
    ) : Periode<LocalDate> {
        override val fom: LocalDate
            get() = ytelse.fom
        override val tom: LocalDate
            get() = ytelse.tom ?: error("Mangler tom")

        fun slåSammen(other: PeriodeGrunnlagYtelseHolder): PeriodeGrunnlagYtelseHolder =
            PeriodeGrunnlagYtelseHolder(
                ytelse.copy(
                    fom = minOf(ytelse.fom, other.ytelse.fom),
                    tom = maxOf(ytelse.tom!!, other.ytelse.tom!!),
                ),
            )

        fun kanSlåsSammen(other: PeriodeGrunnlagYtelseHolder): Boolean =
            ytelse.type == other.ytelse.type &&
                ytelse.subtype == other.ytelse.subtype &&
                overlapperEllerPåfølgesAv(other)
    }
}
