package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.RammevedtakPrivatBilUtil.rammeForReiseMedPrivatBil
import no.nav.tilleggsstonader.sak.util.dummyReiseId
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.offentligTransport.Billettype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilDag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReisePrivatBilPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammevedtakPrivatBil
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.UUID.randomUUID

object DagligReiseTestUtil {
    private val defaultPrivatBilPerioder =
        listOf(
            PrivatBilPeriode(
                fom = 3 januar 2024,
                tom = 7 januar 2024,
                stønadsbeløp = 100,
            ),
            PrivatBilPeriode(
                fom = 8 januar 2024,
                tom = 14 januar 2024,
                stønadsbeløp = 200,
            ),
            PrivatBilPeriode(
                fom = 15 januar 2024,
                tom = 18 januar 2024,
                stønadsbeløp = 300,
            ),
        )

    val defaultRammevedtakPrivatBil = rammevedtakPrivatBil(perioder = defaultPrivatBilPerioder)

    val defaultVedtaksperioder =
        listOf(
            vedtaksperiode(
                fom = 1 januar 2024,
                tom = 7 januar 2024,
            ),
        )
    val defaultBeregningsresultat =
        BeregningsresultatDagligReise(
            offentligTransport = beregningsresultatOffentligTransport(),
            privatBil = beregningsresultatPrivatBil(perioder = defaultPrivatBilPerioder),
        )
    val defaultInnvilgelseDagligReise =
        InnvilgelseDagligReise(
            vedtaksperioder = defaultVedtaksperioder,
            rammevedtakPrivatBil = defaultRammevedtakPrivatBil,
            beregningsresultat = defaultBeregningsresultat,
            beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
        )

    fun innvilgelse(data: InnvilgelseDagligReise = defaultInnvilgelseDagligReise) =
        GeneriskVedtak(
            behandlingId = BehandlingId.random(),
            type = TypeVedtak.INNVILGELSE,
            data = data,
            gitVersjon = Applikasjonsversjon.versjon,
            tidligsteEndring = null,
            opphørsdato = null,
        )

    fun innvilgelse(
        behandlingId: BehandlingId = BehandlingId.random(),
        vedtaksperioder: List<Vedtaksperiode> = defaultVedtaksperioder,
        beregningsresultat: BeregningsresultatDagligReise = defaultBeregningsresultat,
        rammevedtakPrivatBil: RammevedtakPrivatBil? = if (beregningsresultat.privatBil != null) defaultRammevedtakPrivatBil else null,
    ) = GeneriskVedtak(
        behandlingId = behandlingId,
        type = TypeVedtak.INNVILGELSE,
        data =
            InnvilgelseDagligReise(
                vedtaksperioder = vedtaksperioder,
                beregningsresultat = beregningsresultat,
                rammevedtakPrivatBil = rammevedtakPrivatBil,
                beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
            ),
        gitVersjon = Applikasjonsversjon.versjon,
        tidligsteEndring = null,
        opphørsdato = null,
    )

    fun avslag(
        behandlingId: BehandlingId = BehandlingId.random(),
        årsaker: List<ÅrsakAvslag> = listOf(ÅrsakAvslag.ANNET),
        begrunnelse: String = "en begrunnelse",
    ) = GeneriskVedtak(
        behandlingId = behandlingId,
        type = TypeVedtak.AVSLAG,
        data =
            AvslagDagligReise(
                årsaker = årsaker,
                begrunnelse = begrunnelse,
            ),
        gitVersjon = Applikasjonsversjon.versjon,
        tidligsteEndring = null,
        opphørsdato = null,
    )

