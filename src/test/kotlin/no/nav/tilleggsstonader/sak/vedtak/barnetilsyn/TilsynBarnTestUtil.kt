package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.interntVedtak.Testdata.behandlingId
import no.nav.tilleggsstonader.sak.util.Applikasjonsversjon
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.UtgiftBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.OpphørTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.domain.VedtaksperiodeBeregningTestUtil.vedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

object TilsynBarnTestUtil {
    fun innvilgelseDto(
        vedtaksperioder: List<VedtaksperiodeDto>,
        begrunnelse: String? = null,
    ) = InnvilgelseTilsynBarnRequest(
        vedtaksperioder = vedtaksperioder,
        begrunnelse = begrunnelse,
    )

    fun opphørDto() =
        OpphørTilsynBarnRequest(
            årsakerOpphør = listOf(ÅrsakOpphør.ENDRING_UTGIFTER),
            begrunnelse = "Endring i utgifter",
        )

    val defaultBehandling = behandling()
    val defaultVedtaksperiodeId = UUID.randomUUID()

    val defaultBarn1 = BehandlingBarn(behandlingId = behandlingId, ident = "1")
    val defaultBarn2 = BehandlingBarn(behandlingId = behandlingId, ident = "2")

    val beløpsperioderDefault =
        listOf(
            Beløpsperiode(dato = LocalDate.now(), beløp = 1000, målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE),
            Beløpsperiode(dato = LocalDate.now().plusDays(7), beløp = 2000, målgruppe = FaktiskMålgruppe.ENSLIG_FORSØRGER),
        )

    val defaultVedtaksperiode =
        Vedtaksperiode(
            id = defaultVedtaksperiodeId,
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 1, 31),
            målgruppe = FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE,
            aktivitet = AktivitetType.TILTAK,
        )

    val defaultInnvilgelseTilsynBarn =
        InnvilgelseTilsynBarn(
            beregningsresultat =
                BeregningsresultatTilsynBarn(
                    perioder =
                        listOf(
                            beregningsresultatForMåned(vedtaksperioder = listOf(vedtaksperiodeGrunnlag())),
                        ),
                ),
            vedtaksperioder = emptyList(),
        )

    val vedtakBeregningsresultat =
        BeregningsresultatTilsynBarn(
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
        vedtaksperioder: List<VedtaksperiodeGrunnlag> = emptyList(),
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

    fun innvilgelse(data: InnvilgelseTilsynBarn = defaultInnvilgelseTilsynBarn) =
        GeneriskVedtak(
            behandlingId = defaultBehandling.id,
            type = TypeVedtak.INNVILGELSE,
            data = data,
            gitVersjon = Applikasjonsversjon.versjon,
        )

    fun vedtaksperiodeGrunnlag(vedtaksperiode: VedtaksperiodeBeregning = vedtaksperiodeBeregning()): VedtaksperiodeGrunnlag =
        VedtaksperiodeGrunnlag(
            vedtaksperiode = vedtaksperiode,
            aktiviteter = emptyList(),
            antallDager = 0,
        )

    fun innvilgetVedtak(
        behandlingId: BehandlingId = defaultBehandling.id,
        beregningsresultat: BeregningsresultatTilsynBarn = vedtakBeregningsresultat,
        vedtaksperioder: List<Vedtaksperiode> = emptyList(),
    ) = GeneriskVedtak(
        behandlingId = behandlingId,
        type = TypeVedtak.INNVILGELSE,
        data =
            InnvilgelseTilsynBarn(
                beregningsresultat = beregningsresultat,
                vedtaksperioder = vedtaksperioder,
            ),
        gitVersjon = Applikasjonsversjon.versjon,
    )

    fun innvilgetVedtak(
        behandlingId: BehandlingId = defaultBehandling.id,
        vedtak: InnvilgelseTilsynBarn,
    ) = GeneriskVedtak(
        behandlingId = behandlingId,
        type = TypeVedtak.INNVILGELSE,
        data = vedtak,
        gitVersjon = Applikasjonsversjon.versjon,
    )

    fun avslagVedtak(
        behandlingId: BehandlingId = defaultBehandling.id,
        årsaker: List<ÅrsakAvslag>,
        begrunnelse: String,
    ) = GeneriskVedtak(
        behandlingId = behandlingId,
        type = TypeVedtak.AVSLAG,
        data =
            AvslagTilsynBarn(
                årsaker = årsaker,
                begrunnelse = begrunnelse,
            ),
        gitVersjon = Applikasjonsversjon.versjon,
    )

    fun opphørVedtak(
        behandlingId: BehandlingId = defaultBehandling.id,
        årsaker: List<ÅrsakOpphør>,
        beregningsresultat: BeregningsresultatTilsynBarn = vedtakBeregningsresultat,
        begrunnelse: String,
    ) = GeneriskVedtak(
        behandlingId = behandlingId,
        type = TypeVedtak.OPPHØR,
        data =
            OpphørTilsynBarn(
                årsaker = årsaker,
                begrunnelse = begrunnelse,
                beregningsresultat = beregningsresultat,
                vedtaksperioder = emptyList(),
            ),
        gitVersjon = Applikasjonsversjon.versjon,
    )
}
