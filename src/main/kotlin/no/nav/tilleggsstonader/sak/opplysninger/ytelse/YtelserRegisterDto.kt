package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tilleggsstonader.kontrakter.ytelse.EnsligForsørgerStønadstype
import no.nav.tilleggsstonader.kontrakter.ytelse.GjenståendeDagerFraTelleverk
import no.nav.tilleggsstonader.kontrakter.ytelse.ResultatKilde
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import java.time.LocalDate
import java.time.LocalDateTime

data class YtelserRegisterDto(
    val perioder: List<YtelsePeriodeRegisterDto>,
    val kildeResultat: List<KildeResultatYtelseDto>,
    val perioderHentetFom: LocalDate,
    val perioderHentetTom: LocalDate,
    val tidspunktHentet: LocalDateTime,
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = YtelsePeriodeRegisterDto.AAP::class, name = "AAP"),
    JsonSubTypes.Type(value = YtelsePeriodeRegisterDto.Dagpenger::class, name = "DAGPENGER"),
    JsonSubTypes.Type(value = YtelsePeriodeRegisterDto.EnsligForsørger::class, name = "ENSLIG_FORSØRGER"),
    JsonSubTypes.Type(value = YtelsePeriodeRegisterDto.Omstillingsstønad::class, name = "OMSTILLINGSSTØNAD"),
    JsonSubTypes.Type(value = YtelsePeriodeRegisterDto.TiltakspengerTPSak::class, name = "TILTAKSPENGER_TPSAK"),
    JsonSubTypes.Type(value = YtelsePeriodeRegisterDto.TiltakspengerArena::class, name = "TILTAKSPENGER_ARENA"),
)
sealed interface YtelsePeriodeRegisterDto {
    val type: TypeYtelsePeriode
        get() =
            when (this) {
                is AAP -> TypeYtelsePeriode.AAP
                is Dagpenger -> TypeYtelsePeriode.DAGPENGER
                is EnsligForsørger -> TypeYtelsePeriode.ENSLIG_FORSØRGER
                is Omstillingsstønad -> TypeYtelsePeriode.OMSTILLINGSSTØNAD
                is TiltakspengerTPSak -> TypeYtelsePeriode.TILTAKSPENGER_TPSAK
                is TiltakspengerArena -> TypeYtelsePeriode.TILTAKSPENGER_ARENA
            }
    val fom: LocalDate
    val tom: LocalDate?

    data class AAP(
        override val fom: LocalDate,
        override val tom: LocalDate?,
        val aapErFerdigAvklart: Boolean,
    ) : YtelsePeriodeRegisterDto

    data class Dagpenger(
        override val fom: LocalDate,
        override val tom: LocalDate?,
        val gjenståendeDagerFraTelleverk: GjenståendeDagerFraTelleverk?,
    ) : YtelsePeriodeRegisterDto

    data class EnsligForsørger(
        override val fom: LocalDate,
        override val tom: LocalDate?,
        val ensligForsørgerStønadstype: EnsligForsørgerStønadstype,
        val erNyttRegelverk2026: Boolean? = null,
    ) : YtelsePeriodeRegisterDto

    data class Omstillingsstønad(
        override val fom: LocalDate,
        override val tom: LocalDate?,
    ) : YtelsePeriodeRegisterDto

    data class TiltakspengerTPSak(
        override val fom: LocalDate,
        override val tom: LocalDate?,
    ) : YtelsePeriodeRegisterDto

    data class TiltakspengerArena(
        override val fom: LocalDate,
        override val tom: LocalDate?,
    ) : YtelsePeriodeRegisterDto
}

data class KildeResultatYtelseDto(
    val type: TypeYtelsePeriode,
    val resultat: ResultatKilde,
)
