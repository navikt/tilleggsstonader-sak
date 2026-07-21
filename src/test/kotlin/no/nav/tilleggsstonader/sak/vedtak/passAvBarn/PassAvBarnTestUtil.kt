package no.nav.tilleggsstonader.sak.vedtak.passAvBarn

import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.felles.domain.VedtaksperiodeId
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.Beregningsomfang
import no.nav.tilleggsstonader.sak.vedtak.Beregningsplan
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagPassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelsePassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørPassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningTestUtil.vedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.BeregningsresultatPassAvBarn
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.UtgiftBarn
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto.InnvilgelsePassAvBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.passAvBarn.dto.OpphørPassAvBarnRequest
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

object PassAvBarnTestUtil {
    fun innvilgelseDto(
        vedtaksperioder: List<VedtaksperiodeDto>,
        begrunnelse: String? = null,
    ) = InnvilgelsePassAvBarnRequest(
        vedtaksperioder = vedtaksperioder,
        begrunnelse = begrunnelse,
    )

    fun opphørDto(opphørsdato: LocalDate?) =
        OpphørPassAvBarnRequest(
            årsakerOpphør = listOf(ÅrsakOpphør.ENDRING_UTGIFTER),
            begrunnelse = "Endring i utgifter",
            opphørsdato = opphørsdato,
        )

    val defaultBehandling = behandling()
    val defaultVedtaksperiodeId = VedtaksperiodeId.random()

    val behandlingId = BehandlingId.fromString("001464ca-20dc-4f6c-b3e8-c83bd98b3e31")

    val defaultBarn1 = BehandlingBarn(behandlingId = behandlingId, ident = "1")
    val defaultBarn2 = BehandlingBarn(behandlingId = behandlingId, ident = "2")

    val beløpsperioderDefault =
        listOf(
            Beløpsperiode(dato = LocalDate.now(), beløp = 1000, målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE),
            Beløpsperiode(
                dato = LocalDate.now().plusDays(7),
                beløp = 2000,
                målgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER,
            ),
        )

