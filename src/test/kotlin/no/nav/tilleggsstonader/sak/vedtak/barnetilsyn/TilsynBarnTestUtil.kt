package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatForMåned
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.BeregningsresultatTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.Stønadsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.domain.StønadsperiodeGrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.BeregningsresultatTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.OpphørTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.Utgift
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

object TilsynBarnTestUtil {

    fun innvilgelseDto(
        beregningsresultat: BeregningsresultatTilsynBarnDto? = null,
    ) = InnvilgelseTilsynBarnDto(
        beregningsresultat = beregningsresultat,
    )

    fun opphørDto() = OpphørTilsynBarnDto(
        årsakerOpphør = listOf(ÅrsakOpphør.ENDRING_UTGIFTER),
        begrunnelse = "Endring i utgifter",
    )

    val vedtakBeregningsresultat = BeregningsresultatTilsynBarn(
        perioder = listOf(
            beregningsresultatForMåned(),
        ),
    )

    fun beregningsresultatForMåned(
        måned: YearMonth = YearMonth.now(),
        stønadsperioder: List<StønadsperiodeGrunnlag> = emptyList(),
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
        beløpsperioder = listOf(
            Beløpsperiode(dato = LocalDate.now(), beløp = 1000, målgruppe = MålgruppeType.AAP),
            Beløpsperiode(dato = LocalDate.now().plusMonths(1), beløp = 2000, målgruppe = MålgruppeType.AAP),
        ),
    )

    fun stønadsperiodeGrunnlag(
        stønadsperiode: Stønadsperiode,
    ): StønadsperiodeGrunnlag {
        return StønadsperiodeGrunnlag(
            stønadsperiode = stønadsperiode,
            aktiviteter = emptyList(),
            antallDager = 0,
        )
    }

    val vedtaksdata = VedtaksdataTilsynBarn(
        utgifter = mapOf(
            barn(
                BarnId.random(),
                Utgift(YearMonth.of(2023, 1), YearMonth.of(2023, 1), 100),
            ),
        ),
    )

    fun innvilgetVedtak(
        vedtak: VedtaksdataTilsynBarn? = vedtaksdata,
        beregningsresultat: BeregningsresultatTilsynBarn? = vedtakBeregningsresultat,
        behandlingId: BehandlingId = BehandlingId.random(),
    ) = VedtakTilsynBarn(
        behandlingId = behandlingId,
        type = TypeVedtak.INNVILGELSE,
        vedtak = vedtak,
        beregningsresultat = beregningsresultat,
    )

    fun barn(barnId: BarnId, vararg utgifter: Utgift) = Pair(barnId, utgifter.toList())
}
