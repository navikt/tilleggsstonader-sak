package no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.læremidler.detaljerteVedtaksperioder.DetaljertVedtaksperiodeLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.detaljerteVedtaksperioder.sorterOgMergeSammenhengende
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DetaljertVedtaksperiodeLæremidlerTest {
    val vedtaksperiodeJan =
        DetaljertVedtaksperiodeLæremidler(
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 1, 31),
            aktivitet = AktivitetType.UTDANNING,
            målgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER,
            antallMåneder = 1,
            studienivå = Studienivå.HØYERE_UTDANNING,
            studieprosent = 100,
            månedsbeløp = 901,
        )

    val vedtaksperiodeFeb =
        DetaljertVedtaksperiodeLæremidler(
            fom = LocalDate.of(2024, 2, 1),
            tom = LocalDate.of(2024, 2, 29),
            aktivitet = AktivitetType.UTDANNING,
            målgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER,
            antallMåneder = 1,
            studienivå = Studienivå.HØYERE_UTDANNING,
            studieprosent = 100,
            månedsbeløp = 901,
        )

    @Test
    fun `skal slå sammen påfølgende perioder med like verdier`() {
        val vedtaksperioder =
            listOf(
                vedtaksperiodeJan,
                vedtaksperiodeFeb,
            )
        val resultat = vedtaksperioder.sorterOgMergeSammenhengende()
        assertThat(resultat).isEqualTo(
            listOf(
                vedtaksperiodeJan.copy(tom = vedtaksperiodeFeb.tom, antallMåneder = 2),
            ),
        )
    }

    @Test
    fun `skal slå sammen flere påfølgende perioder med like verdier`() {
        val vedtaksperioder =
            listOf(
                vedtaksperiodeJan,
                vedtaksperiodeFeb,
                vedtaksperiodeFeb.copy(
                    fom = LocalDate.of(2024, 3, 1),
                    tom = LocalDate.of(2024, 3, 31),
                ),
            )
        val resultat = vedtaksperioder.sorterOgMergeSammenhengende()
        assertThat(resultat).isEqualTo(
            listOf(
                vedtaksperiodeJan.copy(tom = LocalDate.of(2024, 3, 31), antallMåneder = 3),
            ),
        )
    }

    @Test
    fun `skal ikke slå sammen påfølgende perioder med ulike målgruppe og aktivitet`() {
        val vedtaksperioder =
            listOf(
                vedtaksperiodeJan,
                vedtaksperiodeFeb.copy(
                    aktivitet = AktivitetType.TILTAK,
                    målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
                ),
            )
        val resultat = vedtaksperioder.sorterOgMergeSammenhengende()
        assertThat(resultat).isEqualTo(vedtaksperioder)
    }

    @Test
    fun `Sjekker at antall måneder øker dersom man slår sammen flere perioder`() {
        val vedtaksperioder =
            listOf(
                vedtaksperiodeJan,
                vedtaksperiodeFeb,
                vedtaksperiodeFeb.copy(
                    fom = LocalDate.of(2024, 3, 1),
                    tom = LocalDate.of(2024, 3, 31),
                ),
            )
        val resultat = vedtaksperioder.sorterOgMergeSammenhengende()
        assertThat(resultat[0].antallMåneder).isEqualTo(3)
    }

    @Test
    fun `skal ikke slå sammen påfølgende perioder med ulike studienivå og månedsbeløp`() {
        val vedtaksperioder =
            listOf(
                vedtaksperiodeJan,
                vedtaksperiodeFeb.copy(
                    studienivå = Studienivå.VIDEREGÅENDE,
                    månedsbeløp = 451,
                ),
            )
        val resultat = vedtaksperioder.sorterOgMergeSammenhengende()
        assertThat(resultat).isEqualTo(vedtaksperioder)
    }

    @Test
    fun `skal ikke slå sammen påfølgende perioder med ulik studieprosent og månedsbeløp`() {
        val vedtaksperioder =
            listOf(
                vedtaksperiodeJan,
                vedtaksperiodeFeb.copy(
                    studieprosent = 50,
                    månedsbeløp = 451,
                ),
            )
        val resultat = vedtaksperioder.sorterOgMergeSammenhengende()
        assertThat(resultat).isEqualTo(vedtaksperioder)
    }

    @Test
    fun `skal ikke slå sammen like perioder som ikke er påfølgende`() {
        val vedtaksperioder =
            listOf(
                vedtaksperiodeJan,
                vedtaksperiodeFeb.copy(
                    fom = LocalDate.of(2024, 2, 2),
                ),
            )
        val resultat = vedtaksperioder.sorterOgMergeSammenhengende()
        assertThat(resultat).isEqualTo(vedtaksperioder)
    }
}
