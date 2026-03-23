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
        fun `ingen grensedatoer returnerer uendret tidslinje`() {
            val tidslinje = Tidslinje(listOf(periode(1 januar 2025, 31 mars 2025)))

            val resultat = tidslinje.splittVedDatoer(emptyList())

            assertThat(resultat.perioder).hasSize(1)
            assertThat(resultat.perioder[0].fom).isEqualTo(1 januar 2025)
            assertThat(resultat.perioder[0].tom).isEqualTo(31 mars 2025)
        }

        @Test
        fun `grensedato midt i periode deler perioden i to`() {
            val tidslinje = Tidslinje(listOf(periode(1 januar 2025, 31 mars 2025)))

            val resultat = tidslinje.splittVedDatoer(listOf(1 mars 2025))

            assertThat(resultat.perioder).hasSize(2)
            assertThat(resultat.perioder[0].fom).isEqualTo(1 januar 2025)
            assertThat(resultat.perioder[0].tom).isEqualTo(28 februar 2025)
            assertThat(resultat.perioder[1].fom).isEqualTo(1 mars 2025)
            assertThat(resultat.perioder[1].tom).isEqualTo(31 mars 2025)
        }

        @Test
        fun `grensedato på starten av periode endrer ikke perioden`() {
            val tidslinje = Tidslinje(listOf(periode(1 mars 2025, 31 mars 2025)))

            val resultat = tidslinje.splittVedDatoer(listOf(1 mars 2025))

            assertThat(resultat.perioder).hasSize(1)
            assertThat(resultat.perioder[0].fom).isEqualTo(1 mars 2025)
        }

        @Test
        fun `grensedato etter alle perioder påvirker ikke resultatet`() {
            val tidslinje = Tidslinje(listOf(periode(1 januar 2025, 28 februar 2025)))

            val resultat = tidslinje.splittVedDatoer(listOf(1 april 2025))

            assertThat(resultat.perioder).hasSize(1)
            assertThat(resultat.perioder[0].tom).isEqualTo(28 februar 2025)
        }

        @Test
        fun `to grensedatoer gir tre perioder`() {
            val tidslinje = Tidslinje(listOf(periode(1 januar 2025, 30 april 2025)))

            val resultat = tidslinje.splittVedDatoer(listOf(1 februar 2025, 1 april 2025))

            assertThat(resultat.perioder).hasSize(3)
            assertThat(resultat.perioder[0].fom).isEqualTo(1 januar 2025)
            assertThat(resultat.perioder[0].tom).isEqualTo(31 januar 2025)
            assertThat(resultat.perioder[1].fom).isEqualTo(1 februar 2025)
            assertThat(resultat.perioder[1].tom).isEqualTo(31 mars 2025)
            assertThat(resultat.perioder[2].fom).isEqualTo(1 april 2025)
            assertThat(resultat.perioder[2].tom).isEqualTo(30 april 2025)
        }

        @Test
        fun `grensedato mellom to perioder beholder begge`() {
            val tidslinje =
                Tidslinje(
                    listOf(
                        periode(1 januar 2025, 31 januar 2025),
                        periode(1 mars 2025, 31 mars 2025),
                    ),
                )

            val resultat = tidslinje.splittVedDatoer(listOf(1 februar 2025))

            assertThat(resultat.perioder).hasSize(2)
            assertThat(resultat.perioder[0].fom).isEqualTo(1 januar 2025)
            assertThat(resultat.perioder[1].fom).isEqualTo(1 mars 2025)
        }
    }

    @Nested
    inner class GrupperVedDatoer {
        @Test
        fun `ingen grensedatoer gir ett segment med alle perioder`() {
            val tidslinje = Tidslinje(listOf(periode(1 januar 2025, 31 mars 2025)))

            val resultat = tidslinje.grupperVedDatoer(emptyList())

            assertThat(resultat).hasSize(1)
            assertThat(resultat[0]).hasSize(1)
        }

        @Test
        fun `grensedato midt i periode gir to segmenter`() {
            val tidslinje = Tidslinje(listOf(periode(1 januar 2025, 31 mars 2025)))

            val resultat = tidslinje.grupperVedDatoer(listOf(1 mars 2025))

            assertThat(resultat).hasSize(2)
            assertThat(resultat[0].single().fom).isEqualTo(1 januar 2025)
            assertThat(resultat[0].single().tom).isEqualTo(28 februar 2025)
            assertThat(resultat[1].single().fom).isEqualTo(1 mars 2025)
            assertThat(resultat[1].single().tom).isEqualTo(31 mars 2025)
        }

        @Test
        fun `grensedato på starten av periode gir ett segment`() {
            val tidslinje = Tidslinje(listOf(periode(1 mars 2025, 31 mars 2025)))

            val resultat = tidslinje.grupperVedDatoer(listOf(1 mars 2025))

            assertThat(resultat).hasSize(1)
            assertThat(resultat[0].single().fom).isEqualTo(1 mars 2025)
        }

        @Test
        fun `to grensedatoer gir tre segmenter`() {
            val tidslinje = Tidslinje(listOf(periode(1 januar 2025, 30 april 2025)))

            val resultat = tidslinje.grupperVedDatoer(listOf(1 februar 2025, 1 april 2025))

            assertThat(resultat).hasSize(3)
            assertThat(resultat[0].single().fom).isEqualTo(1 januar 2025)
            assertThat(resultat[0].single().tom).isEqualTo(31 januar 2025)
            assertThat(resultat[1].single().fom).isEqualTo(1 februar 2025)
            assertThat(resultat[1].single().tom).isEqualTo(31 mars 2025)
            assertThat(resultat[2].single().fom).isEqualTo(1 april 2025)
            assertThat(resultat[2].single().tom).isEqualTo(30 april 2025)
        }
    }
}
