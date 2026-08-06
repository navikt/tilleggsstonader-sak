package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.kontrakter.aktivitet.AktivitetArenaDto
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.ytelse.GjenståendeDagerFraTelleverk
import no.nav.tilleggsstonader.kontrakter.ytelse.ResultatKilde
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.GrunnlagYtelse.KildeResultatYtelse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.PeriodeGrunnlagYtelse.YtelseSubtype
import java.time.LocalDate
import java.time.LocalDateTime

data class VilkårperioderGrunnlagDto(
    val aktivitet: GrunnlagAktivitetDto,
    val ytelse: GrunnlagYtelseDto?,
    val hentetInformasjon: HentetInformasjonDto?,
)

data class GrunnlagAktivitetDto(
    val aktiviteter: List<AktivitetArenaDto>,
)

data class GrunnlagYtelseDto(
    val perioder: List<PeriodeGrunnlagYtelseDto>,
    val kildeResultat: List<KildeResultatYtelseDto>,
) {
    data class KildeResultatYtelseDto(
        val type: TypeYtelsePeriode,
        val resultat: ResultatKilde,
    )
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = PeriodeGrunnlagYtelseDto.AAP::class, name = "AAP"),
    JsonSubTypes.Type(value = PeriodeGrunnlagYtelseDto.Dagpenger::class, name = "DAGPENGER"),
    JsonSubTypes.Type(value = PeriodeGrunnlagYtelseDto.EnsligForsørger::class, name = "ENSLIG_FORSØRGER"),
    JsonSubTypes.Type(value = PeriodeGrunnlagYtelseDto.Omstillingsstønad::class, name = "OMSTILLINGSSTØNAD"),
    JsonSubTypes.Type(value = PeriodeGrunnlagYtelseDto.TiltakspengerTPSak::class, name = "TILTAKSPENGER_TPSAK"),
    JsonSubTypes.Type(value = PeriodeGrunnlagYtelseDto.TiltakspengerArena::class, name = "TILTAKSPENGER_ARENA"),
)
sealed interface PeriodeGrunnlagYtelseDto {
    val fom: LocalDate
    val tom: LocalDate?
    val kanYtelseBrukesIBehandling: Boolean
    val type: TypeYtelsePeriode
        get() =
            when (this) {
                is AAP -> TypeYtelsePeriode.AAP
                is Dagpenger -> TypeYtelsePeriode.DAGPENGER
                is EnsligForsørger -> TypeYtelsePeriode.ENSLIG_FORSØRGER
                is Omstillingsstønad -> TypeYtelsePeriode.OMSTILLINGSSTØNAD
                is TiltakspengerArena -> TypeYtelsePeriode.TILTAKSPENGER_ARENA
                is TiltakspengerTPSak -> TypeYtelsePeriode.TILTAKSPENGER_TPSAK
            }

    data class AAP(
        override val fom: LocalDate,
        override val tom: LocalDate?,
        override val subtype: YtelseSubtype? = null,
        override val kanYtelseBrukesIBehandling: Boolean,
    ) : PeriodeGrunnlagYtelseDto,
        HarYtelseSubtype {
        init {
            validerSubtype(type)
        }
    }

    data class Dagpenger(
        override val fom: LocalDate,
        override val tom: LocalDate?,
        val gjenståendeDagerFraTelleverk: GjenståendeDagerFraTelleverk? = null,
        override val kanYtelseBrukesIBehandling: Boolean,
    ) : PeriodeGrunnlagYtelseDto

    data class EnsligForsørger(
        override val fom: LocalDate,
        override val tom: LocalDate?,
        override val subtype: YtelseSubtype? = null,
        val erNyttRegelverk2026: Boolean? = null,
        override val kanYtelseBrukesIBehandling: Boolean,
    ) : PeriodeGrunnlagYtelseDto,
        HarYtelseSubtype {
        init {
            validerSubtype(type)
        }
    }

    data class Omstillingsstønad(
        override val fom: LocalDate,
        override val tom: LocalDate?,
        override val kanYtelseBrukesIBehandling: Boolean,
    ) : PeriodeGrunnlagYtelseDto

    data class TiltakspengerTPSak(
        override val fom: LocalDate,
        override val tom: LocalDate?,
        override val kanYtelseBrukesIBehandling: Boolean,
    ) : PeriodeGrunnlagYtelseDto

