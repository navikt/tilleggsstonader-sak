package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DetaljertVedtaksperiodeDagligReiseTsrTest {
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
            typeAktivtet = TypeAktivitet.ENKELAMO,
            stønadstype = Stønadstype.DAGLIG_REISE_TSR,
        )

    val vedtaksperiodeFeb =
        DetaljertVedtaksperiodeDagligReise(
            fom = førsteFeb,
            tom = sisteFeb,
            aktivitet = AktivitetType.TILTAK,
            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
            typeDagligReise = TypeDagligReise.OFFENTLIG_TRANSPORT,
            typeAktivtet = TypeAktivitet.GRUPPEAMO,
            stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        )

    @Test
    fun `skal ikke slå sammen aktivteter med ulik typeAktivitet`() {
        val vedtaksperioder =
            listOf(
                vedtaksperiodeJan,
                vedtaksperiodeFeb,
            )
        val resultat = vedtaksperioder.sorterOgMergeSammenhengendeEllerOverlappende()
        assertThat(resultat).isEqualTo(vedtaksperioder)
    }
}
