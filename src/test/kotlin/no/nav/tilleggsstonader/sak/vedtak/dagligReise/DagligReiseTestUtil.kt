package no.nav.tilleggsstonader.sak.vedtak.dagligReise

import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.beregning.Billettype
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsgrunnlagOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatDagligReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForPeriode
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatForReise
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.BeregningsresultatOffentligTransport
import no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.dagligReise.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate
import java.util.UUID
import java.util.UUID.randomUUID

object DagligReiseTestUtil {
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
                beregningsresultatOffentligTransport(),
        )
    val defaultInnvilgelseDagligReise =
        InnvilgelseDagligReise(
            vedtaksperioder = defaultVedtaksperioder,
            beregningsresultat = defaultBeregningsresultat,
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
    ) = GeneriskVedtak(
        behandlingId = behandlingId,
        type = TypeVedtak.INNVILGELSE,
        data =
            InnvilgelseDagligReise(
                vedtaksperioder = vedtaksperioder,
                beregningsresultat = beregningsresultat,
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
    reiseId = ReiseId.random(),
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
