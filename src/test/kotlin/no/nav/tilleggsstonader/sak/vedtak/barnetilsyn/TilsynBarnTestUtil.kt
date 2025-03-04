package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.behandling.barn.BehandlingBarn
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.interntVedtak.Testdata.behandlingId
import no.nav.tilleggsstonader.sak.util.behandling
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.StønadsperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.UtgiftBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.VedtaksperiodeBeregning
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnRequestV2
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.domain.AvslagTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.Vedtaksperiode
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakAvslag
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.AktivitetType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

object TilsynBarnTestUtil {
    fun innvilgelseDto() = InnvilgelseTilsynBarnRequest

    fun innvilgelseDtoV2(
        vedtaksperioder: List<VedtaksperiodeDto>,
        begrunnelse: String? = null,
    ) = InnvilgelseTilsynBarnRequestV2(
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
            Beløpsperiode(dato = LocalDate.now(), beløp = 1000, målgruppe = MålgruppeType.AAP),
            Beløpsperiode(dato = LocalDate.now().plusDays(7), beløp = 2000, målgruppe = MålgruppeType.OVERGANGSSTØNAD),
        )

    val defaultVedtaksperiode =
        Vedtaksperiode(
            id = defaultVedtaksperiodeId,
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 1, 31),
            målgruppe = MålgruppeType.AAP,
            aktivitet = AktivitetType.TILTAK,
        )

    val defaultVedtaksperiodeBeregning =
        VedtaksperiodeBeregning(
            fom = LocalDate.of(2024, 1, 1),
            tom = LocalDate.of(2024, 1, 31),
            målgruppe = MålgruppeType.AAP,
            aktivitet = AktivitetType.TILTAK,
        )

    val defaultInnvilgelseTilsynBarn =
        InnvilgelseTilsynBarn(
            beregningsresultat =
                BeregningsresultatTilsynBarn(
                    perioder =
                        listOf(
                            beregningsresultatForMåned(stønadsperioder = listOf(stønadsperiodeGrunnlag())),
                        ),
                ),
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
        stønadsperioder: List<StønadsperiodeGrunnlag> = emptyList(),
        beløpsperioder: List<Beløpsperiode> = beløpsperioderDefault,
        utgifterTotal: Int = 5000,
    ) = BeregningsresultatForMåned(
        dagsats = BigDecimal.TEN,
        månedsbeløp = 3000,
        grunnlag =
            Beregningsgrunnlag(
                måned = måned,
                makssats = 3000,
                stønadsperioderGrunnlag = stønadsperioder,
                utgifter = listOf(UtgiftBarn(defaultBarn1.id, 1000)),
                utgifterTotal = utgifterTotal,
                antallBarn = 1,
            ),
        beløpsperioder = beløpsperioder,
    )

    fun innvilgelse(data: InnvilgelseTilsynBarn = defaultInnvilgelseTilsynBarn) =
        GeneriskVedtak(
            behandlingId = defaultBehandling.id,
            type = TypeVedtak.INNVILGELSE,
            data = data,
        )

    fun stønadsperiodeGrunnlag(vedtaksperiode: VedtaksperiodeBeregning = defaultVedtaksperiodeBeregning): StønadsperiodeGrunnlag =
        StønadsperiodeGrunnlag(
            stønadsperiode = vedtaksperiode,
            aktiviteter = emptyList(),
            antallDager = 0,
        )

    fun innvilgetVedtak(
        beregningsresultat: BeregningsresultatTilsynBarn = vedtakBeregningsresultat,
        behandlingId: BehandlingId = defaultBehandling.id,
        vedtaksperioder: List<Vedtaksperiode>? = null,
    ) = GeneriskVedtak(
        behandlingId = behandlingId,
        type = TypeVedtak.INNVILGELSE,
        data =
            InnvilgelseTilsynBarn(
                beregningsresultat = beregningsresultat,
                vedtaksperioder = vedtaksperioder,
            ),
    )

    fun innvilgetVedtak(
        behandlingId: BehandlingId = defaultBehandling.id,
        vedtak: InnvilgelseTilsynBarn,
    ) = GeneriskVedtak(
        behandlingId = behandlingId,
        type = TypeVedtak.INNVILGELSE,
        data = vedtak,
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
    )
}
