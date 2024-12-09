package no.nav.tilleggsstonader.sak.vedtak.læremidler.domain

import no.nav.tilleggsstonader.kontrakter.felles.Periode
import java.time.LocalDate
import java.time.YearMonth
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType

data class BeregningsresultatLæremidler(
    val perioder: List<BeregningsresultatForMåned>,
)

data class BeregningsresultatForMåned(
    val beløp: Int,
    val grunnlag: Beregningsgrunnlag,
)

data class Beregningsgrunnlag(
    override val fom: LocalDate,
    override val tom: LocalDate,
    val utbetalingsMåned: YearMonth,
    val studienivå: Studienivå,
    val studieprosent: Int,
    val sats: Int,
    val målgruppe: MålgruppeType,
) : Periode<LocalDate>