    fun vedtaksperiode(
        id: UUID = randomUUID(),
        fom: LocalDate = 1 januar 2024,
        tom: LocalDate = 31 januar 2024,
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

private fun beregningsresultatOffentligTransport(
    fom: LocalDate = 1 januar 2024,
    tom: LocalDate = 31 januar 2024,
    reiser: List<BeregningsresultatForReise> =
        listOf(
            beregningsresultatForReise(fom = fom, tom = tom),
        ),
) = BeregningsresultatOffentligTransport(
    reiser = reiser,
)

private fun beregningsresultatForReise(
    fom: LocalDate = 1 januar 2024,
    tom: LocalDate = 31 januar 2024,
    perioder: List<BeregningsresultatForPeriode> =
        listOf(
            beregningsresultatForPeriode(fom = fom, tom = tom),
        ),
) = BeregningsresultatForReise(
    reiseId = dummyReiseId,
    perioder = perioder,
)

private fun beregningsresultatForPeriode(
    fom: LocalDate = 1 januar 2024,
    tom: LocalDate = 31 januar 2024,
    grunnlag: BeregningsgrunnlagOffentligTransport = beregningsgrunnlagOffentligTransport(fom = fom, tom = tom),
    beløp: Int = 1000,
    billettdetaljer: Map<Billettype, Int> =
        mapOf(
            Billettype.TRETTIDAGERSBILLETT to 1000,
        ),
) = BeregningsresultatForPeriode(
    grunnlag = grunnlag,
    beløp = beløp,
    billettdetaljer = billettdetaljer,
)

private fun beregningsgrunnlagOffentligTransport(
    fom: LocalDate = 1 januar 2024,
    tom: LocalDate = 31 januar 2024,
    prisEnkeltbillett: Int = 50,
    prisSyvdagersbillett: Int = 300,
    pris30dagersbillett: Int = 1000,
    antallReisedagerPerUke: Int = 5,
    vedtaksperioder: List<VedtaksperiodeGrunnlag> =
        listOf(
            vedtaksperiodeGrunnlag(fom = fom, tom = tom),
        ),
    antallReisedager: Int = 20,
) = BeregningsgrunnlagOffentligTransport(
    fom = fom,
    tom = tom,
    prisEnkeltbillett = prisEnkeltbillett,
    prisSyvdagersbillett = prisSyvdagersbillett,
    pris30dagersbillett = pris30dagersbillett,
    antallReisedagerPerUke = antallReisedagerPerUke,
    vedtaksperioder = vedtaksperioder,
    antallReisedager = antallReisedager,
    brukersNavKontor = null,
)

private fun vedtaksperiodeGrunnlag(
    id: UUID = randomUUID(),
    fom: LocalDate = 1 januar 2025,
    tom: LocalDate = 31 januar 2025,
    aktivitet: AktivitetType = AktivitetType.TILTAK,
    målgruppe: FaktiskMålgruppe = MålgruppeType.AAP.faktiskMålgruppe(),
    antallReisedagerIVedtaksperioden: Int = 20,
) = VedtaksperiodeGrunnlag(
    id = id,
    fom = fom,
    tom = tom,
    aktivitet = aktivitet,
    målgruppe = målgruppe,
    antallReisedagerIVedtaksperioden = antallReisedagerIVedtaksperioden,
)

private fun beregningsresultatPrivatBil(perioder: List<PrivatBilPeriode>) =
    BeregningsresultatPrivatBil(
        reiser = listOf(beregningsresultatForReisePrivatBil(perioder = perioder)),
    )

private fun beregningsresultatForReisePrivatBil(
    reiseId: ReiseId = dummyReiseId,
    perioder: List<PrivatBilPeriode>,
) = BeregningsresultatForReisePrivatBil(
    reiseId = reiseId,
    perioder = perioder.map { periode -> beregningsresultatForReisePrivatBilPeriode(periode) },
)

private fun beregningsresultatForReisePrivatBilPeriode(periode: PrivatBilPeriode) =
    BeregningsresultatForReisePrivatBilPeriode(
        fom = periode.fom,
        tom = periode.tom,
        grunnlag = beregningsresultatForReisePrivatBilGrunnlag(fom = periode.fom, stønadsbeløpForDag = periode.stønadsbeløp),
        stønadsbeløp = periode.stønadsbeløp.toBigDecimal(),
        brukersNavKontor = null,
        fraTidligereVedtak = false,
    )

private fun beregningsresultatForReisePrivatBilGrunnlag(
    fom: LocalDate = 1 januar 2024,
    stønadsbeløpForDag: Int = 100,
) = BeregningsresultatForReisePrivatBilGrunnlag(
    dager =
        listOf(
            BeregningsresultatForReisePrivatBilDag(
                dato = fom,
                parkeringskostnad = 0,
                dagsatsUtenParkering = stønadsbeløpForDag.toBigDecimal(),
                stønadsbeløpForDag = stønadsbeløpForDag.toBigDecimal(),
            ),
        ),
)

private fun rammevedtakPrivatBil(perioder: List<PrivatBilPeriode>) =
    RammevedtakPrivatBil(
        reiser =
            listOf(
                rammeForReiseMedPrivatBil(
                    reiseId = dummyReiseId,
                    fom = perioder.first().fom,
                    tom = perioder.last().tom,
                    vedtaksperioder =
                        perioder.map { periode ->
                            DagligReiseTestUtil.vedtaksperiode(fom = periode.fom, tom = periode.tom)
                        },
                    delperioder =
                        perioder.map { periode ->
                            val dagerIperiode = ChronoUnit.DAYS.between(periode.fom, periode.tom.plusDays(1)).toInt()
                            no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBilDelperiode(
                                fom = periode.fom,
                                tom = periode.tom,
                                reisedagerPerUke = 5,
                                ekstrakostnader =
                                    no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain
                                        .RammeForReiseMedPrivatEkstrakostnader(
                                            null,
                                            null,
                                        ),
                                satser =
                                    listOf(
                                        no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.RammeForReiseMedPrivatBilSatsForDelperiode(
                                            fom = periode.fom,
                                            tom = periode.tom,
                                            satsBekreftetVedVedtakstidspunkt = true,
                                            kilometersats = 2.94.toBigDecimal(),
                                            dagsatsUtenParkering = (periode.stønadsbeløp / dagerIperiode).toBigDecimal(),
                                        ),
                                    ),
                            )
                        },
                ),
            ),
    )

private data class PrivatBilPeriode(
    val fom: LocalDate,
    val tom: LocalDate,
    val stønadsbeløp: Int,
)
