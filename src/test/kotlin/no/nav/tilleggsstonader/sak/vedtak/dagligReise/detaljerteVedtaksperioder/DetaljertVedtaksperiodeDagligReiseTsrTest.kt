package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
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
            typeAktivtet = TypeAktivitet.GRUPPEAMO,
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
