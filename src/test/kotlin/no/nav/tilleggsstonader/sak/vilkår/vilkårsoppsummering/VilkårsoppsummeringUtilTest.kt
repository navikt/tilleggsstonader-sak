package no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering

import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagGrunnlagsdataBarn
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering.VilkårsoppsummeringUtil.harBarnUnder2ÅrIStønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering.VilkårsoppsummeringUtil.utledAlderNårStønadsperiodeBegynner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDate.now
import java.util.UUID

class VilkårsoppsummeringUtilTest {
    @Nested
    inner class UtledAlderNårStønadsperiodeBegynner {

        @Test
        fun `skal svare med null hvis man mangler dato for stønadsperiode`() {
            assertThat(utledAlderNårStønadsperiodeBegynner(null, null)).isNull()
            assertThat(utledAlderNårStønadsperiodeBegynner(now(), null)).isNull()
            assertThat(utledAlderNårStønadsperiodeBegynner(now().minusYears(3), null)).isNull()
            assertThat(utledAlderNårStønadsperiodeBegynner(now().minusDays(3), null)).isNull()
        }

        @Test
        fun `skal svare med 0 hvis barnet mangler fødselsdato då vi ikke vet alderen til barnet`() {
            assertThat(utledAlderNårStønadsperiodeBegynner(null, now())).isEqualTo(0)

            assertThat(utledAlderNårStønadsperiodeBegynner(null, now().minusYears(3))).isEqualTo(0)
            assertThat(utledAlderNårStønadsperiodeBegynner(null, now().minusDays(3))).isEqualTo(0)

            assertThat(utledAlderNårStønadsperiodeBegynner(null, now().plusYears(3))).isEqualTo(0)
            assertThat(utledAlderNårStønadsperiodeBegynner(null, now().plusDays(3))).isEqualTo(0)
        }

        @Test
        fun `skal returnere barnets alder vid tidspunktet for første stønadsperioden`() {
            assertThat(utledAlderNårStønadsperiodeBegynner(now(), now())).isEqualTo(0)

            val toÅrSiden = now().minusYears(2)
            assertThat(utledAlderNårStønadsperiodeBegynner(toÅrSiden, now())).isEqualTo(2)
            assertThat(utledAlderNårStønadsperiodeBegynner(toÅrSiden.minusDays(1), now())).isEqualTo(2)
            assertThat(utledAlderNårStønadsperiodeBegynner(toÅrSiden.plusDays(1), now())).isEqualTo(1)
        }
    }

    /*
    | fødsel
    # 2 år
    2021   2022   2023    2024
             <--------------->
    |      #                        Barn er 2 år før periode
        |           #               Barn er 2 år under periode
                        |      #    Barn er 2 år under periode
                                 |  Barn fødes etter periode
     */
    @Nested
    inner class StønadsperiodeBarnUnder2ÅrTest {

        @Test
        fun `barn uten fødselsdato er under 2 år`() {
            assertThat(
                harBarnUnder2ÅrIStønadsperiode(
                    listOf(barn(null)),
                    listOf(stønadsperiode(fom = now(), tom = now())),
                ),
            ).isTrue
        }

        @Test
        fun `barn er 2 år før perioden`() {
            assertThat(
                harBarnUnder2ÅrIStønadsperiode(
                    listOf(barn(LocalDate.of(2020, 1, 1))),
                    listOf(stønadsperiode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1))),
                ),
            ).isFalse
        }

        @Test
        fun `barn født før perioden, men 2 år under perioden`() {
            assertThat(
                harBarnUnder2ÅrIStønadsperiode(
                    listOf(barn(LocalDate.of(2020, 1, 1))),
                    listOf(stønadsperiode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 1))),
                ),
            ).isTrue
            assertThat(
                harBarnUnder2ÅrIStønadsperiode(
                    listOf(barn(LocalDate.of(2020, 1, 1))),
                    listOf(stønadsperiode(LocalDate.of(2021, 1, 1), LocalDate.of(2024, 1, 1))),
                ),
            ).isTrue
        }

        @Test
        fun `barn født i perioden`() {
            assertThat(
                harBarnUnder2ÅrIStønadsperiode(
                    listOf(barn(LocalDate.of(2020, 1, 1))),
                    listOf(stønadsperiode(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1))),
                ),
            ).isTrue

            assertThat(
                harBarnUnder2ÅrIStønadsperiode(
                    listOf(barn(LocalDate.of(2021, 1, 1))),
                    listOf(stønadsperiode(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1))),
                ),
            ).isTrue

            assertThat(
                harBarnUnder2ÅrIStønadsperiode(
                    listOf(barn(LocalDate.of(2015, 1, 1))),
                    listOf(stønadsperiode(LocalDate.of(2000, 1, 1), LocalDate.of(2030, 1, 1))),
                ),
            ).isTrue
        }

        @Test
        fun `barn født etter perioden`() {
            assertThat(
                harBarnUnder2ÅrIStønadsperiode(
                    listOf(barn(LocalDate.of(2021, 1, 1))),
                    listOf(stønadsperiode(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1))),
                ),
            ).isFalse
            assertThat(
                harBarnUnder2ÅrIStønadsperiode(
                    listOf(barn(LocalDate.of(2028, 1, 1))),
                    listOf(stønadsperiode(LocalDate.of(2020, 1, 1), LocalDate.of(2021, 1, 1))),
                ),
            ).isFalse
        }

        private fun barn(localDate: LocalDate?) = lagGrunnlagsdataBarn(fødselsdato = localDate)

        private fun stønadsperiode(fom: LocalDate, tom: LocalDate) =
            stønadsperiode(UUID.randomUUID(), fom = fom, tom = tom).tilDto()
    }
}
