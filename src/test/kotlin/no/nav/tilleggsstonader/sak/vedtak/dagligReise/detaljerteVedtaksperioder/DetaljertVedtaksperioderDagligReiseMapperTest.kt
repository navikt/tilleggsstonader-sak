package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport.Billettype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder.DetaljertVedtaksperioderDagligReiseMapper.finnDetaljerteVedtaksperioderDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.TypeDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import java.util.UUID.randomUUID

class DetaljertVedtaksperioderDagligReiseMapperTest {
    private val førsteJanuar = 1 januar 2024
    private val sisteJanuar = 31 januar 2024
    private val førsteFeb = 1 februar 2024
    private val sisteFeb = 29 februar 2024

    @Test
    fun `skal mappe TSO vedtak til detaljerte vedtaksperioder`() {
        val tsoVedtak =
            innvilgelse(
                InnvilgelseDagligReise(
                    vedtaksperioder = defaultVedtaksperioder,
                    beregningsresultat = defaultBeregningsresultat,
                    rammevedtakPrivatBil = null,
                    beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                ),
            )
        val tsrVedtak =
            innvilgelse(
                InnvilgelseDagligReise(
                    vedtaksperioder = defaultVedtaksperioder,
                    beregningsresultat = defaultBeregningsresultat,
                    rammevedtakPrivatBil = null,
                    beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                ),
            )

        val resultat =
            finnDetaljerteVedtaksperioderDagligReise(
                vedtaksdataTso = tsoVedtak.data,
                vedtaksdataTsr = tsrVedtak.data,
                adresserTso = mapOf(dummyReiseId to "adresseTso"),
                adresserTsr = mapOf(dummyReiseId to "adresseTsr"),
            )
        val forventetTso =
            DetaljertVedtaksperiodeDagligReise(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
                typeDagligReise = TypeDagligReise.OFFENTLIG_TRANSPORT,
                detaljertBeregningsperioder =
                    listOf(
                        DetaljertBeregningsperioder(
                            fom = førsteFeb,
                            tom = sisteFeb,
                            prisEnkeltbillett = 50,
                            prisSyvdagersbillett = 300,
                            pris30dagersbillett = 1000,
                            beløp = 1000,
                            billettdetaljer = mapOf(Billettype.TRETTIDAGERSBILLETT to 1000),
                            antallReisedager = 20,
                            antallReisedagerPerUke = 5,
                        ),
                        DetaljertBeregningsperioder(
                            fom = førsteJanuar,
                            tom = sisteJanuar,
                            prisEnkeltbillett = 50,
                            prisSyvdagersbillett = 300,
                            pris30dagersbillett = 1000,
                            beløp = 1000,
                            billettdetaljer = mapOf(Billettype.TRETTIDAGERSBILLETT to 1000),
                            antallReisedager = 20,
                            antallReisedagerPerUke = 5,
                        ),
                    ),
                adresse = "adresseTso",
            )
        val forventetTsr = forventetTso.copy(stønadstype = Stønadstype.DAGLIG_REISE_TSR, adresse = "adresseTsr")
        assertThat(resultat).containsExactly(forventetTso, forventetTsr)
    }

    @Test
    fun `skal opprette en detaljert vedtaksperiode for hver adresse`() {
        val førsteReiseId = ReiseId.random()
        val andreReiseId = ReiseId.random()
        val tsoVedtak =
            innvilgelse(
                InnvilgelseDagligReise(
                    vedtaksperioder = defaultVedtaksperioder,
                    beregningsresultat =
                        lagBeregningsresultatMedToReiser(
                            førsteReiseId = førsteReiseId,
                            andreReiseId = andreReiseId,
                            fom = førsteJanuar,
                            tom = sisteJanuar,
                        ),
                    rammevedtakPrivatBil = null,
                    beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
                ),
            )

        val resultat =
            finnDetaljerteVedtaksperioderDagligReise(
                vedtaksdataTso = tsoVedtak.data,
                vedtaksdataTsr = null,
                adresserTso =
                    mapOf(
                        førsteReiseId to "adresse 1",
                        andreReiseId to "adresse 2",
                    ),
                adresserTsr = emptyMap(),
            )

        assertThat(resultat).hasSize(2)
        assertThat(resultat.map { it.adresse }).containsExactly("adresse 1", "adresse 2")
    }

