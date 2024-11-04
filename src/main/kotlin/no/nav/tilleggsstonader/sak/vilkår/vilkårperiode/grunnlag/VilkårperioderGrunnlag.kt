package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag

import no.nav.tilleggsstonader.kontrakter.aktivitet.Kilde
import no.nav.tilleggsstonader.kontrakter.aktivitet.StatusAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Periode
import no.nav.tilleggsstonader.kontrakter.felles.mergeSammenhengende
import no.nav.tilleggsstonader.kontrakter.felles.overlapperEllerPåfølgesAv
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.collections.plus
import kotlin.collections.sortedBy
import no.nav.tilleggsstonader.kontrakter.ytelse.EnsligForsørgerStønadstype as EnsligForsørgerStønadstypeKontrakter

data class VilkårperioderGrunnlag(
    val aktivitet: GrunnlagAktivitet,
    val ytelse: GrunnlagYtelse,
    val hentetInformasjon: HentetInformasjon,
)

@Table("vilkarperioder_grunnlag")
data class VilkårperioderGrunnlagDomain(
    @Id
    val behandlingId: BehandlingId,
    val grunnlag: VilkårperioderGrunnlag,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)

data class GrunnlagAktivitet(
    val aktiviteter: List<PeriodeGrunnlagAktivitet>,
)

data class GrunnlagYtelse(
    val perioder: List<PeriodeGrunnlagYtelse>,
)

/**
 * Kopi av [no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto]
 */
data class PeriodeGrunnlagAktivitet(
    val id: String,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val type: String,
    val typeNavn: String,
    val status: StatusAktivitet?,
    val statusArena: String?,
    val antallDagerPerUke: Int?,
    val prosentDeltakelse: BigDecimal?,
    val erStønadsberettiget: Boolean?,
    val erUtdanning: Boolean?,
    val arrangør: String?,
    val kilde: Kilde,
)

data class PeriodeGrunnlagYtelse(
    val type: TypeYtelsePeriode,
    val fom: LocalDate,
    val tom: LocalDate?,
    val ensligForsørgerStønadstype: EnsligForsørgerStønadstype? = null,
) {
    companion object {

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
    }
}

data class PeriodeGrunnlagYtelseHolder(
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
        ytelse.type == other.ytelse.type && ytelse.ensligForsørgerStønadstype == other.ytelse.ensligForsørgerStønadstype && overlapperEllerPåfølgesAv(other)
}

data class HentetInformasjon(
    val fom: LocalDate,
    val tom: LocalDate,
    val tidspunktHentet: LocalDateTime,
)

enum class EnsligForsørgerStønadstype {
    OVERGANGSSTØNAD,
    SKOLEPENGER,
}

fun EnsligForsørgerStønadstypeKontrakter.tilDomenetype(): EnsligForsørgerStønadstype {
    return when (this) {
        EnsligForsørgerStønadstypeKontrakter.OVERGANGSSTØNAD -> EnsligForsørgerStønadstype.OVERGANGSSTØNAD
        EnsligForsørgerStønadstypeKontrakter.SKOLEPENGER -> EnsligForsørgerStønadstype.SKOLEPENGER
        EnsligForsørgerStønadstypeKontrakter.BARNETILSYN -> error("BARNETILSYN skal ikke brukes i vilkårperioder grunnlag")
    }
}
