package no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain

import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil.rammeForReiseMedPrivatBil
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RammevedtakPrivatBilAvkortingTest {
    val uke1Fom = 6 januar 2025
    val uke1Tom = 12 januar 2025
    val uke2Fom = 13 januar 2025
    val uke2Tom = 19 januar 2025
    val uke3Fom = 20 januar 2025
    val uke3Tom = 26 januar 2025

    @Nested
    inner class AvkortEtterDato {
        @Test
        fun `reise med tom lik maksTom returneres uendret`() {
            val reise = rammeForReiseMedPrivatBil(fom = uke1Fom, tom = uke1Tom)

            assertThat(reise.avkortEtterDato(uke1Tom)).isEqualTo(reise)
        }

        @Test
        fun `reise fom etter maksTom fjernes`() {
            val reise = rammeForReiseMedPrivatBil(fom = uke2Fom, tom = uke2Tom)

            assertThat(reise.avkortEtterDato(uke1Tom)).isNull()
        }

        @Test
        fun `reise som avkortes til fom beholder kun én dag`() {
            val reise = rammeForReiseMedPrivatBil(fom = uke1Fom, tom = uke1Tom)

            val resultat = reise.avkortEtterDato(uke1Fom)

            assertThat(resultat?.grunnlag?.fom).isEqualTo(uke1Fom)
            assertThat(resultat?.grunnlag?.tom).isEqualTo(uke1Fom)
        }

        @Test
        fun `reise som overlapper maksTom avkortes til maksTom`() {
            val reise = rammeForReiseMedPrivatBil(fom = uke1Fom, tom = uke1Tom)
            val maksTom = 9 januar 2025

            val resultat = reise.avkortEtterDato(maksTom)

            assertThat(resultat).isNotNull
            assertThat(resultat!!.grunnlag.fom).isEqualTo(uke1Fom)
            assertThat(resultat.grunnlag.tom).isEqualTo(maksTom)
            assertThat(
                resultat.grunnlag.delperioder
                    .single()
                    .tom,
            ).isEqualTo(maksTom)
        }

        @Test
        fun `maksTom på siste dag i uke1 beholder kun uke1`() {
            val reise = reiseMed3Uker()

            val resultat = reise.avkortEtterDato(uke1Tom)

            assertThat(resultat).isNotNull
            assertThat(resultat!!.grunnlag.fom).isEqualTo(uke1Fom)
            assertThat(resultat.grunnlag.tom).isEqualTo(uke1Tom)
            assertThat(resultat.grunnlag.delperioder).hasSize(1)
        }

        @Test
        fun `tre delperioder - én uendret, én forkortet, én fjernet`() {
            val reise = reiseMed3Uker()
            val maksTom = 14 januar 2025

            val resultat = reise.avkortEtterDato(maksTom)!!

            assertThat(resultat.grunnlag.fom).isEqualTo(uke1Fom)
            assertThat(resultat.grunnlag.tom).isEqualTo(maksTom)

            val delperioder = resultat.grunnlag.delperioder
            assertThat(delperioder).hasSize(2)
            assertThat(delperioder[0].fom).isEqualTo(uke1Fom)
            assertThat(delperioder[0].tom).isEqualTo(uke1Tom)
            assertThat(delperioder[1].fom).isEqualTo(uke2Fom)
            assertThat(delperioder[1].tom).isEqualTo(maksTom)
        }

        @Test
        fun `sats som overlapper maksTom avkortes til maksTom`() {
            val sats1 = lagSats(uke2Fom, 15 januar 2025)
            val sats2 = lagSats(16 januar 2025, uke2Tom)
            val reise =
                rammeForReiseMedPrivatBil(
                    fom = uke2Fom,
                    tom = uke2Tom,
                    delperioder = listOf(lagDelperiode(uke2Fom, uke2Tom, listOf(sats1, sats2))),
                )
            val maksTom = 16 januar 2025

            val resultat = reise.avkortEtterDato(maksTom)

            assertThat(resultat).isNotNull
            val avkortetDelperiode = resultat!!.grunnlag.delperioder.single()
            assertThat(avkortetDelperiode.satser).hasSize(2)
            assertThat(avkortetDelperiode.satser.first().tom).isEqualTo(15 januar 2025)
            assertThat(avkortetDelperiode.satser.last().tom).isEqualTo(maksTom)
        }

        @Test
        fun `sats som starter etter maksTom fjernes`() {
            val sats1 = lagSats(uke2Fom, 14 januar 2025)
            val sats2 = lagSats(15 januar 2025, uke2Tom)
            val reise =
                rammeForReiseMedPrivatBil(
                    fom = uke2Fom,
                    tom = uke2Tom,
                    delperioder = listOf(lagDelperiode(uke2Fom, uke2Tom, listOf(sats1, sats2))),
                )
            val maksTom = 14 januar 2025

            val resultat = reise.avkortEtterDato(maksTom)

            assertThat(resultat).isNotNull
            val avkortetDelperiode = resultat!!.grunnlag.delperioder.single()
            assertThat(avkortetDelperiode.satser).hasSize(1)
            assertThat(avkortetDelperiode.satser.single().tom).isEqualTo(maksTom)
        }
    }

    private fun reiseMed3Uker() =
        rammeForReiseMedPrivatBil(
            fom = uke1Fom,
            tom = uke3Tom,
            delperioder =
                listOf(
                    lagDelperiode(uke1Fom, uke1Tom),
                    lagDelperiode(uke2Fom, uke2Tom),
                    lagDelperiode(uke3Fom, uke3Tom),
                ),
        )

    private fun lagDelperiode(
        fom: LocalDate,
        tom: LocalDate,
        satser: List<RammeForReiseMedPrivatBilSatsForDelperiode> = listOf(lagSats(fom, tom)),
    ) = RammeForReiseMedPrivatBilDelperiode(
        fom = fom,
        tom = tom,
        reisedagerPerUke = 5,
        ekstrakostnader = RammeForReiseMedPrivatEkstrakostnader(null, null),
        satser = satser,
    )

    private fun lagSats(
        fom: LocalDate,
        tom: LocalDate,
    ) = RammeForReiseMedPrivatBilSatsForDelperiode(
        fom = fom,
        tom = tom,
        kilometersats = 2.94.toBigDecimal(),
        dagsatsUtenParkering = 100.toBigDecimal(),
        satsBekreftetVedVedtakstidspunkt = true,
    )
}
