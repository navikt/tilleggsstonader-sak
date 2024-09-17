package no.nav.tilleggsstonader.sak.vedtak.barnetilsyn

import no.nav.tilleggsstonader.sak.felles.domain.BarnId
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.vedtak.TypeVedtak
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.Beløpsperiode
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.Beregningsgrunnlag
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.Beregningsresultat
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.BeregningsresultatTilsynBarnDto
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.InnvilgelseTilsynBarnDto
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

    val vedtakBeregningsresultat = VedtaksdataBeregningsresultat(
        perioder = listOf(
            Beregningsresultat(
                dagsats = BigDecimal.TEN,
                månedsbeløp = 1000,
                grunnlag = Beregningsgrunnlag(
                    måned = YearMonth.now(),
                    makssats = 1000,
                    stønadsperioderGrunnlag = emptyList(),
                    utgifter = emptyList(),
                    utgifterTotal = 2000,
                    antallBarn = 1,
                ),
                beløpsperioder = listOf(
                    Beløpsperiode(dato = LocalDate.now(), beløp = 1000, målgruppe = MålgruppeType.AAP),
                    Beløpsperiode(dato = LocalDate.now().plusMonths(1), beløp = 2000, målgruppe = MålgruppeType.AAP),
                ),
            ),
        ),
    )

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
        beregningsresultat: VedtaksdataBeregningsresultat? = vedtakBeregningsresultat,
        behandlingId: BehandlingId = BehandlingId.random(),
    ) = VedtakTilsynBarn(
        behandlingId = behandlingId,
        type = TypeVedtak.INNVILGELSE,
        vedtak = vedtak,
        beregningsresultat = beregningsresultat,
    )

    fun barn(barnId: BarnId, vararg utgifter: Utgift) = Pair(barnId, utgifter.toList())
}
