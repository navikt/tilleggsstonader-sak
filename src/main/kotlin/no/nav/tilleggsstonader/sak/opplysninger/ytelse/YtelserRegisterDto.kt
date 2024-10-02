package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import no.nav.tilleggsstonader.kontrakter.ytelse.EnsligForsørgerStønadstype
import no.nav.tilleggsstonader.kontrakter.ytelse.StatusHentetInformasjon
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import java.time.LocalDate
import java.time.LocalDateTime

data class YtelserRegisterDto(
    val perioder: List<YtelsePeriodeRegisterDto>,
    val hentetInformasjon: List<HentetInformasjonDto>,
    val tidspunktHentet: LocalDateTime,
)

data class YtelsePeriodeRegisterDto(
    val type: TypeYtelsePeriode,
    val fom: LocalDate,
    val tom: LocalDate?,
    val aapErFerdigAvklart: Boolean? = null,
    val ensligForsørgerStønadstype: EnsligForsørgerStønadstype? = null,
)

data class HentetInformasjonDto(
    val type: TypeYtelsePeriode,
    val status: StatusHentetInformasjon,
)
