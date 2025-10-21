package no.nav.tilleggsstonader.sak.vedtak.dagligReise

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
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseDagligReise
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.BeregningsresultatForPeriodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate
import java.util.UUID
import java.util.UUID.randomUUID

object DagligReiseTestUtil {
    val defaultVedtaksperioder =
        listOf(
            vedtaksperiode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 7),
            ),
        )
    val defaultBeregningsresultat =
        BeregningsresultatDagligReise(
            offentligTransport =
                BeregningsresultatOffentligTransport(
                    reiser =
                        listOf(
                            BeregningsresultatForReise(
                                perioder =
                                    listOf(
                                        BeregningsresultatForPeriode(
                                            grunnlag =
                                                BeregningsgrunnlagOffentligTransport(
                                                    fom = defaultVedtaksperioder.first().fom,
                                                    tom = defaultVedtaksperioder.first().tom,
                                                    prisEnkeltbillett = 50,
                                                    prisSyvdagersbillett = 300,
                                                    pris30dagersbillett = 1000,
                                                    antallReisedagerPerUke = 5,
                                                    vedtaksperioder =
                                                        listOf(
                                                            no.nav.tilleggsstonader.sak.vedtak.dagligReise.domain.VedtaksperiodeGrunnlag(
                                                                id = randomUUID(),
                                                                fom = defaultVedtaksperioder.first().fom,
                                                                tom = defaultVedtaksperioder.first().tom,
                                                                aktivitet = AktivitetType.TILTAK,
                                                                målgruppe = MålgruppeType.AAP.faktiskMålgruppe(),
                                                                antallReisedagerIVedtaksperioden = 20,
                                                            ),
                                                        ),
                                                    antallReisedager = 20,
                                                ),
                                            beløp = 1000,
                                            billettdetaljer =
                                                mapOf(
                                                    Billettype.TRETTIDAGERSBILLETT to
                                                        1000,
                                                ),
                                        ),
                                    ),
                            ),
                        ),
                ),
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
            AvslagLæremidler(
                årsaker = årsaker,
                begrunnelse = begrunnelse,
            ),
        gitVersjon = Applikasjonsversjon.versjon,
        tidligsteEndring = null,
        opphørsdato = null,
    )

    fun beregningsresultatForMåned(
        fom: LocalDate,
        tom: LocalDate,
        utbetalingsdato: LocalDate = fom,
        målgruppe: FaktiskMålgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
        aktivitet: AktivitetType = AktivitetType.TILTAK,
    ): BeregningsresultatForMåned =
        BeregningsresultatForMåned(
            beløp = 875,
            grunnlag =
                Beregningsgrunnlag(
                    fom = fom,
                    tom = tom,
                    utbetalingsdato = utbetalingsdato,
                    studienivå = Studienivå.HØYERE_UTDANNING,
                    studieprosent = 100,
                    sats = 875,
                    satsBekreftet = true,
                    målgruppe = målgruppe,
                    aktivitet = aktivitet,
                ),
        )

    fun beregningsresultatForPeriodeDto(
        fom: LocalDate,
        tom: LocalDate,
        antallMåneder: Int = 1,
        stønadsbeløpForPeriode: Int = 875,
        utbetalingsdato: LocalDate = fom,
    ): BeregningsresultatForPeriodeDto =
        BeregningsresultatForPeriodeDto(
            fom = fom,
            tom = tom,
            antallMåneder = antallMåneder,
            studienivå = Studienivå.HØYERE_UTDANNING,
            studieprosent = 100,
            stønadsbeløpPerMåned = 875,
            stønadsbeløpForPeriode = stønadsbeløpForPeriode,
            utbetalingsdato = utbetalingsdato,
            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
            aktivitet = AktivitetType.TILTAK,
            delAvTidligereUtbetaling = false,
        )

    fun vedtaksperiode(
        id: UUID = UUID.randomUUID(),
        fom: LocalDate = LocalDate.of(2025, 1, 1),
        tom: LocalDate = LocalDate.of(2025, 1, 31),
        målgruppe: FaktiskMålgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
        aktivitet: AktivitetType = AktivitetType.TILTAK,
    ) = Vedtaksperiode(
        id = id,
        fom = fom,
        tom = tom,
        målgruppe = målgruppe,
        aktivitet = aktivitet,
    )

    fun vedtaksperiodeDto(
        id: UUID = UUID.randomUUID(),
        fom: LocalDate = LocalDate.of(2025, 1, 1),
        tom: LocalDate = LocalDate.of(2025, 1, 31),
        målgruppe: FaktiskMålgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
        aktivitet: AktivitetType = AktivitetType.TILTAK,
    ) = VedtaksperiodeDto(
        id = id,
        fom = fom,
        tom = tom,
        målgruppeType = målgruppe,
        aktivitetType = aktivitet,
    )
}
