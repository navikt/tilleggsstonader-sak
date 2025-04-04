package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørLæremidler
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.BeregningsresultatLæremidler
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Studienivå
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.læremidler.domain.VedtaksperiodeStatus
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.BeregningsresultatForPeriodeDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtaksperiodeLæremidlerDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.time.LocalDate
import java.util.UUID

object LæremidlerTestUtil {
    val defaultVedtaksperioder =
        listOf(
            vedtaksperiode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 7),
            ),
        )
    val defaultBeregningsresultat =
        BeregningsresultatLæremidler(
            perioder =
                listOf(
                    beregningsresultatForMåned(
                        fom = LocalDate.of(2024, 1, 1),
                        tom = LocalDate.of(2024, 1, 7),
                        utbetalingsdato = LocalDate.of(2024, 1, 1),
                    ),
                ),
        )
    val defaultInnvilgelseLæremidler =
        InnvilgelseLæremidler(
            vedtaksperioder = defaultVedtaksperioder,
            beregningsresultat = defaultBeregningsresultat,
        )

    fun innvilgelse(data: InnvilgelseLæremidler = defaultInnvilgelseLæremidler) =
        GeneriskVedtak(
            behandlingId = BehandlingId.random(),
            type = TypeVedtak.INNVILGELSE,
            data = data,
            gitVersjon = Applikasjonsversjon.versjon,
        )

    fun innvilgelse(
        behandlingId: BehandlingId = BehandlingId.random(),
        vedtaksperioder: List<Vedtaksperiode> = defaultVedtaksperioder,
        beregningsresultat: BeregningsresultatLæremidler = defaultBeregningsresultat,
    ) = GeneriskVedtak(
        behandlingId = behandlingId,
        type = TypeVedtak.INNVILGELSE,
        data =
            InnvilgelseLæremidler(
                vedtaksperioder = vedtaksperioder,
                beregningsresultat = beregningsresultat,
            ),
        gitVersjon = Applikasjonsversjon.versjon,
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
    )

    fun opphør(
        behandlingId: BehandlingId = BehandlingId.random(),
        vedtaksperioder: List<Vedtaksperiode> = defaultVedtaksperioder,
        beregningsresultat: BeregningsresultatLæremidler = defaultBeregningsresultat,
        årsaker: List<ÅrsakOpphør> = listOf(ÅrsakOpphør.ENDRING_UTGIFTER),
        begrunnelse: String = "En begrunnelse",
    ) = GeneriskVedtak(
        behandlingId = behandlingId,
        type = TypeVedtak.OPPHØR,
        data =
            OpphørLæremidler(
                vedtaksperioder = vedtaksperioder,
                beregningsresultat = beregningsresultat,
                årsaker = årsaker,
                begrunnelse = begrunnelse,
            ),
        gitVersjon = Applikasjonsversjon.versjon,
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
        stønadsbeløp: Int = 875,
        utbetalingsdato: LocalDate = fom,
    ): BeregningsresultatForPeriodeDto =
        BeregningsresultatForPeriodeDto(
            fom = fom,
            tom = tom,
            antallMåneder = antallMåneder,
            studienivå = Studienivå.HØYERE_UTDANNING,
            studieprosent = 100,
            beløp = 875,
            stønadsbeløp = stønadsbeløp,
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
        status: VedtaksperiodeStatus = VedtaksperiodeStatus.NY,
    ) = Vedtaksperiode(
        id = id,
        fom = fom,
        tom = tom,
        målgruppe = målgruppe,
        aktivitet = aktivitet,
        status = status,
    )

    fun vedtaksperiodeDto(
        id: UUID = UUID.randomUUID(),
        fom: LocalDate = LocalDate.of(2025, 1, 1),
        tom: LocalDate = LocalDate.of(2025, 1, 31),
        målgruppe: FaktiskMålgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
        aktivitet: AktivitetType = AktivitetType.TILTAK,
    ) = VedtaksperiodeLæremidlerDto(
        id = id,
        fom = fom,
        tom = tom,
        målgruppeType = målgruppe,
        aktivitetType = aktivitet,
    )
}