    val defaultVedtaksperiode =
        Vedtaksperiode(
            id = defaultVedtaksperiodeId,
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 1, 31),
            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
            aktivitet = AktivitetType.TILTAK,
        )

    val defaultInnvilgelsePassAvBarn =
        InnvilgelsePassAvBarn(
            beregningsresultat =
                BeregningsresultatPassAvBarn(
                    perioder =
                        listOf(
                            beregningsresultatForMåned(vedtaksperioder = listOf(vedtaksperiodeGrunnlag())),
                        ),
                ),
            vedtaksperioder = emptyList(),
            beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
        )

    val vedtakBeregningsresultat =
        BeregningsresultatPassAvBarn(
            perioder =
                listOf(
                    beregningsresultatForMåned(),
                ),
        )

    fun beregningsresultatForMåned(
        måned: YearMonth = YearMonth.of(2024, 1),
        vedtaksperioder: List<VedtaksperiodeGrunnlag> = emptyList(),
        beløpsperioder: List<Beløpsperiode> = beløpsperioderDefault,
        utgifterTotal: Int = 5000,
    ) = BeregningsresultatForMåned(
        dagsats = BigDecimal.TEN,
        månedsbeløp = 3000,
        grunnlag =
            Beregningsgrunnlag(
                måned = måned,
                makssats = 3000,
                vedtaksperiodeGrunnlag = vedtaksperioder,
                utgifter = listOf(UtgiftBarn(defaultBarn1.id, 1000)),
                utgifterTotal = utgifterTotal,
                antallBarn = 1,
            ),
        beløpsperioder = beløpsperioder,
    )

    fun beregningsresultatForMåned(
        måned: YearMonth = YearMonth.of(2024, 1),
        beløpsperioder: List<Beløpsperiode> = beløpsperioderDefault,
        grunnlag: Beregningsgrunnlag = beregningsgrunnlag(måned),
    ) = BeregningsresultatForMåned(
        dagsats = BigDecimal.TEN,
        månedsbeløp = 3000,
        grunnlag = grunnlag,
        beløpsperioder = beløpsperioder,
    )

    fun beregningsgrunnlag(
        måned: YearMonth = YearMonth.of(2024, 1),
        vedtaksperioder: List<VedtaksperiodeGrunnlag> =
            listOf(vedtaksperiodeGrunnlag(vedtaksperiodeBeregning(måned.atDay(1), måned.atEndOfMonth()))),
        utgifterTotal: Int = 5000,
        utgifter: List<UtgiftBarn> = listOf(UtgiftBarn(defaultBarn1.id, 1000)),
    ) = Beregningsgrunnlag(
        måned = måned,
        makssats = 3000,
        vedtaksperiodeGrunnlag = vedtaksperioder,
        utgifter = utgifter,
        utgifterTotal = utgifterTotal,
        antallBarn = 1,
    )

    fun innvilgelse(data: InnvilgelsePassAvBarn = defaultInnvilgelsePassAvBarn) =
        GeneriskVedtak(
            behandlingId = defaultBehandling.id,
            type = TypeVedtak.INNVILGELSE,
            data = data,
            gitVersjon = Applikasjonsversjon.versjon,
            tidligsteEndring = null,
            opphørsdato = null,
        )

    fun vedtaksperiodeGrunnlag(vedtaksperiode: VedtaksperiodeBeregning = vedtaksperiodeBeregning()): VedtaksperiodeGrunnlag =
        VedtaksperiodeGrunnlag(
            vedtaksperiode = vedtaksperiode,
            aktiviteter = emptyList(),
            antallDager = 0,
        )

    fun innvilgetVedtak(
        behandlingId: BehandlingId = defaultBehandling.id,
        beregningsresultat: BeregningsresultatPassAvBarn = vedtakBeregningsresultat,
        vedtaksperioder: List<Vedtaksperiode> = emptyList(),
        tidligsteEndring: LocalDate? = null,
        beregningsplan: Beregningsplan = Beregningsplan(Beregningsomfang.ALLE_PERIODER),
    ) = GeneriskVedtak(
        behandlingId = behandlingId,
        type = TypeVedtak.INNVILGELSE,
        data =
            InnvilgelsePassAvBarn(
                beregningsresultat = beregningsresultat,
                vedtaksperioder = vedtaksperioder,
                beregningsplan = beregningsplan,
            ),
        gitVersjon = Applikasjonsversjon.versjon,
        tidligsteEndring = tidligsteEndring,
        opphørsdato = null,
    )

    fun innvilgetVedtak(
        behandlingId: BehandlingId = defaultBehandling.id,
        vedtak: InnvilgelsePassAvBarn,
    ) = GeneriskVedtak(
        behandlingId = behandlingId,
        type = TypeVedtak.INNVILGELSE,
        data = vedtak,
        gitVersjon = Applikasjonsversjon.versjon,
        tidligsteEndring = null,
        opphørsdato = null,
    )

    fun avslagVedtak(
        behandlingId: BehandlingId = defaultBehandling.id,
        årsaker: List<ÅrsakAvslag>,
        begrunnelse: String,
    ) = GeneriskVedtak(
        behandlingId = behandlingId,
        type = TypeVedtak.AVSLAG,
        data =
            AvslagPassAvBarn(
                årsaker = årsaker,
                begrunnelse = begrunnelse,
            ),
        gitVersjon = Applikasjonsversjon.versjon,
        tidligsteEndring = null,
        opphørsdato = null,
    )

    fun opphørVedtak(
        behandlingId: BehandlingId = defaultBehandling.id,
        årsaker: List<ÅrsakOpphør>,
        beregningsresultat: BeregningsresultatPassAvBarn = vedtakBeregningsresultat,
        begrunnelse: String,
        opphørsdato: LocalDate = LocalDate.now(),
        beregningsplan: Beregningsplan = Beregningsplan(Beregningsomfang.FRA_DATO, opphørsdato),
    ) = GeneriskVedtak(
        behandlingId = behandlingId,
        type = TypeVedtak.OPPHØR,
        data =
            OpphørPassAvBarn(
                årsaker = årsaker,
                begrunnelse = begrunnelse,
                beregningsresultat = beregningsresultat,
                vedtaksperioder = emptyList(),
                beregningsplan = beregningsplan,
            ),
        gitVersjon = Applikasjonsversjon.versjon,
        tidligsteEndring = null,
        opphørsdato = opphørsdato,
    )
}
