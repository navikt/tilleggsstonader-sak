package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.StønadsperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnRequest
import no.nav.tilleggsstonader.sak.vedtak.domain.GeneriskVedtak
import no.nav.tilleggsstonader.sak.vedtak.domain.InnvilgelseTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.domain.StønadsperiodeBeregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.domain.ÅrsakOpphør
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

object TilsynBarnTestUtil {

    fun innvilgelseDto() = InnvilgelseTilsynBarnRequest

    fun opphørDto() = OpphørTilsynBarnRequest(
        årsakerOpphør = listOf(ÅrsakOpphør.ENDRING_UTGIFTER),
        begrunnelse = "Endring i utgifter",
    )

    val beløpsperioderDefault = listOf(
        Beløpsperiode(dato = LocalDate.now(), beløp = 1000, målgruppe = MålgruppeType.AAP),
        Beløpsperiode(dato = LocalDate.now().plusMonths(1), beløp = 2000, målgruppe = MålgruppeType.AAP),
    )

    val vedtakBeregningsresultat = BeregningsresultatTilsynBarn(
        perioder = listOf(
            beregningsresultatForMåned(),
        ),
    )

    fun beregningsresultatForMåned(
        måned: YearMonth = YearMonth.now(),
        stønadsperioder: List<StønadsperiodeGrunnlag> = emptyList(),
        beløpsperioder: List<Beløpsperiode> = beløpsperioderDefault,
    ) = BeregningsresultatForMåned(
        dagsats = BigDecimal.TEN,
        månedsbeløp = 1000,
        grunnlag = Beregningsgrunnlag(
            måned = måned,
            makssats = 1000,
            stønadsperioderGrunnlag = stønadsperioder,
            utgifter = emptyList(),
            utgifterTotal = 2000,
            antallBarn = 1,
        ),
        beløpsperioder = beløpsperioder,
    )

    fun stønadsperiodeGrunnlag(
        stønadsperiode: StønadsperiodeBeregningsgrunnlag,
    ): StønadsperiodeGrunnlag {
        return StønadsperiodeGrunnlag(
            stønadsperiode = stønadsperiode,
            aktiviteter = emptyList(),
            antallDager = 0,
        )
    }

    fun innvilgetVedtak(
        beregningsresultat: BeregningsresultatTilsynBarn = vedtakBeregningsresultat,
        behandlingId: BehandlingId = BehandlingId.random(),
    ) = GeneriskVedtak(
        behandlingId = behandlingId,
        type = TypeVedtak.INNVILGELSE,
        data = InnvilgelseTilsynBarn(
            beregningsresultat = beregningsresultat,
        ),
    )
}
