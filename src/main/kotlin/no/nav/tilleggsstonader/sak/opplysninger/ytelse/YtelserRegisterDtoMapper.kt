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
            .map {
                YtelsePeriodeRegisterDto(
                    type = it.type,
                    fom = it.fom,
                    tom = it.tom,
                    aapErFerdigAvklart = if (it is YtelsePeriode.AAP) it.aapErFerdigAvklart else null,
                    ensligForsørgerStønadstype = if (it is YtelsePeriode.EnsligForsørger) it.ensligForsørgerStønadstype else null,
                    erNyttRegelverk2026 = if (it is YtelsePeriode.EnsligForsørger) it.erNyttRegelverk2026 else null,
                )
            }.sortedWith(sorteringTomDesc)
}
