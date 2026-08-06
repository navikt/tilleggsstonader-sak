package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Mapper fra [YtelsePerioderDto] fra integrasjoner til [YtelserRegisterDto].
 */
object YtelserRegisterDtoMapper {
    private val sorteringTomDesc =
        compareByDescending<YtelsePeriodeRegisterDto, LocalDate?>(nullsLast()) { it.tom }
            .thenByDescending { it.fom }

    fun YtelsePerioderDto.tilDto(): YtelserRegisterDto =
        YtelserRegisterDto(
            perioder = mapPerioder(),
            perioderHentetFom = perioderHentetFom,
            perioderHentetTom = perioderHentetTom,
            kildeResultat = kildeResultat.map { KildeResultatYtelseDto(type = it.type, resultat = it.resultat) },
            tidspunktHentet = LocalDateTime.now(),
        )

    private fun YtelsePerioderDto.mapPerioder(): List<YtelsePeriodeRegisterDto> =
        this.perioder
            .map { it.tilDto() }
            .sortedWith(sorteringTomDesc)

    private fun YtelsePeriode.tilDto(): YtelsePeriodeRegisterDto =
        when (this) {
            is YtelsePeriode.AAP ->
                YtelsePeriodeRegisterDto.AAP(
                    fom = fom,
                    tom = tom,
                    aapErFerdigAvklart = aapErFerdigAvklart,
                )
            is YtelsePeriode.Dagpenger ->
                YtelsePeriodeRegisterDto.Dagpenger(
                    fom = fom,
                    tom = tom,
                    gjenståendeDagerFraTelleverk = gjenståendeDagerFraTelleverk,
                )
            is YtelsePeriode.EnsligForsørger ->
                YtelsePeriodeRegisterDto.EnsligForsørger(
                    fom = fom,
                    tom = tom,
                    ensligForsørgerStønadstype = ensligForsørgerStønadstype,
                    erNyttRegelverk2026 = erNyttRegelverk2026,
                )
            is YtelsePeriode.Omstillingsstønad ->
                YtelsePeriodeRegisterDto.Omstillingsstønad(
                    fom = fom,
                    tom = tom,
                )
            is YtelsePeriode.TiltakspengerArena ->
                YtelsePeriodeRegisterDto.TiltakspengerArena(
                    fom = fom,
                    tom = tom,
                )
            is YtelsePeriode.TiltakspengerTPSak ->
                YtelsePeriodeRegisterDto.TiltakspengerTPSak(
                    fom = fom,
                    tom = tom,
                )
        }
}
