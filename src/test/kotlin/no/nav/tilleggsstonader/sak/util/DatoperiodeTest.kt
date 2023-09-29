package no.nav.tilleggsstonader.sak.util

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class DatoperiodeTest {

    @Test
    fun `inneholder returnere true hvis dato er i perioden`() {
        val periode = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 5, 1))

        val inneholder = periode inneholder LocalDate.of(2019, 1, 1)

        assertThat(inneholder).isEqualTo(true)
    }

    @Test
    fun `inneholder returnere true hvis dato ikke er i perioden`() {
        val periode = Datoperiode(LocalDate.of(2019, 1, 2), LocalDate.of(2019, 5, 1))

        val inneholder = periode inneholder LocalDate.of(2019, 1, 1)

        assertThat(inneholder).isEqualTo(false)
    }

    @Test
    fun `snitt returnerer lik periode for like perioder`() {
        val periode1 = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 5, 1))
        val periode2 = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 5, 1))

        val snitt = periode1 snitt periode2

        assertThat(snitt).isEqualTo(periode1)
    }

    @Test
    fun `snitt returnerer null for periode uten overlapp`() {
        val periode1 = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 5, 1))
        val periode2 = Datoperiode(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 12, 1))

        val snitt = periode1 snitt periode2

        assertThat(snitt).isEqualTo(null)
    }

    @Test
    fun `snitt returnerer lik periode uansett hvilken periode som ligger til grunn`() {
        val periode1 = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 5, 1))
        val periode2 = Datoperiode(LocalDate.of(2019, 3, 1), LocalDate.of(2019, 12, 1))

        val snitt1til2 = periode1 snitt periode2
        val snitt2til1 = periode2 snitt periode1

        assertThat(snitt1til2).isEqualTo(snitt2til1)
        assertThat(snitt1til2).isEqualTo(Datoperiode(LocalDate.of(2019, 3, 1), LocalDate.of(2019, 5, 1)))
    }

    @Test
    fun `union returnerer lik periode for like perioder`() {
        val periode1 = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 5, 1))
        val periode2 = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 5, 1))

        val union = periode1 union periode2

        assertThat(union).isEqualTo(periode1)
    }

    @Test
    fun `union returnerer riktig periode for påfølgende perioder`() {
        val periode1 = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 5, 31))
        val periode2 = Datoperiode(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 12, 31))

        val union = periode1 union periode2

        assertThat(union).isEqualTo(Datoperiode(LocalDate.of(2018, 1, 1), LocalDate.of(2019, 5, 31)))
    }

    @Test
    fun `union kaster exception for perioder som ikke følger hverandre eller overlapper`() {
        val periode1 = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 5, 1))
        val periode2 = Datoperiode(LocalDate.of(2018, 1, 1), LocalDate.of(2018, 11, 1))

        assertThatThrownBy {
            periode1 union periode2
        }.hasMessage("Kan ikke lage union av perioder som $periode1 og $periode2 som ikke overlapper eller direkte følger hverandre.")
    }

    @Test
    fun `union returnerer lik periode uansett hvilken periode som ligger til grunn`() {
        val periode1 = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 5, 1))
        val periode2 = Datoperiode(LocalDate.of(2019, 3, 1), LocalDate.of(2019, 12, 1))

        val union1til2 = periode1 union periode2
        val union2til1 = periode2 union periode1

        assertThat(union1til2).isEqualTo(union2til1)
        assertThat(union1til2).isEqualTo(Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 12, 1)))
    }

    @Test
    fun `inneholder returnerer true for periode som helt inneholder innsendt periode`() {
        val periode1 = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 3, 1))
        val periode2 = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 1))

        val inneholder = periode1 inneholder periode2

        assertThat(inneholder).isEqualTo(true)
    }

    @Test
    fun `inneholder returnerer false for periode som stikker utenfor innsendt periode`() {
        val periode1 = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 3, 1))
        val periode2 = Datoperiode(LocalDate.of(2019, 2, 1), LocalDate.of(2019, 4, 1))

        val inneholder = periode1 inneholder periode2

        assertThat(inneholder).isEqualTo(false)
    }

    @Test
    fun `omsluttesAv returnerer true for periode som helt omsluttes av innsendt periode`() {
        val periode1 = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 1))
        val periode2 = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 3, 1))

        val inneholder = periode1 omsluttesAv periode2

        assertThat(inneholder).isEqualTo(true)
    }

    @Test
    fun `omsluttesAv returnerer false for periode som nesten omsluttes av innsendt periode`() {
        val periode1 = Datoperiode(LocalDate.of(2019, 2, 1), LocalDate.of(2019, 4, 1))
        val periode2 = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 3, 1))

        val inneholder = periode1 omsluttesAv periode2

        assertThat(inneholder).isEqualTo(false)
    }

    @Test
    fun `overlapperIStartenAv returnerer true hvis denne perioden overlapper i starten av perioden som sendes inn`() {
        val periodeSomOverlapperStarten = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 1, 1))
        val periode = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 3, 1))

        val overlapperIStartenAv = periodeSomOverlapperStarten overlapperKunIStartenAv periode

        assertThat(overlapperIStartenAv).isEqualTo(true)
    }

    @Test
    fun `overlapperIStartenAv returnerer false hvis denne perioden er lik den som sendes inn`() {
        val periodeSomErLik = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 3, 1))
        val periode = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 3, 1))

        val overlapperIStartenAv = periodeSomErLik overlapperKunIStartenAv periode

        assertThat(overlapperIStartenAv).isEqualTo(false)
    }

    @Test
    fun `overlapperIStartenAv returnerer false hvis denne perioden er før den som sendes inn`() {
        val periodeSomErFør = Datoperiode(LocalDate.of(2018, 9, 1), LocalDate.of(2018, 12, 1))
        val periode = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 3, 1))

        val overlapperIStartenAv = periodeSomErFør overlapperKunIStartenAv periode

        assertThat(overlapperIStartenAv).isEqualTo(false)
    }

    @Test
    fun `overlapperIStartenAv returnerer false hvis denne perioden starter etter den som sendes inn`() {
        val periodeSomErInneI = Datoperiode(LocalDate.of(2018, 9, 2), LocalDate.of(2018, 9, 25))
        val periode = Datoperiode(LocalDate.of(2018, 9, 1), LocalDate.of(2018, 9, 1))

        val overlapperIStartenAv = periodeSomErInneI overlapperKunIStartenAv periode

        assertThat(overlapperIStartenAv).isEqualTo(false)
    }

    @Test
    fun `overlapperISluttenAv returnerer true hvis denne perioden overlapper i slutten av perioden som sendes inn`() {
        val periodeSomOverlapperSlutten = Datoperiode(LocalDate.of(2019, 3, 1), LocalDate.of(2019, 3, 1))
        val periode = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 3, 1))

        val overlapperISluttenAv = periodeSomOverlapperSlutten overlapperKunISluttenAv periode

        assertThat(overlapperISluttenAv).isEqualTo(true)
    }

    @Test
    fun `overlapperISluttenAv returnerer false hvis denne perioden er lik den som sendes inn`() {
        val periodeSomErLik = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 3, 1))
        val periode = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 3, 1))

        val overlapperISluttenAv = periodeSomErLik overlapperKunISluttenAv periode

        assertThat(overlapperISluttenAv).isEqualTo(false)
    }

    @Test
    fun `overlapperISluttenAv returnerer false hvis denne perioden er etter den som sendes inn`() {
        val periodeSomErEtter = Datoperiode(LocalDate.of(2019, 4, 1), LocalDate.of(2019, 4, 1))
        val periode = Datoperiode(LocalDate.of(2019, 1, 1), LocalDate.of(2019, 3, 1))

        val overlapperISluttenAv = periodeSomErEtter overlapperKunISluttenAv periode

        assertThat(overlapperISluttenAv).isEqualTo(false)
    }

    @Test
    fun `overlapperISluttenAv returnerer false hvis denne perioden slutter før den som sendes inn`() {
        val periodeSomErInneI = Datoperiode(LocalDate.of(2018, 9, 2), LocalDate.of(2018, 9, 29))
        val periode = Datoperiode(LocalDate.of(2018, 9, 1), LocalDate.of(2018, 9, 1))

        val overlapperISluttenAv = periodeSomErInneI overlapperKunISluttenAv periode

        assertThat(overlapperISluttenAv).isEqualTo(false)
    }

    @Test
    fun `lengdeIHeleMåneder feiler for perioder som ikke er hele måneder`() {
        val periode = Datoperiode(LocalDate.of(2018, 9, 2), LocalDate.of(2020, 5, 31))

        assertThatThrownBy { periode.lengdeIHeleMåneder() }
            .hasMessage("Forsøk på å beregne lengde i hele måneder for en periode som ikke er hele måneder: 2018-09-02 - 2020-05-31")
    }

    @Test
    fun `lengdeIHeleMåneder returnerer korrekt antall måneder for lange perioder`() {
        val periode = Datoperiode(LocalDate.of(2015, 9, 1), LocalDate.of(2028, 3, 31))

        val lengdeIHeleMåneder = periode.lengdeIHeleMåneder()

        assertThat(lengdeIHeleMåneder).isEqualTo(151)
    }

    @Test
    fun `lengdeIHeleMåneder returnerer korrekt antall måneder for korte perioder`() {
        val periode = Datoperiode(LocalDate.of(2015, 9, 1), LocalDate.of(2015, 12, 31))

        val lengdeIHeleMåneder = periode.lengdeIHeleMåneder()

        assertThat(lengdeIHeleMåneder).isEqualTo(4)
    }
}