    private fun innvilgelse(data: InnvilgelseDagligReise = defaultInnvilgelseDagligReise) =
        GeneriskVedtak(
            behandlingId = BehandlingId.random(),
            type = TypeVedtak.INNVILGELSE,
            data = data,
            gitVersjon = Applikasjonsversjon.versjon,
            tidligsteEndring = null,
            opphørsdato = null,
        )

    private fun lagBeregningsresultatMedToReiser(
        førsteReiseId: ReiseId = dummyReiseId,
        andreReiseId: ReiseId = ReiseId.random(),
        fom: LocalDate,
        tom: LocalDate,
    ) = BeregningsresultatDagligReise(
        offentligTransport =
            BeregningsresultatOffentligTransport(
                reiser =
                    listOf(
                        BeregningsresultatForReise(
                            reiseId = førsteReiseId,
                            perioder =
                                listOf(
                                    beregningsresultatForPeriode(fom, tom),
                                ),
                        ),
                        BeregningsresultatForReise(
                            reiseId = andreReiseId,
                            perioder =
                                listOf(
                                    beregningsresultatForPeriode(fom, tom),
                                ),
                        ),
                    ),
            ),
        privatBil = null, // TODO
    )

    val defaultVedtaksperioder =
        listOf(
            vedtaksperiode(
                fom = 1 januar 2024,
                tom = 7 januar 2024,
            ),
        )
    val defaultBeregningsresultat =
        BeregningsresultatDagligReise(
            offentligTransport =
                BeregningsresultatOffentligTransport(
                    reiser =
                        listOf(
                            BeregningsresultatForReise(
                                reiseId = dummyReiseId,
                                perioder =
                                    listOf(
                                        beregningsresultatForPeriode(førsteJanuar, sisteJanuar),
                                        beregningsresultatForPeriode(førsteFeb, sisteFeb),
                                    ),
                            ),
                        ),
                ),
            privatBil = null,
        )
    val defaultInnvilgelseDagligReise =
        InnvilgelseDagligReise(
            vedtaksperioder = defaultVedtaksperioder,
            beregningsresultat = defaultBeregningsresultat,
            rammevedtakPrivatBil = null,
            beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
        )

    fun vedtaksperiode(
        id: UUID = randomUUID(),
        fom: LocalDate = 1 januar 2025,
        tom: LocalDate = 31 januar 2025,
        målgruppe: FaktiskMålgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
        aktivitet: AktivitetType = AktivitetType.TILTAK,
    ) = Vedtaksperiode(
        id = id,
        fom = fom,
        tom = tom,
        målgruppe = målgruppe,
        aktivitet = aktivitet,
    )
}

private fun beregningsresultatForPeriode(
    fom: LocalDate,
    tom: LocalDate,
): BeregningsresultatForPeriode =
    BeregningsresultatForPeriode(
        grunnlag =
            BeregningsgrunnlagOffentligTransport(
                fom = fom,
                tom = tom,
                prisEnkeltbillett = 50,
                prisSyvdagersbillett = 300,
                pris30dagersbillett = 1000,
                antallReisedagerPerUke = 5,
                vedtaksperioder =
                    listOf(
                        VedtaksperiodeGrunnlag(
                            id = randomUUID(),
                            fom = fom,
                            tom = tom,
                            aktivitet = AktivitetType.TILTAK,
                            typeAktivitet = null,
                            målgruppe = MålgruppeType.AAP.faktiskMålgruppe(),
                            antallReisedagerIVedtaksperioden = 20,
                        ),
                    ),
                antallReisedager = 20,
                brukersNavKontor = null,
            ),
        beløp = 1000,
        billettdetaljer =
            mapOf(
                Billettype.TRETTIDAGERSBILLETT to
                    1000,
            ),
    )