    data class TiltakspengerArena(
        override val fom: LocalDate,
        override val tom: LocalDate?,
        override val kanYtelseBrukesIBehandling: Boolean,
    ) : PeriodeGrunnlagYtelseDto
}

data class HentetInformasjonDto(
    val fom: LocalDate,
    val tom: LocalDate,
    val tidspunktHentet: LocalDateTime,
)

fun VilkårperioderGrunnlag.tilDto(stønadstype: Stønadstype) =
    VilkårperioderGrunnlagDto(
        aktivitet = this.aktivitet.tilDto(),
        ytelse = this.ytelse.tilDto(stønadstype = stønadstype),
        hentetInformasjon = this.hentetInformasjon.tilDto(),
    )

fun GrunnlagAktivitet.tilDto() =
    GrunnlagAktivitetDto(
        aktiviteter = this.aktiviteter.map { it.tilDto() },
    )

private fun RegisterAktivitet.tilDto() =
    AktivitetArenaDto(
        id = id,
        fom = fom,
        tom = tom,
        type = type,
        typeNavn = typeNavn,
        status = status,
        statusArena = statusArena,
        antallDagerPerUke = antallDagerPerUke,
        prosentDeltakelse = prosentDeltakelse,
        erStønadsberettiget = erStønadsberettiget,
        erUtdanning = erUtdanning,
        arrangør = arrangør,
        kilde = kilde,
    )

fun GrunnlagYtelse.tilDto(stønadstype: Stønadstype) =
    GrunnlagYtelseDto(
        perioder = this.perioder.map { it.tilDto(stønadstype = stønadstype) },
        kildeResultat = this.kildeResultat.map { it.tilDto() },
    )

private fun KildeResultatYtelse.tilDto() =
    GrunnlagYtelseDto.KildeResultatYtelseDto(
        type = this.type,
        resultat = this.resultat,
    )

fun PeriodeGrunnlagYtelse.tilDto(stønadstype: Stønadstype) =
    when (this) {
        is PeriodeGrunnlagYtelse.AAP ->
            PeriodeGrunnlagYtelseDto.AAP(
                fom = this.fom,
                tom = this.tom,
                kanYtelseBrukesIBehandling = kanYtelseBrukesIBehandling(stønadstype, this),
                subtype = this.subtype,
            )

        is PeriodeGrunnlagYtelse.Dagpenger ->
            PeriodeGrunnlagYtelseDto.Dagpenger(
                fom = this.fom,
                tom = this.tom,
                kanYtelseBrukesIBehandling = kanYtelseBrukesIBehandling(stønadstype, this),
                gjenståendeDagerFraTelleverk = this.gjenståendeDagerFraTelleverk,
            )

        is PeriodeGrunnlagYtelse.EnsligForsørger ->
            PeriodeGrunnlagYtelseDto.EnsligForsørger(
                fom = this.fom,
                tom = this.tom,
                kanYtelseBrukesIBehandling = kanYtelseBrukesIBehandling(stønadstype, this),
                subtype = this.subtype,
                erNyttRegelverk2026 = this.erNyttRegelverk2026,
            )

        is PeriodeGrunnlagYtelse.Omstillingsstønad ->
            PeriodeGrunnlagYtelseDto.Omstillingsstønad(
                fom = this.fom,
                tom = this.tom,
                kanYtelseBrukesIBehandling = kanYtelseBrukesIBehandling(stønadstype, this),
            )

        is PeriodeGrunnlagYtelse.TiltakspengerArena ->
            PeriodeGrunnlagYtelseDto.TiltakspengerArena(
                fom = this.fom,
                tom = this.tom,
                kanYtelseBrukesIBehandling = kanYtelseBrukesIBehandling(stønadstype, this),
            )

        is PeriodeGrunnlagYtelse.TiltakspengerTPSak ->
            PeriodeGrunnlagYtelseDto.TiltakspengerTPSak(
                fom = this.fom,
                tom = this.tom,
                kanYtelseBrukesIBehandling = kanYtelseBrukesIBehandling(stønadstype, this),
            )
    }

fun HentetInformasjon.tilDto() =
    HentetInformasjonDto(
        fom = this.fom,
        tom = this.tom,
        tidspunktHentet = this.tidspunktHentet,
    )
