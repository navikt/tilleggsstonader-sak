package no.nav.tilleggsstonader.sak.vedtak.læremidler.beregning

import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class MålgruppeOgAktivitetTest {

    @Nested
    inner class Sortering {

        @Test
        fun `høyest utdanning trumfer`() {
            val høyereUtdanning = målgruppeOgAktivitet(studienivå = Studienivå.HØYERE_UTDANNING)
            val videregående = målgruppeOgAktivitet(studienivå = Studienivå.VIDEREGÅENDE)

            val aktiviteter = listOf(høyereUtdanning, videregående)

            aktiviteter.assertSortIsEqualTo(høyereUtdanning)
        }

        @Test
        fun `høyest studieprosent trumfer`() {
            val prosent100 = målgruppeOgAktivitet(prosent = 100)
            val prosent50 = målgruppeOgAktivitet(prosent = 50)

            val aktiviteter = listOf(prosent100, prosent50)

            aktiviteter.assertSortIsEqualTo(prosent100)
        }

        @Test
        fun `nivå trumfer prosent`() {
            val aktivitet1 = målgruppeOgAktivitet(prosent = 100, studienivå = Studienivå.VIDEREGÅENDE)
            val aktivitet2 = målgruppeOgAktivitet(prosent = 50, studienivå = Studienivå.HØYERE_UTDANNING)

            val aktiviteter = listOf(aktivitet1, aktivitet2)

            aktiviteter.assertSortIsEqualTo(aktivitet2)
        }

        private fun List<MålgruppeOgAktivitet>.assertSortIsEqualTo(expected: MålgruppeOgAktivitet) {
            assertThat(this.minOf { it }).isEqualTo(expected)
            assertThat(this.reversed().minOf { it }).isEqualTo(expected)
        }
    }

    private fun målgruppeOgAktivitet(
        målgruppe: MålgruppeType = MålgruppeType.AAP,
        aktivitet: AktivitetType = AktivitetType.TILTAK,
        studienivå: Studienivå = Studienivå.HØYERE_UTDANNING,
        prosent: Int = 100,
    ) = MålgruppeOgAktivitet(
        målgruppe = målgruppe,
        aktivitet = AktivitetLæremidlerBeregningGrunnlag(
            id = UUID.randomUUID(),
            fom = LocalDate.now(),
            tom = LocalDate.now(),
            type = aktivitet,
            studienivå = studienivå,
            prosent = prosent,
        ),
    )
}
