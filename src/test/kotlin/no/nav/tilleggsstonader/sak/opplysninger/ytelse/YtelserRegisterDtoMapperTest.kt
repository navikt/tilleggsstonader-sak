package no.nav.tilleggsstonader.sak.opplysninger.ytelse

import no.nav.tilleggsstonader.kontrakter.ytelse.EnsligForsørgerStønadstype
import no.nav.tilleggsstonader.kontrakter.ytelse.GjenståendeDagerFraTelleverk
import no.nav.tilleggsstonader.kontrakter.ytelse.ResultatKilde
import no.nav.tilleggsstonader.kontrakter.ytelse.TypeYtelsePeriode
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.kildeResultatAAP
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.kildeResultatEnsligForsørger
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.periodeAAP
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.periodeDagpenger
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.periodeEnsligForsørger
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelsePerioderUtil.ytelsePerioderDto
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelserRegisterDtoMapper.tilDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate.now

class YtelserRegisterDtoMapperTest {
    @Test
    fun `skal mappe gjenstående dager fra telleverk`() {
        val gjenståendeDagerFraTelleverk = GjenståendeDagerFraTelleverk(dato = now(), antallDager = 5)
        val dagpengerPeriode =
            periodeDagpenger(
                fom = now(),
                tom = now().plusDays(10),
                gjenståendeDagerFraTelleverk = gjenståendeDagerFraTelleverk,
            )

        val dto = ytelsePerioderDto(perioder = listOf(dagpengerPeriode)).tilDto().perioder

        assertThat(dto).containsExactly(
            YtelsePeriodeRegisterDto.Dagpenger(
                fom = dagpengerPeriode.fom,
                tom = dagpengerPeriode.tom,
                gjenståendeDagerFraTelleverk = gjenståendeDagerFraTelleverk,
            ),
        )
    }

    @Test
    fun `skal sortere perioder etter tom desc`() {
        val aapPeriode1 =
            periodeAAP(
                fom = now(),
                tom = now(),
            )
        val aapPeriode2 = periodeAAP(fom = now().plusDays(1), tom = now().plusYears(1))

        val aapNullTom1 = periodeAAP(fom = now().plusDays(1), tom = null)
        val aapNullTom2 = periodeAAP(fom = now().plusDays(2), tom = null)

        val efPeriode = periodeEnsligForsørger(fom = now().plusDays(10), tom = now().plusDays(10), erNyttRegelverk = true)
        val perioder =
            ytelsePerioderDto(
                perioder = listOf(aapNullTom2, aapPeriode1, aapNullTom1, aapPeriode2, efPeriode),
            ).tilDto().perioder

        assertThat(perioder).containsExactly(
            YtelsePeriodeRegisterDto(
                type = TypeYtelsePeriode.AAP,
                fom = aapNullTom2.fom,
                tom = aapNullTom2.tom,
                aapErFerdigAvklart = false,
            ),
            YtelsePeriodeRegisterDto(
                type = TypeYtelsePeriode.AAP,
                fom = aapNullTom1.fom,
                tom = aapNullTom1.tom,
                aapErFerdigAvklart = false,
            ),
            YtelsePeriodeRegisterDto(
                type = TypeYtelsePeriode.AAP,
                fom = aapPeriode2.fom,
                tom = aapPeriode2.tom,
                aapErFerdigAvklart = false,
            ),
            YtelsePeriodeRegisterDto(
                type = TypeYtelsePeriode.ENSLIG_FORSØRGER,
                fom = efPeriode.fom,
                tom = efPeriode.tom,
                ensligForsørgerStønadstype = EnsligForsørgerStønadstype.OVERGANGSSTØNAD,
                erNyttRegelverk2026 = false,
            ),
            YtelsePeriodeRegisterDto(
                type = TypeYtelsePeriode.AAP,
                fom = aapPeriode1.fom,
                tom = aapPeriode1.tom,
                aapErFerdigAvklart = false,
            ),
        )
    }

    @Nested
    inner class HentetInformasjon {
        @Test
        fun `skal mappe status fra aap`() {
            val kildeResultat = listOf(kildeResultatAAP(resultat = ResultatKilde.FEILET))
            val dto = ytelsePerioderDto(kildeResultat = kildeResultat).tilDto().kildeResultat

            assertThat(dto).containsExactly(
                KildeResultatYtelseDto(type = TypeYtelsePeriode.AAP, resultat = ResultatKilde.FEILET),
            )
        }

        @Test
        fun `skal mappe status fra enslig forsørger`() {
            val kildeResultat = listOf(kildeResultatEnsligForsørger(resultat = ResultatKilde.OK))
            val dto = ytelsePerioderDto(kildeResultat = kildeResultat).tilDto().kildeResultat

            assertThat(dto).containsExactly(
                KildeResultatYtelseDto(
                    type = TypeYtelsePeriode.ENSLIG_FORSØRGER,
                    resultat = ResultatKilde.OK,
                ),
            )
        }

        @Test
        fun `skal håndtere flere systemer`() {
            val kildeResultat =
                ytelsePerioderDto(
                    kildeResultat =
                        listOf(
                            kildeResultatEnsligForsørger(resultat = ResultatKilde.FEILET),
                            kildeResultatAAP(resultat = ResultatKilde.OK),
                        ),
                ).tilDto().kildeResultat

            assertThat(kildeResultat).containsExactlyInAnyOrder(
                KildeResultatYtelseDto(
                    type = TypeYtelsePeriode.ENSLIG_FORSØRGER,
                    resultat = ResultatKilde.FEILET,
                ),
                KildeResultatYtelseDto(type = TypeYtelsePeriode.AAP, resultat = ResultatKilde.OK),
            )
        }
    }
}
