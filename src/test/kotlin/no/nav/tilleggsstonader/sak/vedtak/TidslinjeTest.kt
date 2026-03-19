package no.nav.tilleggsstonader.sak.vedtak

import no.nav.tilleggsstonader.libs.utils.dato.april
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.felles.Tidslinje
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TidslinjeTest {
    private fun periode(
        fom: LocalDate,
        tom: LocalDate,
    ) = VedtaksperiodeBeregning(
        fom = fom,
        tom = tom,
        målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
        aktivitet = AktivitetType.TILTAK,
    )

    @Nested
    inner class SplittVedDatoer {
        @Test
        fun `ingen grensedatoer gir ett segment med alle perioder`() {
            val tidslinje = Tidslinje(listOf(periode(1 januar 2025, 31 mars 2025)))

            val segmenter = tidslinje.splittVedDatoer(emptyList())

            assertThat(segmenter).hasSize(1)
            assertThat(segmenter[0].perioder).hasSize(1)
            assertThat(segmenter[0].perioder[0].fom).isEqualTo(1 januar 2025)
            assertThat(segmenter[0].perioder[0].tom).isEqualTo(31 mars 2025)
        }

        @Test
        fun `grensedato midt i periode deler perioden i to segmenter`() {
            val tidslinje = Tidslinje(listOf(periode(1 januar 2025, 31 mars 2025)))

            val segmenter = tidslinje.splittVedDatoer(listOf(1 mars 2025))

            assertThat(segmenter).hasSize(2)
            assertThat(segmenter[0].perioder[0].fom).isEqualTo(1 januar 2025)
            assertThat(segmenter[0].perioder[0].tom).isEqualTo(28 februar 2025)
            assertThat(segmenter[1].perioder[0].fom).isEqualTo(1 mars 2025)
            assertThat(segmenter[1].perioder[0].tom).isEqualTo(31 mars 2025)
        }

        @Test
        fun `grensedato på starten av periode gir ett segment`() {
            val tidslinje = Tidslinje(listOf(periode(1 mars 2025, 31 mars 2025)))

            val segmenter = tidslinje.splittVedDatoer(listOf(1 mars 2025))

            assertThat(segmenter).hasSize(1)
            assertThat(segmenter[0].perioder[0].fom).isEqualTo(1 mars 2025)
        }

        @Test
        fun `grensedato etter alle perioder påvirker ikke resultatet`() {
            val tidslinje = Tidslinje(listOf(periode(1 januar 2025, 28 februar 2025)))

            val segmenter = tidslinje.splittVedDatoer(listOf(1 april 2025))

            assertThat(segmenter).hasSize(1)
            assertThat(segmenter[0].perioder[0].tom).isEqualTo(28 februar 2025)
        }

        @Test
        fun `to grensedatoer gir tre segmenter`() {
            val tidslinje = Tidslinje(listOf(periode(1 januar 2025, 30 april 2025)))

            val segmenter = tidslinje.splittVedDatoer(listOf(1 februar 2025, 1 april 2025))

            assertThat(segmenter).hasSize(3)
            assertThat(segmenter[0].perioder[0].fom).isEqualTo(1 januar 2025)
            assertThat(segmenter[0].perioder[0].tom).isEqualTo(31 januar 2025)
            assertThat(segmenter[1].perioder[0].fom).isEqualTo(1 februar 2025)
            assertThat(segmenter[1].perioder[0].tom).isEqualTo(31 mars 2025)
            assertThat(segmenter[2].perioder[0].fom).isEqualTo(1 april 2025)
            assertThat(segmenter[2].perioder[0].tom).isEqualTo(30 april 2025)
        }

        @Test
        fun `grensedato mellom to perioder beholder begge i hvert sitt segment`() {
            val tidslinje =
                Tidslinje(
                    listOf(
                        periode(1 januar 2025, 31 januar 2025),
                        periode(1 mars 2025, 31 mars 2025),
                    ),
                )

            val segmenter = tidslinje.splittVedDatoer(listOf(1 februar 2025))

            assertThat(segmenter).hasSize(2)
            assertThat(segmenter[0].perioder).hasSize(1)
            assertThat(segmenter[0].perioder[0].fom).isEqualTo(1 januar 2025)
            assertThat(segmenter[1].perioder).hasSize(1)
            assertThat(segmenter[1].perioder[0].fom).isEqualTo(1 mars 2025)
        }
    }
}
