package no.nav.tilleggsstonader.sak.opplysninger.ytelse

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
            perioder =
                this.perioder
                    .map {
                        YtelsePeriodeRegisterDto(
                            type = it.type,
                            fom = it.fom,
                            tom = it.tom,
                            aapErFerdigAvklart = it.aapErFerdigAvklart,
                            ensligForsørgerStønadstype = it.ensligForsørgerStønadstype,
                        )
                    }.sortedWith(sorteringTomDesc),
            kildeResultat = kildeResultat.map { KildeResultatYtelseDto(type = it.type, resultat = it.resultat) },
            tidspunktHentet = LocalDateTime.now(),
        )
}
