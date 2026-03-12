package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.dummyReiseId
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
                ),
            )
        val tsrVedtak =
            innvilgelse(
                InnvilgelseDagligReise(
                    vedtaksperioder = defaultVedtaksperioder,
                    beregningsresultat = defaultBeregningsresultat,
                    rammevedtakPrivatBil = null,
                ),
            )

        val resultat =
            finnDetaljerteVedtaksperioderDagligReise(
                vedtaksdataTso = tsoVedtak.data,
                vedtaksdataTsr = tsrVedtak.data,
            )
        val forventetTso =
            DetaljertVedtaksperiodeDagligReise(
                stønadstype = Stønadstype.DAGLIG_REISE_TSO,
                typeDagligReise = TypeDagligReise.OFFENTLIG_TRANSPORT,
                detaljertBeregningsperioder =
                    listOf(
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
                    ),
            )
        val forventetTsr = forventetTso.copy(stønadstype = Stønadstype.DAGLIG_REISE_TSR)
        assertThat(resultat).containsExactly(forventetTso, forventetTsr)
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

    private fun toVedtaksperioder(
        fom1: LocalDate,
        tom1: LocalDate,
        fom2: LocalDate,
        tom2: LocalDate,
    ) = InnvilgelseDagligReise(
        vedtaksperioder =
            listOf(
                vedtaksperiode(fom = fom1, tom = tom1),
                vedtaksperiode(fom = fom2, tom = tom2),
            ),
        beregningsresultat = lagBeregningsresultatMedToPerioder(fom1, tom1, fom2, tom2),
        rammevedtakPrivatBil = null,
    )

    private fun lagBeregningsresultatMedToReiser(
        fom: LocalDate,
        tom: LocalDate,
    ) = BeregningsresultatDagligReise(
        offentligTransport =
            BeregningsresultatOffentligTransport(
                reiser =
                    listOf(
                        BeregningsresultatForReise(
                            reiseId = dummyReiseId,
                            perioder =
                                listOf(
                                    beregningsresultatForPeriode(fom, tom),
                                ),
                        ),
                        BeregningsresultatForReise(
                            reiseId = dummyReiseId,
                            perioder =
                                listOf(
                                    beregningsresultatForPeriode(fom, tom),
                                ),
                        ),
                    ),
            ),
        privatBil = null, // TODO
    )

    private fun lagBeregningsresultatMedToPerioder(
        fom1: LocalDate,
        tom1: LocalDate,
        fom2: LocalDate,
        tom2: LocalDate,
    ) = BeregningsresultatDagligReise(
        offentligTransport =
            BeregningsresultatOffentligTransport(
                reiser =
                    listOf(
                        BeregningsresultatForReise(
                            reiseId = dummyReiseId,
                            perioder =
                                listOf(
                                    beregningsresultatForPeriode(fom1, tom1),
                                    beregningsresultatForPeriode(fom2, tom2),
                                ),
                        ),
                    ),
            ),
        privatBil = null,
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
