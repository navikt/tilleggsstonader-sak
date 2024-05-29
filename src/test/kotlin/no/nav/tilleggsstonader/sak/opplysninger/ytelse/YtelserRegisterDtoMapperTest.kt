package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import no.nav.tilleggsstonader.kontrakter.ytelse.StatusHentetInformasjon
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.hentetInformasjonAAP
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.hentetInformasjonEnsligForsørger
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.periodeAAP
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.periodeEnsligForsørger
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDto
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelserRegisterDtoMapper.tilDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate.now

class YtelserRegisterDtoMapperTest {

    @Test
    fun `skal sortere perioder etter tom desc`() {
        val aapPeriode1 = periodeAAP(
            fom = now(),
            tom = now(),
        )
        val aapPeriode2 = periodeAAP(fom = now().plusDays(1), tom = now().plusYears(1))

        val aapNullTom1 = periodeAAP(fom = now().plusDays(1), tom = null)
        val aapNullTom2 = periodeAAP(fom = now().plusDays(2), tom = null)

        val efPeriode = periodeEnsligForsørger(fom = now().plusDays(10), tom = now().plusDays(10))
        val perioder = ytelsePerioderDto(
            perioder = listOf(aapNullTom2, aapPeriode1, aapNullTom1, aapPeriode2, efPeriode),
        ).tilDto().perioder

        assertThat(perioder).containsExactly(
            YtelsePeriodeRegisterDto(TypeYtelsePeriode.AAP, fom = aapNullTom2.fom, tom = aapNullTom2.tom),
            YtelsePeriodeRegisterDto(TypeYtelsePeriode.AAP, fom = aapNullTom1.fom, tom = aapNullTom1.tom),
            YtelsePeriodeRegisterDto(TypeYtelsePeriode.AAP, fom = aapPeriode2.fom, tom = aapPeriode2.tom),
            YtelsePeriodeRegisterDto(TypeYtelsePeriode.ENSLIG_FORSØRGER, fom = efPeriode.fom, tom = efPeriode.tom),
            YtelsePeriodeRegisterDto(TypeYtelsePeriode.AAP, fom = aapPeriode1.fom, tom = aapPeriode1.tom),
        )
    }

    @Nested
    inner class HentetInformasjon {

        @Test
        fun `skal mappe status fra aap`() {
            val hentetInformasjon = listOf(hentetInformasjonAAP(status = StatusHentetInformasjon.FEILET))
            val dto = ytelsePerioderDto(hentetInformasjon = hentetInformasjon).tilDto().hentetInformasjon

            assertThat(dto).containsExactly(
                HentetInformasjonDto(type = TypeYtelsePeriode.AAP, status = StatusHentetInformasjon.FEILET),
            )
        }

        @Test
        fun `skal mappe status fra enslig forsørger`() {
            val hentetInformasjon = listOf(hentetInformasjonEnsligForsørger(status = StatusHentetInformasjon.OK))
            val dto = ytelsePerioderDto(hentetInformasjon = hentetInformasjon).tilDto().hentetInformasjon

            assertThat(dto).containsExactly(
                HentetInformasjonDto(type = TypeYtelsePeriode.ENSLIG_FORSØRGER, status = StatusHentetInformasjon.OK),
            )
        }

        @Test
        fun `skal håndtere flere systemer`() {
            val hentetInformasjon = ytelsePerioderDto(
                hentetInformasjon = listOf(
                    hentetInformasjonEnsligForsørger(status = StatusHentetInformasjon.FEILET),
                    hentetInformasjonAAP(status = StatusHentetInformasjon.OK),
                ),
            ).tilDto().hentetInformasjon

            assertThat(hentetInformasjon).containsExactlyInAnyOrder(
                HentetInformasjonDto(type = TypeYtelsePeriode.ENSLIG_FORSØRGER, status = StatusHentetInformasjon.FEILET),
                HentetInformasjonDto(type = TypeYtelsePeriode.AAP, status = StatusHentetInformasjon.OK),
            )
        }
    }
}
