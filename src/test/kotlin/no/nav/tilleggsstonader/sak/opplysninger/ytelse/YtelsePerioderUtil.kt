package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import no.nav.tilleggsstonader.kontrakter.ytelse.HentetInformasjon
import no.nav.tilleggsstonader.kontrakter.ytelse.StatusHentetInformasjon
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePeriode
import no.nav.tilleggsstonader.kontrakter.ytelse.YtelsePerioderDto
import java.time.LocalDate

object YtelsePerioderUtil {

    fun ytelsePerioderDto(
        perioder: List<YtelsePeriode> = listOf(periodeAAP(), periodeEnsligForsørger()),
        hentetInformasjon: List<HentetInformasjon> = listOf(hentetInformasjonAAP(), hentetInformasjonEnsligForsørger()),
    ): YtelsePerioderDto {
        return YtelsePerioderDto(perioder = perioder, hentetInformasjon = hentetInformasjon)
    }

    fun hentetInformasjonAAP(
        status: StatusHentetInformasjon = StatusHentetInformasjon.OK,
    ) = HentetInformasjon(type = TypeYtelsePeriode.AAP, status = status)

    fun hentetInformasjonEnsligForsørger(
        status: StatusHentetInformasjon = StatusHentetInformasjon.OK,
    ) = HentetInformasjon(type = TypeYtelsePeriode.ENSLIG_FORSØRGER, status = status)

    fun periodeAAP(
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate? = LocalDate.now(),
    ): YtelsePeriode = YtelsePeriode(type = TypeYtelsePeriode.AAP, fom = fom, tom = tom)

    fun periodeEnsligForsørger(
        fom: LocalDate = LocalDate.now(),
        tom: LocalDate? = LocalDate.now(),
    ): YtelsePeriode = YtelsePeriode(type = TypeYtelsePeriode.ENSLIG_FORSØRGER, fom = fom, tom = tom)
}
