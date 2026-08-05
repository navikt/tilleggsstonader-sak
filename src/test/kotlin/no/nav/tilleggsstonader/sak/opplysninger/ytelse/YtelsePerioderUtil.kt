package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import no.nav.tilleggsstonader.kontrakter.ytelse.GjenståendeDagerFraTelleverk
import no.nav.tilleggsstonader.kontrakter.ytelse.EnsligForsørgerStønadstype
import no.nav.tilleggsstonader.kontrakter.ytelse.ResultatKilde
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto.KildeResultatYtelse
import java.time.LocalDate

object YtelsePerioderUtil {
    fun tomYtelsePerioderDto(): YtelsePerioderDto =
        YtelsePerioderDto(
            perioder = emptyList(),
            kildeResultat = emptyList(),
            perioderHentetFom = LocalDate.now(),
            perioderHentetTom = LocalDate.now(),
        )

    fun ytelsePerioderDtoAAP(): YtelsePerioderDto =
        YtelsePerioderDto(
            perioder = listOf(periodeAAP()),
            kildeResultat = listOf(kildeResultatAAP()),
            perioderHentetFom = LocalDate.now(),
            perioderHentetTom = LocalDate.now(),
        )

    fun ytelsePerioderDtoTiltakspengerTpsak(): YtelsePerioderDto =
        YtelsePerioderDto(
            perioder = listOf(periodeTiltakspengerTpsak()),
            kildeResultat = listOf(kildeResultatTiltakspengerTpsak()),
            perioderHentetFom = LocalDate.now(),
            perioderHentetTom = LocalDate.now(),
        )

    fun ytelsePerioderDto(
        perioder: List<YtelsePeriode> = listOf(periodeAAP(), periodeEnsligForsørger()),
        kildeResultat: List<KildeResultatYtelse> = listOf(kildeResultatAAP(), kildeResultatEnsligForsørger()),
    ): YtelsePerioderDto =
        YtelsePerioderDto(
            perioder = perioder,
            kildeResultat = kildeResultat,
            perioderHentetFom = LocalDate.now(),
            perioderHentetTom = LocalDate.now(),
        )

    fun kildeResultatAAP(resultat: ResultatKilde = ResultatKilde.OK) =
        KildeResultatYtelse(type = TypeYtelsePeriode.AAP, resultat = resultat)

    fun kildeResultatTiltakspengerTpsak(resultat: ResultatKilde = ResultatKilde.OK) =
        KildeResultatYtelse(type = TypeYtelsePeriode.TILTAKSPENGER_TPSAK, resultat = resultat)

    fun kildeResultatEnsligForsørger(resultat: ResultatKilde = ResultatKilde.OK) =
        KildeResultatYtelse(type = TypeYtelsePeriode.ENSLIG_FORSØRGER, resultat = resultat)

    fun periodeAAP(
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate? = LocalDate.now(),
    ): YtelsePeriode = YtelsePeriode.AAP(fom = fom, tom = tom, aapErFerdigAvklart = false)

    fun periodeTiltakspengerTpsak(
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate? = LocalDate.now(),
    ): YtelsePeriode = YtelsePeriode.TiltakspengerTPSak(fom = fom, tom = tom)

    fun periodeTiltakspengerArena(
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate? = LocalDate.now(),
    ): YtelsePeriode = YtelsePeriode.TiltakspengerArena(fom = fom, tom = tom)

    fun periodeDagpenger(
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate? = LocalDate.now(),
        gjenståendeDagerFraTelleverk: GjenståendeDagerFraTelleverk? = null,
    ): YtelsePeriode =
        YtelsePeriode.Dagpenger(
            fom = fom,
            tom = tom,
            gjenståendeDagerFraTelleverk = gjenståendeDagerFraTelleverk,
        )

    fun periodeEnsligForsørger(
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate? = LocalDate.now(),
        erNyttRegelverk: Boolean = false,
    ): YtelsePeriode =
        YtelsePeriode.EnsligForsørger(
            fom = fom,
            tom = tom,
            ensligForsørgerStønadstype = EnsligForsørgerStønadstype.OVERGANGSSTØNAD,
            erNyttRegelverk2026 = erNyttRegelverk,
        )

    fun periodeOmstillingsstønad(
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate? = LocalDate.now(),
    ): YtelsePeriode = YtelsePeriode.Omstillingsstønad(fom = fom, tom = tom)
}
