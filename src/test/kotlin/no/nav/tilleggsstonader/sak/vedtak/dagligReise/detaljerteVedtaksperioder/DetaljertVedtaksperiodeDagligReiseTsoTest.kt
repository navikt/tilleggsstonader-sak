package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport.Billettype
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise

class DetaljertVedtaksperiodeDagligReiseTsoTest {
    val førsteJan = 1 januar 2024
    val sisteJan = 31 januar 2024
    val førsteFeb = 1 februar 2024
    val sisteFeb = 29 februar 2024

    val vedtaksperiodeJan =
        DetaljertBeregningsperioderDagligReise(
            fom = førsteJan,
            tom = sisteJan,
            prisEnkeltbillett = 30,
            prisSyvdagersbillett = 150,
            pris30dagersbillett = 500,
            beløp = 300,
            billettdetaljer = mapOf(Billettype.ENKELTBILLETT to 20),
            antallReisedager = 20,
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            antallReisedagerPerUke = 3,
            typeDagligReise = TypeDagligReise.OFFENTLIG_TRANSPORT,
        )

    val vedtaksperiodeFeb =
        DetaljertBeregningsperioderDagligReise(
            fom = førsteFeb,
            tom = sisteFeb,
            prisEnkeltbillett = 30,
            prisSyvdagersbillett = 150,
            pris30dagersbillett = 500,
            beløp = 300,
            billettdetaljer = mapOf(Billettype.ENKELTBILLETT to 20),
            antallReisedager = 20,
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            antallReisedagerPerUke = 3,
            typeDagligReise = TypeDagligReise.OFFENTLIG_TRANSPORT,
        )

// //    @Test
// //    fun `skal slå sammen påfølgende perioder med like verdier`() {
// //        val vedtaksperioder =
// //            listOf(
// //                vedtaksperiodeJan,
// //                vedtaksperiodeFeb,
// //            )
// //        val resultat = vedtaksperioder.sorterOgMergeSammenhengendeEllerOverlappende()
// //        assertThat(resultat).isEqualTo(
// //            listOf(
// //                vedtaksperiodeJan.copy(tom = vedtaksperiodeFeb.tom),
// //            ),
// //        )
// //    }
//
//    @Test
//    fun `skal slå sammen flere påfølgende perioder med like verdier`() {
//        val vedtaksperioder =
//            listOf(
//                vedtaksperiodeJan,
//                vedtaksperiodeFeb,
//                vedtaksperiodeFeb.copy(
//                    fom = LocalDate.of(2024, 3, 1),
//                    tom = LocalDate.of(2024, 3, 31),
//                ),
//            )
//        val resultat = vedtaksperioder.sorterOgMergeSammenhengendeEllerOverlappende()
//        assertThat(resultat).isEqualTo(
//            listOf(
//                vedtaksperiodeJan.copy(tom = LocalDate.of(2024, 3, 31)),
//            ),
//        )
//    }
//
//    @Test
//    fun `skal sortere perioder før sammenslåing`() {
//        val vedtaksperioder =
//            listOf(
//                vedtaksperiodeFeb,
//                vedtaksperiodeJan,
//            )
//        val resultat = vedtaksperioder.sorterOgMergeSammenhengendeEllerOverlappende()
//        assertThat(resultat).isEqualTo(
//            listOf(
//                vedtaksperiodeJan.copy(tom = vedtaksperiodeFeb.tom),
//            ),
//        )
//    }
//
//    @Test
//    fun `skal ikke slå sammen påfølgende perioder med ulike målgruppe og aktivitet`() {
//        val vedtaksperioder =
//            listOf(
//                vedtaksperiodeJan,
//                vedtaksperiodeFeb.copy(
//                    aktivitet = AktivitetType.UTDANNING,
//                    målgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER,
//                ),
//            )
//        val resultat = vedtaksperioder.sorterOgMergeSammenhengendeEllerOverlappende()
//        assertThat(resultat).isEqualTo(vedtaksperioder)
//    }
//
//    @Test
//    fun `skal ikke slå sammen påfølgende perioder med ulike daglig reise typer`() {
//        val vedtaksperioder =
//            listOf(
//                vedtaksperiodeJan,
//                vedtaksperiodeFeb.copy(
//                    typeDagligReise = TypeDagligReise.PRIVAT_BIL,
//                ),
//            )
//        val resultat = vedtaksperioder.sorterOgMergeSammenhengendeEllerOverlappende()
//        assertThat(resultat).isEqualTo(vedtaksperioder)
//    }
//
//    @Test
//    fun `skal ikke slå sammen like perioder som ikke er påfølgende`() {
//        val vedtaksperioder =
//            listOf(
//                vedtaksperiodeJan,
//                vedtaksperiodeFeb.copy(
//                    fom = LocalDate.of(2024, 2, 2),
//                ),
//            )
//        val resultat = vedtaksperioder.sorterOgMergeSammenhengendeEllerOverlappende()
//        assertThat(resultat).isEqualTo(vedtaksperioder)
//    }
}
