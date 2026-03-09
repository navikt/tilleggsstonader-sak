package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport.Billettype
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import org.junit.Test

class DetaljertVedtaksperiodeDagligReiseTsoTest {
    val førsteJan = 1 januar 2024
    val sisteJan = 31 januar 2024
    val førsteFeb = 1 februar 2024
    val sisteFeb = 29 februar 2024

    val vedtaksperiodeJan =
        DetaljertVedtaksperiodeDagligReise(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            typeDagligReise = TypeDagligReise.OFFENTLIG_TRANSPORT,
            detaljertBeregningsperioder =
                listOf(
                    DetaljertBeregningsperioder(
                        fom = førsteJan,
                        tom = sisteJan,
                        prisEnkeltbillett = 30,
                        prisSyvdagersbillett = 150,
                        pris30dagersbillett = 500,
                        beløp = 300,
                        billettdetaljer = mapOf(Billettype.ENKELTBILLETT to 20),
                        antallReisedager = 20,
                        antallReisedagerPerUke = 3,
                    ),
                ),
        )

    val vedtaksperiodeFeb =
        DetaljertVedtaksperiodeDagligReise(
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
            typeDagligReise = TypeDagligReise.OFFENTLIG_TRANSPORT,
            detaljertBeregningsperioder =
                listOf(
                    DetaljertBeregningsperioder(
                        fom = førsteFeb,
                        tom = sisteFeb,
                        prisEnkeltbillett = 30,
                        prisSyvdagersbillett = 150,
                        pris30dagersbillett = 500,
                        beløp = 300,
                        billettdetaljer = mapOf(Billettype.ENKELTBILLETT to 20),
                        antallReisedager = 20,
                        antallReisedagerPerUke = 3,
                    ),
                ),
        )

    @Test
    fun `skal slå sammen påfølgende perioder med like verdier`() {
    }
}
