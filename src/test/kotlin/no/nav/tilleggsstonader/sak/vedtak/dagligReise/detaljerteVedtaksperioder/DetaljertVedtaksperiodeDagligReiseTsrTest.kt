package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport.Billettype
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise

class DetaljertVedtaksperiodeDagligReiseTsrTest {
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
//
//    @Test
//    fun `skal ikke slå sammen aktivteter med ulik typeAktivitet`() {
//        val vedtaksperioder =
//            listOf(
//                vedtaksperiodeJan,
//                vedtaksperiodeFeb,
//            )
//        val resultat = vedtaksperioder.sorterOgMergeSammenhengendeEllerOverlappende()
//        assertThat(resultat).isEqualTo(vedtaksperioder)
//    }
}
