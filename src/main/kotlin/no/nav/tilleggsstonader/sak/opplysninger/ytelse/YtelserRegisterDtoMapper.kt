package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto
import java.time.LocalDate
import java.time.LocalDateTime

object YtelserRegisterDtoMapper {

    private val sorteringTomDesc =
        compareByDescending<YtelsePeriodeRegisterDto, LocalDate?>(nullsLast()) { it.tom }
            .thenByDescending { it.fom }

    fun YtelsePerioderDto.tilDto(): YtelserRegisterDto {
        return YtelserRegisterDto(
            perioder = this.perioder.map {
                YtelsePeriodeRegisterDto(
                    type = it.type,
                    fom = it.fom,
                    tom = it.tom,
                    aapErFerdigAvklart = it.aapErFerdigAvklart,
                    ensligForsørgerStønadstype = it.ensligForsørgerStønadstype,
                )
            }.sortedWith(sorteringTomDesc),
            hentetInformasjon = hentetInformasjon.map { HentetInformasjonDto(type = it.type, status = it.status) },
            tidspunktHentet = LocalDateTime.now(),
        )
    }
}
