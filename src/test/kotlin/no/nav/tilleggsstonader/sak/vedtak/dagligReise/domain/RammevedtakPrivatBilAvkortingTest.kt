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
    inner class AvkortReiseTilDato {
        @Test
        fun `reise som slutter før opphørsDato returneres uendret`() {
            val reise = rammeForReiseMedPrivatBil(fom = uke1Fom, tom = uke1Tom)

            assertThat(reise.avkortTilDato(uke2Fom.minusDays(1))).isEqualTo(reise)
        }

        @Test
        fun `reise som starter på opphørsDato fjernes`() {
            val reise = rammeForReiseMedPrivatBil(fom = uke2Fom, tom = uke2Tom)

            assertThat(reise.avkortTilDato(uke2Fom.minusDays(1))).isNull()
        }

        @Test
        fun `reise som starter etter opphørsDato fjernes`() {
            val reise = rammeForReiseMedPrivatBil(fom = uke3Fom, tom = uke3Tom)

            assertThat(reise.avkortTilDato(uke2Fom.minusDays(1))).isNull()
        }

        @Test
        fun `reise som overlapper opphørsDato får sluttdato satt til dagen før opphørsDato`() {
            val reise = rammeForReiseMedPrivatBil(fom = uke1Fom, tom = uke1Tom)
            val opphørsDato = 10 januar 2025 // onsdag i uke 1

            val resultat = reise.avkortTilDato(opphørsDato.minusDays(1))

            assertThat(resultat).isNotNull
            assertThat(resultat!!.grunnlag.fom).isEqualTo(uke1Fom)
            assertThat(resultat.grunnlag.tom).isEqualTo(9 januar 2025)
            assertThat(
                resultat.grunnlag.delperioder
                    .single()
                    .tom,
            ).isEqualTo(9 januar 2025)
        }

        @Test
        fun `opphørsDato på start av uke2 beholder kun uke1`() {
            val reise = reiseMed3Uker()

            val resultat = reise.avkortTilDato(uke2Fom.minusDays(1))

            assertThat(resultat).isNotNull
            assertThat(resultat!!.grunnlag.fom).isEqualTo(uke1Fom)
            assertThat(resultat.grunnlag.tom).isEqualTo(uke1Tom)
            assertThat(resultat.grunnlag.delperioder).hasSize(1)
        }

        @Test
        fun `opphørsDato midt i uke2 kapper uke2 og fjerner uke3`() {
            val reise = reiseMed3Uker()
            val opphørsDato = 15 januar 2025 // onsdag i uke 2

            val resultat = reise.avkortTilDato(opphørsDato.minusDays(1))

            assertThat(resultat).isNotNull
            assertThat(resultat!!.grunnlag.fom).isEqualTo(uke1Fom)
            assertThat(resultat.grunnlag.tom).isEqualTo(14 januar 2025)
            assertThat(resultat.grunnlag.delperioder).hasSize(2)
            assertThat(
                resultat.grunnlag.delperioder
                    .last()
                    .fom,
            ).isEqualTo(uke2Fom)
            assertThat(
                resultat.grunnlag.delperioder
                    .last()
                    .tom,
            ).isEqualTo(14 januar 2025)
        }

        @Test
        fun `satser i avkortet delperiode avkortes ikke`() {
            val sats1 = lagSats(uke2Fom, 15 januar 2025)
            val sats2 = lagSats(16 januar 2025, uke2Tom)
            val reise =
                rammeForReiseMedPrivatBil(
                    fom = uke2Fom,
                    tom = uke2Tom,
                    delperioder = listOf(lagDelperiode(uke2Fom, uke2Tom, listOf(sats1, sats2))),
                )
            val opphørsDato = 17 januar 2025 // torsdag i uke 2

            val resultat = reise.avkortTilDato(opphørsDato.minusDays(1))

            assertThat(resultat).isNotNull
            val avkortetDelperiode = resultat!!.grunnlag.delperioder.single()
            assertThat(avkortetDelperiode.satser).hasSize(2)
            assertThat(avkortetDelperiode.satser.first().tom).isEqualTo(15 januar 2025) // uendret
            assertThat(avkortetDelperiode.satser.last().tom).isEqualTo(uke2Tom) // uendret — satser avkortes ikke
        }

        @Test
        fun `satser forblir uendret etter avkorting av delperiode`() {
            val sats1 = lagSats(uke2Fom, 14 januar 2025)
            val sats2 = lagSats(15 januar 2025, uke2Tom)
            val reise =
                rammeForReiseMedPrivatBil(
                    fom = uke2Fom,
                    tom = uke2Tom,
                    delperioder = listOf(lagDelperiode(uke2Fom, uke2Tom, listOf(sats1, sats2))),
                )
            val opphørsDato = 15 januar 2025

            val resultat = reise.avkortTilDato(opphørsDato.minusDays(1))

            assertThat(resultat).isNotNull
            val avkortetDelperiode = resultat!!.grunnlag.delperioder.single()
            assertThat(avkortetDelperiode.satser).hasSize(2)
            assertThat(avkortetDelperiode.satser.first().tom).isEqualTo(14 januar 2025) // uendret
            assertThat(avkortetDelperiode.satser.last().tom).isEqualTo(uke2Tom) // uendret — satser avkortes ikke
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
