package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport.Billettype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.BeregningsresultatForPeriodeDto
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
        DetaljertVedtaksperiodeDagligReise(
            fom = førsteJan,
            tom = sisteJan,
            aktivitet = AktivitetType.TILTAK,
            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
            typeDagligReise = TypeDagligReise.OFFENTLIG_TRANSPORT,
            typeAktivtet = null,
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            beregningsresultat =

                listOf(
                    BeregningsresultatForPeriodeDto(
                        fom = førsteJan,
                        tom = sisteJan,
                        prisEnkeltbillett = 40,
                        prisSyvdagersbillett = null,
                        pris30dagersbillett = 800,
                        antallReisedagerPerUke = 5,
                        beløp = 800,
                        billettdetaljer =
                            mapOf(
                                Billettype.TRETTIDAGERSBILLETT to 1,
                            ),
                        antallReisedager = 22,
                        fraTidligereVedtak = false,
                        brukersNavKontor = "1014",
                    ),
                ),
        )

    val vedtaksperiodeFeb =
        DetaljertVedtaksperiodeDagligReise(
            fom = førsteFeb,
            tom = sisteFeb,
            aktivitet = AktivitetType.TILTAK,
            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
            typeDagligReise = TypeDagligReise.OFFENTLIG_TRANSPORT,
            typeAktivtet = null,
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            beregningsresultat =
                listOf(
                    BeregningsresultatForPeriodeDto(
                        fom = førsteFeb,
                        tom = sisteFeb,
                        prisEnkeltbillett = 40,
                        prisSyvdagersbillett = null,
                        pris30dagersbillett = 800,
                        antallReisedagerPerUke = 5,
                        beløp = 800,
                        billettdetaljer =
                            mapOf(
                                Billettype.TRETTIDAGERSBILLETT to 1,
                            ),
                        antallReisedager = 21,
                        fraTidligereVedtak = false,
                        brukersNavKontor = "1014",
                    ),
                ),
        )

    @Test
    fun `skal slå sammen påfølgende perioder med like verdier`() {
        val vedtaksperioder =
            listOf(
                vedtaksperiodeJan,
                vedtaksperiodeFeb,
            )
        val resultat = vedtaksperioder.sorterOgMergeSammenhengendeEllerOverlappende()
        assertThat(resultat).isEqualTo(
            listOf(
                vedtaksperiodeJan.copy(
                    tom = vedtaksperiodeFeb.tom,
                    beregningsresultat =
                        vedtaksperiodeJan.beregningsresultat.orEmpty() +
                            vedtaksperiodeFeb.beregningsresultat.orEmpty(),
                ),
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
        val resultat = vedtaksperioder.sorterOgMergeSammenhengendeEllerOverlappende()
        assertThat(resultat).isEqualTo(
            listOf(
                vedtaksperiodeJan.copy(
                    tom = LocalDate.of(2024, 3, 31),
                    beregningsresultat =
                        vedtaksperiodeJan.beregningsresultat.orEmpty() +
                            vedtaksperiodeFeb.beregningsresultat.orEmpty() +
                            vedtaksperiodeFeb.beregningsresultat.orEmpty(),
                ),
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
        val resultat = vedtaksperioder.sorterOgMergeSammenhengendeEllerOverlappende()
        assertThat(resultat).isEqualTo(
            listOf(
                vedtaksperiodeJan.copy(
                    tom = vedtaksperiodeFeb.tom,
                    beregningsresultat =
                        vedtaksperiodeJan.beregningsresultat.orEmpty() +
                            vedtaksperiodeFeb.beregningsresultat.orEmpty(),
                ),
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
        val resultat = vedtaksperioder.sorterOgMergeSammenhengendeEllerOverlappende()
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
        val resultat = vedtaksperioder.sorterOgMergeSammenhengendeEllerOverlappende()
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
        val resultat = vedtaksperioder.sorterOgMergeSammenhengendeEllerOverlappende()
        assertThat(resultat).isEqualTo(vedtaksperioder)
    }

    fun DetaljertVedtaksperiodeDagligReise.harLikeMergeVerdier(other: DetaljertVedtaksperiodeDagligReise): Boolean =
        aktivitet == other.aktivitet &&
            målgruppe == other.målgruppe &&
            typeDagligReise == other.typeDagligReise &&
            stønadstype == other.stønadstype &&
            typeAktivtet == other.typeAktivtet
}
