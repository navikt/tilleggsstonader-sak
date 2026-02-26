package no.nav.tilleggsstonader.sak.vedtak.dagligReise.detaljerteVedtaksperioder

import no.nav.tilleggsstonader.kontrakter.aktivitet.TypeAktivitet
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.utils.dato.april
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
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
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.dto.BeregningsresultatForPeriodeDto
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
    private val defaultAktivitet = AktivitetType.TILTAK
    private val defaultMålgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE
    private val defaultTypeDagligReise = TypeDagligReise.OFFENTLIG_TRANSPORT

    private val førsteJanuar = 1 januar 2024
    private val sisteJanuar = 31 januar 2024
    private val førsteFeb = 1 februar 2024
    private val sisteFeb = 29 februar 2024
    private val førsteMars = 1 mars 2024
    private val sisteMars = 31 mars 2024
    private val førsteApril = 1 april 2024
    private val sisteApril = 30 april 2024

    @Test
    fun `skal slå sammen sammenhengende vedtaksperioder med like verdier`() {
        val vedtak =
            innvilgelse(
                toVedtaksperioder(
                    førsteJanuar,
                    sisteJanuar,
                    førsteFeb,
                    sisteFeb,
                ),
            )

        val resultat =
            finnDetaljerteVedtaksperioderDagligReise(
                vedtaksdataTso = vedtak.data,
                vedtaksdataTsr = null,
            )
        val forventetResultat =
            listOf(
                detaljertVedtaksperiodeDagligReiseTso(
                    fom = førsteJanuar,
                    tom = sisteFeb,
                    beregningsresultat = listOf(beregningsDto(førsteJanuar, sisteJanuar), beregningsDto(førsteFeb, sisteFeb)),
                ),
            )
        assertThat(resultat).isEqualTo(forventetResultat)
    }

    @Test
    fun `skal ikke slå sammen vedtaksperioder som ikke overlapper`() {
        val vedtak =
            innvilgelse(
                toVedtaksperioder(
                    førsteApril,
                    sisteApril,
                    førsteFeb,
                    sisteFeb,
                ),
            )
        val resultat =
            finnDetaljerteVedtaksperioderDagligReise(
                vedtaksdataTso = vedtak.data,
                vedtaksdataTsr = null,
            )
        val forventetResultat =
            listOf(
                detaljertVedtaksperiodeDagligReiseTso(førsteFeb, sisteFeb, beregningsresultat = listOf(beregningsDto(førsteFeb, sisteFeb))),
                detaljertVedtaksperiodeDagligReiseTso(
                    førsteApril,
                    sisteApril,
                    beregningsresultat = listOf(beregningsDto(førsteApril, sisteApril)),
                ),
            )

        assertThat(resultat).isEqualTo(forventetResultat)
    }

    @Test
    fun `skal slå sammen like perioder når det er flere enn 1 reise`() {
        val vedtak =
            innvilgelse(
                toReiser(
                    førsteJanuar,
                    sisteJanuar,
                ),
            )

        val resultat =
            finnDetaljerteVedtaksperioderDagligReise(
                vedtaksdataTso = vedtak.data,
                vedtaksdataTsr = null,
            )
        val forventetResultat =
            listOf(
                detaljertVedtaksperiodeDagligReiseTso(
                    førsteJanuar,
                    sisteJanuar,
                    beregningsresultat = listOf(beregningsDto(førsteJanuar, sisteJanuar), beregningsDto(førsteJanuar, sisteJanuar)),
                ),
            )
        assertThat(resultat).isEqualTo(forventetResultat)
    }

    @Test
    fun `skal opprette separate vedtaksperioder når vedtaksperioder har ulike målgrupper`() {
        val vedtak =
            innvilgelse(
                innvilgelseMedUlikeVedtaksperioder(
                    fom = førsteJanuar,
                    tom = sisteJanuar,
                    vedtaksperioder = listOf(
                        vedtaksperiodeGrunnlag(førsteJanuar, sisteJanuar, målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE),
                        vedtaksperiodeGrunnlag(førsteJanuar, sisteJanuar, målgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER, aktivitet = AktivitetType.UTDANNING),
                    ),
                ),
            )

        val resultat =
            finnDetaljerteVedtaksperioderDagligReise(
                vedtaksdataTso = vedtak.data,
                vedtaksdataTsr = null,
            )

        assertThat(resultat).hasSize(2)
        assertThat(resultat.map { it.målgruppe }).containsExactlyInAnyOrder(
            FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
            FaktiskMålgruppe.ENSLIG_FORSØRGER,
        )
    }

    @Test
    fun `skal opprette separate vedtaksperioder når vedtaksperioder har ulike aktiviteter`() {
        val vedtak =
            innvilgelse(
                innvilgelseMedUlikeVedtaksperioder(
                    fom = førsteJanuar,
                    tom = sisteJanuar,
                    vedtaksperioder = listOf(
                        vedtaksperiodeGrunnlag(førsteJanuar, sisteJanuar, aktivitet = AktivitetType.TILTAK),
                        vedtaksperiodeGrunnlag(førsteJanuar, sisteJanuar, aktivitet = AktivitetType.UTDANNING, målgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER),
                    ),
                ),
            )

        val resultat =
            finnDetaljerteVedtaksperioderDagligReise(
                vedtaksdataTso = vedtak.data,
                vedtaksdataTsr = null,
            )

        assertThat(resultat).hasSize(2)
        assertThat(resultat.map { it.aktivitet }).containsExactlyInAnyOrder(
            AktivitetType.TILTAK,
            AktivitetType.UTDANNING,
        )
    }

    @Test
    fun `skal bruke fom og tom fra vedtaksperiodene når vedtaksperioder slås sammen`() {
        val vedtak =
            innvilgelse(
                innvilgelseMedUlikeVedtaksperioder(
                    fom = førsteJanuar,
                    tom = sisteMars,
                    vedtaksperioder = listOf(
                        vedtaksperiodeGrunnlag(førsteJanuar, sisteJanuar),
                        vedtaksperiodeGrunnlag(førsteFeb, sisteMars),
                    ),
                ),
            )

        val resultat =
            finnDetaljerteVedtaksperioderDagligReise(
                vedtaksdataTso = vedtak.data,
                vedtaksdataTsr = null,
            )

        assertThat(resultat).hasSize(1)
        assertThat(resultat.first().fom).isEqualTo(førsteJanuar)
        assertThat(resultat.first().tom).isEqualTo(sisteMars)
    }

    @Test
    fun `skal gruppere like vedtaksperioder og bruke fom og tom fra gruppert vedtaksperiode`() {
        val vedtak =
            innvilgelse(
                innvilgelseMedUlikeVedtaksperioder(
                    fom = førsteJanuar,
                    tom = sisteApril,
                    vedtaksperioder = listOf(
                        vedtaksperiodeGrunnlag(førsteJanuar, sisteJanuar, aktivitet = AktivitetType.TILTAK),
                        vedtaksperiodeGrunnlag(førsteFeb, sisteFeb, aktivitet = AktivitetType.TILTAK),
                        vedtaksperiodeGrunnlag(førsteMars, sisteApril, aktivitet = AktivitetType.UTDANNING, målgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER),
                    ),
                ),
            )

        val resultat =
            finnDetaljerteVedtaksperioderDagligReise(
                vedtaksdataTso = vedtak.data,
                vedtaksdataTsr = null,
            )

        assertThat(resultat).hasSize(2)

        val tiltakPeriode = resultat.first { it.aktivitet == AktivitetType.TILTAK }
        assertThat(tiltakPeriode.fom).isEqualTo(førsteJanuar)
        assertThat(tiltakPeriode.tom).isEqualTo(sisteFeb)

        val utdanningPeriode = resultat.first { it.aktivitet == AktivitetType.UTDANNING }
        assertThat(utdanningPeriode.fom).isEqualTo(førsteMars)
        assertThat(utdanningPeriode.tom).isEqualTo(sisteApril)
    }

    private fun toReiser(
        fom: LocalDate,
        tom: LocalDate,
    ) = InnvilgelseDagligReise(
        vedtaksperioder =
            listOf(
                vedtaksperiode(fom = fom, tom = tom),
            ),
        beregningsresultat = lagBeregningsresultatMedToReiser(fom, tom),
        rammevedtakPrivatBil = null,
    )

    private fun detaljertVedtaksperiodeDagligReiseTso(
        fom: LocalDate,
        tom: LocalDate,
        aktivitet: AktivitetType = defaultAktivitet,
        målgruppe: FaktiskMålgruppe = defaultMålgruppe,
        typeDagligReise: TypeDagligReise = defaultTypeDagligReise,
        beregningsresultat: List<BeregningsresultatForPeriodeDto>,
    ) = DetaljertVedtaksperiodeDagligReise(
        fom = fom,
        tom = tom,
        aktivitet = aktivitet,
        målgruppe = målgruppe,
        typeDagligReise = typeDagligReise,
        stønadstype = Stønadstype.DAGLIG_REISE_TSO,
        typeAktivtet = null,
        beregningsresultat =
        beregningsresultat,
    )

    private fun beregningsDto(
        fom: LocalDate,
        tom: LocalDate,
    ) = BeregningsresultatForPeriodeDto(
        fom = fom,
        tom = tom,
        prisEnkeltbillett = 50,
        prisSyvdagersbillett = 300,
        pris30dagersbillett = 1000,
        antallReisedagerPerUke = 5,
        beløp = 1000,
        billettdetaljer = mapOf(Billettype.TRETTIDAGERSBILLETT to 1),
        antallReisedager = 20,
        fraTidligereVedtak = false,
        brukersNavKontor = null,
    )

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

    private fun vedtaksperiodeGrunnlag(
        fom: LocalDate,
        tom: LocalDate,
        målgruppe: FaktiskMålgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
        aktivitet: AktivitetType = AktivitetType.TILTAK,
        typeAktivitet: TypeAktivitet? = null,
    ) = VedtaksperiodeGrunnlag(
        id = randomUUID(),
        fom = fom,
        tom = tom,
        målgruppe = målgruppe,
        aktivitet = aktivitet,
        typeAktivitet = typeAktivitet,
        antallReisedagerIVedtaksperioden = 20,
    )

    private fun innvilgelseMedUlikeVedtaksperioder(
        fom: LocalDate,
        tom: LocalDate,
        vedtaksperioder: List<VedtaksperiodeGrunnlag>,
    ) = InnvilgelseDagligReise(
        vedtaksperioder = listOf(vedtaksperiode(fom = fom, tom = tom)),
        beregningsresultat = BeregningsresultatDagligReise(
            offentligTransport = BeregningsresultatOffentligTransport(
                reiser = listOf(
                    BeregningsresultatForReise(
                        reiseId = dummyReiseId,
                        perioder = listOf(
                            BeregningsresultatForPeriode(
                                grunnlag = BeregningsgrunnlagOffentligTransport(
                                    fom = fom,
                                    tom = tom,
                                    prisEnkeltbillett = 50,
                                    prisSyvdagersbillett = 300,
                                    pris30dagersbillett = 1000,
                                    antallReisedagerPerUke = 5,
                                    vedtaksperioder = vedtaksperioder,
                                    antallReisedager = 20,
                                    brukersNavKontor = null,
                                ),
                                beløp = 1000,
                                billettdetaljer = mapOf(Billettype.TRETTIDAGERSBILLETT to 1),
                            ),
                        ),
                    ),
                ),
            ),
        ),
        rammevedtakPrivatBil = null,
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
                    1,
            ),
    )
