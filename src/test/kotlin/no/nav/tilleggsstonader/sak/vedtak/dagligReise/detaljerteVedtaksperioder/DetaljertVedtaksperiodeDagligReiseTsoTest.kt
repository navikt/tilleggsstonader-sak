package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DetaljertVedtaksperiodeDagligReiseTsoTest {
    val førsteJan = 1 januar 2024
    val sisteJan = 31 januar 2024
    val førsteFeb = 1 februar 2024
    val sisteFeb = 29 februar 2024

    val vedtaksperiodeJan =
        DetaljertVedtaksperiodeDagligReiseTso(
            fom = førsteJan,
            tom = sisteJan,
            aktivitet = AktivitetType.TILTAK,
            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
            typeDagligReise = TypeDagligReise.OFFENTLIG_TRANSPORT,
        )

    val vedtaksperiodeFeb =
        DetaljertVedtaksperiodeDagligReiseTso(
            fom = førsteFeb,
            tom = sisteFeb,
            aktivitet = AktivitetType.TILTAK,
            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
            typeDagligReise = TypeDagligReise.OFFENTLIG_TRANSPORT,
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
            )
        val resultat = vedtaksperioder.sorterOgMergeSammenhengende()
        assertThat(resultat).isEqualTo(
            listOf(
                vedtaksperiodeJan.copy(tom = LocalDate.of(2024, 3, 31)),
            ),
        )
    }

    @Test
    fun `skal sortere perioder før sammenslåing`() {
        val vedtaksperioder =
            listOf(
                vedtaksperiodeFeb,
                vedtaksperiodeJan,
            )
        val resultat = vedtaksperioder.sorterOgMergeSammenhengende()
        assertThat(resultat).isEqualTo(
            listOf(
                vedtaksperiodeJan.copy(tom = vedtaksperiodeFeb.tom),
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
    fun `skal ikke slå sammen påfølgende perioder med ulike daglig reise typer`() {
        val vedtaksperioder =
            listOf(
                vedtaksperiodeJan,
                vedtaksperiodeFeb.copy(
                    typeDagligReise = TypeDagligReise.PRIVAT_BIL,
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
