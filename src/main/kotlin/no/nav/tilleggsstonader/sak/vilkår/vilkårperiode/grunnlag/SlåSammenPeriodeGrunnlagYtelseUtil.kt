package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import java.time.LocalDate

object SlåSammenPeriodeGrunnlagYtelseUtil {
    fun List<PeriodeGrunnlagYtelse>.slåSammenOverlappendeEllerPåfølgende(): List<PeriodeGrunnlagYtelse> {
        val (perioderMedTom, perioderUtenTom) = this.partition { it.tom != null }

        val sammenslåttePerioder =
            perioderMedTom
                .map { PeriodeGrunnlagYtelseHolder(it) }
                .sortedWith(compareBy({ it.ytelse.type }, { it }))
                .mergeSammenhengende(
                    skalMerges = { v1, v2 -> v1.kanSlåsSammen(v2) },
                    merge = { v1, v2 -> v1.slåSammen(v2) },
                ).map { it.ytelse }

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
                ytelse.slåSammenPeriode(
                    fom = minOf(ytelse.fom, other.ytelse.fom),
                    tom = maxOf(ytelse.tom!!, other.ytelse.tom!!),
                ),
            )

        fun kanSlåsSammen(other: PeriodeGrunnlagYtelseHolder): Boolean =
            ytelse.type == other.ytelse.type &&
                ytelse.subtype() == other.ytelse.subtype() &&
                overlapperEllerPåfølgesAv(other)
    }

    private fun PeriodeGrunnlagYtelse.subtype(): PeriodeGrunnlagYtelse.YtelseSubtype? =
        when (this) {
            is PeriodeGrunnlagYtelse.AAP -> subtype
            is PeriodeGrunnlagYtelse.EnsligForsørger -> subtype
            is PeriodeGrunnlagYtelse.Dagpenger,
            is PeriodeGrunnlagYtelse.Omstillingsstønad,
            is PeriodeGrunnlagYtelse.TiltakspengerArena,
            is PeriodeGrunnlagYtelse.TiltakspengerTPSak,
            -> null
        }

    private fun PeriodeGrunnlagYtelse.slåSammenPeriode(
        fom: LocalDate,
        tom: LocalDate,
    ): PeriodeGrunnlagYtelse =
        when (this) {
            is PeriodeGrunnlagYtelse.AAP -> copy(fom = fom, tom = tom)
            is PeriodeGrunnlagYtelse.Dagpenger -> copy(fom = fom, tom = tom)
            is PeriodeGrunnlagYtelse.EnsligForsørger -> copy(fom = fom, tom = tom)
            is PeriodeGrunnlagYtelse.Omstillingsstønad -> copy(fom = fom, tom = tom)
            is PeriodeGrunnlagYtelse.TiltakspengerArena -> copy(fom = fom, tom = tom)
            is PeriodeGrunnlagYtelse.TiltakspengerTPSak -> copy(fom = fom, tom = tom)
        }
}
