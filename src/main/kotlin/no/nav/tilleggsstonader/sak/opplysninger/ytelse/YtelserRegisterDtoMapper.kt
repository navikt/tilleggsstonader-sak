package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto
import java.time.LocalDateTime

object YtelserRegisterDtoMapper {

    fun YtelsePerioderDto.tilDto(): YtelserRegisterDto {
        return YtelserRegisterDto(
            perioder = this.perioder.map { YtelsePeriodeRegisterDto(type = it.type, fom = it.fom, tom = it.tom) }
                .sortedByDescending { it.tom },
            hentetInformasjon = hentetInformasjon.map { HentetInformasjonDto(type = it.type, status = it.status) },
            tidspunktHentet = LocalDateTime.now(),
        )
    }
}
