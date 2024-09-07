package no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.util.GrunnlagsdataUtil.lagGrunnlagsdataBarn
import no.nav.tilleggsstonader.sak.util.stønadsperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsperiode.dto.tilDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårsoppsummering.VilkårsoppsummeringUtil.harBarnUnder2ÅrIStønadsperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDate.now

class VilkårsoppsummeringUtilTest {

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
            stønadsperiode(BehandlingId.randomUUID(), fom = fom, tom = tom).tilDto()
    }
}
