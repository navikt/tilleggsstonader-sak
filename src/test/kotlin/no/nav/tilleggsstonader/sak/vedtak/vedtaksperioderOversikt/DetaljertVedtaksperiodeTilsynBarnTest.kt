package no.nav.tilleggsstonader.sak.vedtak.vedtaksperioderOversikt

import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.detaljerteVedtaksperioder.DetaljertVedtaksperiodeTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.detaljerteVedtaksperioder.sorterOgMergeSammenhengende
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DetaljertVedtaksperiodeTilsynBarnTest {
    val vedtaksperiodeJan =
        DetaljertVedtaksperiodeTilsynBarn(
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 1, 31),
            aktivitet = AktivitetType.TILTAK,
            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
            antallBarn = 1,
            totalMånedsUtgift = 1000,
        )

    val vedtaksperiodeFeb =
        DetaljertVedtaksperiodeTilsynBarn(
            fom = LocalDate.of(2024, 2, 1),
            tom = LocalDate.of(2024, 2, 29),
            aktivitet = AktivitetType.TILTAK,
            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
            antallBarn = 1,
            totalMånedsUtgift = 1000,
        )

    @Test
    fun `skal slå sammen to påfølgende perioder med like verdier`() {
        val vedtaksperioder =
            listOf(
                vedtaksperiodeJan,
                vedtaksperiodeFeb,
            )
        val resultat = vedtaksperioder.sorterOgMergeSammenhengende()
        assertThat(resultat).isEqualTo(
            listOf(
                vedtaksperiodeJan.copy(tom = vedtaksperiodeFeb.tom),
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
                vedtaksperiodeFeb.copy(
                    fom = LocalDate.of(2024, 4, 1),
                    tom = LocalDate.of(2024, 4, 30),
                ),
            )
        val resultat = vedtaksperioder.sorterOgMergeSammenhengende()
        assertThat(resultat).isEqualTo(
            listOf(
                vedtaksperiodeJan.copy(tom = LocalDate.of(2024, 4, 30)),
            ),
        )
    }

    @Test
    fun `skal ikke slå sammen påfølgende perioder med ulike målgruppe og aktivitet`() {
        val vedtaksperioder =
            listOf(
                vedtaksperiodeJan,
                vedtaksperiodeFeb.copy(
                    aktivitet = AktivitetType.UTDANNING,
                    målgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER,
                ),
            )
        val resultat = vedtaksperioder.sorterOgMergeSammenhengende()
        assertThat(resultat).isEqualTo(vedtaksperioder)
    }

    @Test
    fun `skal ikke slå sammen påfølgende perioder med ulike totalMånedsUtgift`() {
        val vedtaksperioder =
            listOf(
                vedtaksperiodeJan,
                vedtaksperiodeFeb.copy(
                    totalMånedsUtgift = 2000,
                ),
            )
        val resultat = vedtaksperioder.sorterOgMergeSammenhengende()
        assertThat(resultat).isEqualTo(vedtaksperioder)
    }

    @Test
    fun `skal ikke slå sammen påfølgende perioder med ulikt antall barn`() {
        val vedtaksperioder =
            listOf(
                vedtaksperiodeJan,
                vedtaksperiodeFeb.copy(
                    antallBarn = 2,
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
