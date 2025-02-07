package no.nav.tilleggsstonader.sak.vedtak.læremidler

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
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
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.BeregningsresultatForPeriodeDto
import no.nav.tilleggsstonader.sak.vedtak.læremidler.dto.VedtaksperiodeStatus
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.time.LocalDate
import java.util.UUID

object LæremidlerTestUtil {
    val defaultVedtaksperioder =
        listOf(
            Vedtaksperiode(
                id = UUID.randomUUID(),
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 1, 7),
                status = VedtaksperiodeStatus.NY,
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
        )

    fun innvilgelse(
        vedtaksperioder: List<Vedtaksperiode> = defaultVedtaksperioder,
        beregningsresultat: BeregningsresultatLæremidler = defaultBeregningsresultat,
    ) = GeneriskVedtak(
        behandlingId = BehandlingId.random(),
        type = TypeVedtak.INNVILGELSE,
        data =
            InnvilgelseLæremidler(
                vedtaksperioder = vedtaksperioder,
                beregningsresultat = beregningsresultat,
            ),
    )

    fun avslag(
        årsaker: List<ÅrsakAvslag> = listOf(ÅrsakAvslag.ANNET),
        begrunnelse: String = "en begrunnelse",
    ) = GeneriskVedtak(
        behandlingId = BehandlingId.random(),
        type = TypeVedtak.AVSLAG,
        data =
            AvslagLæremidler(
                årsaker = årsaker,
                begrunnelse = begrunnelse,
            ),
    )

    fun opphør(
        vedtaksperioder: List<Vedtaksperiode> = defaultVedtaksperioder,
        beregningsresultat: BeregningsresultatLæremidler = defaultBeregningsresultat,
        årsaker: List<ÅrsakOpphør> = listOf(ÅrsakOpphør.ENDRING_UTGIFTER),
        begrunnelse: String = "En begrunnelse",
    ) = GeneriskVedtak(
        behandlingId = BehandlingId.random(),
        type = TypeVedtak.OPPHØR,
        data =
            OpphørLæremidler(
                vedtaksperioder = vedtaksperioder,
                beregningsresultat = beregningsresultat,
                årsaker = årsaker,
                begrunnelse = begrunnelse,
            ),
    )

    fun beregningsresultatForMåned(
        fom: LocalDate,
        tom: LocalDate,
        utbetalingsdato: LocalDate = fom,
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
                    målgruppe = MålgruppeType.AAP,
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
        )
}
